package com.pixelstudio.ui;

import com.pixelstudio.command.Args;
import com.pixelstudio.command.Command;
import com.pixelstudio.command.CommandContext;
import com.pixelstudio.command.CommandRegistry;
import com.pixelstudio.exception.StudioException;
import com.pixelstudio.model.Project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * 命令行循环：读一行 -&gt; 切分 -&gt; 找命令 -&gt; 执行 -&gt; 处理错误 -&gt; 回到第一步。
 *
 * <p><b>这个类想让你练到什么？</b>
 * <ul>
 *   <li><b>try-with-resources</b>：{@code try (BufferedReader in = ...)}，
 *       代码块结束时不管有没有抛异常都会自动关流，不用手写 finally；</li>
 *   <li><b>异常的分层处理</b>：业务错误（{@link StudioException}）给用户看一句人话，
 *       其它异常才打印堆栈 —— 详见 {@link #run} 里的注释；</li>
 *   <li><b>用 {@code Optional} 表达"没有找到"}</b>，而不是返回 null；</li>
 *   <li><b>UTF-8 显式指定</b>：中文乱码九成出在这里。</li>
 * </ul>
 */
public class CommandLine {

    /**
     * BOM 字符的码点。用数字写而不是直接写那个字符本身 ——
     * 源文件里放一个看不见的字符，迟早会有人（包括编辑器）把它弄丢。
     */
    private static final char BOM = 0xFEFF;

    private final CommandContext context;
    private final BufferedReader in;
    private final PrintStream out;

    public CommandLine(Project project, CommandRegistry registry, InputStream in, PrintStream out) {
        this.context = new CommandContext(project, registry, out);
        // 显式写死 UTF-8。Windows 默认是 GBK，不写的话中文可能变成乱码
        this.in = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        this.out = out;
    }

    /** 交互模式：一直读到用户输入 quit。 */
    public void run() {
        printBanner();
        while (context.isRunning()) {
            out.print("> ");
            out.flush();     // 提示符必须 flush，否则会被缓冲住不显示
            String line;
            try {
                line = in.readLine();
            } catch (IOException e) {
                out.println("读取输入出错：" + e.getMessage());
                return;
            }
            if (line == null) {
                // 用户按了 Ctrl+D，或者输入被重定向到文件读完了
                out.println();
                out.println("  输入结束了，再见！");
                return;
            }
            executeOne(line);
        }
    }

    /**
     * 脚本模式：把一个文件里的命令逐行执行一遍。
     *
     * <p>这是练"参数化 / 批处理"的好玩法：你可以写一串命令生成一整幅画，
     * 反复调参数，每次都省得手敲。{@code #} 开头是注释，空行自动跳过。
     */
    public void runScript(Path scriptFile) {
        if (!Files.exists(scriptFile)) {
            out.println("  脚本文件不存在：" + scriptFile);
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(scriptFile, StandardCharsets.UTF_8)) {
            out.println("  正在执行脚本 " + scriptFile + " ...");
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.strip();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                out.printf("%3d| %s%n", lineNumber, trimmed);
                try {
                    executeOne(trimmed);
                } catch (RuntimeException e) {
                    // 脚本里某一行的参数写错了，不该把整个脚本都终止掉
                    out.println("    ^ 第 " + lineNumber + " 行出错：" + e.getMessage());
                }
                if (!context.isRunning()) {
                    break;
                }
            }
            out.println("  脚本执行完毕。");
        } catch (IOException e) {
            out.println("  读取脚本失败：" + e.getMessage());
        }
    }

    /** 执行一行命令。抽出来是为了让交互模式和脚本模式共用同一套逻辑。 */
    public void executeOne(String rawLine) {
        String line = stripBom(rawLine).strip();
        if (line.isEmpty() || line.startsWith("#")) {
            return;
        }

        try {
            // 1. 切分：第一个词是命令名，其余是参数
            Args args = Args.parse(line);
            if (args.isEmpty()) {
                return;
            }
            String commandName = args.get(0);
            List<String> rest = args.values().subList(1, args.size());

            // 2. 找命令：用 Optional 表达"可能没有"
            Optional<Command> found = context.registry().find(commandName);
            if (found.isEmpty()) {
                throw new StudioException("没有这条命令：" + commandName
                        + "（输入 help 看有哪些命令）");
            }

            // 3. 执行
            found.get().execute(context, rest);

        } catch (StudioException e) {
            // 用户犯的错：只给一句人话，不甩堆栈。堆栈对终端用户毫无意义
            out.println("  ! " + e.getMessage());
        } catch (RuntimeException e) {
            // 没预料到的错误：这才是真 bug，打完整堆栈方便你定位
            out.println("  ! 发生了未预料的错误：" + e);
            e.printStackTrace(out);
        }
    }

    /**
     * 去掉 UTF-8 BOM。
     *
     * <p>BOM（Byte Order Mark，字节顺序标记）是 {@code U+FEFF} 这个"看不见的字符"。
     * 用 Windows 记事本、或者 PowerShell 的 {@code Set-Content -Encoding utf8} 存文件时，
     * 它会被悄悄写在文件最开头；管道的输入也一样。
     *
     * <p>如果不处理，用户会遇到一个非常费解的现象：
     * 明明写的是 {@code new 10 10}，程序却说"没有这条命令：﻿new"，还多显示一个方框。
     * 而且 {@code strip()} 去不掉它 —— 因为 U+FEFF 严格来说不是空白字符。
     *
     * <p>这是一个很好的例子：<strong>现实世界的脏数据是无处不在的</strong>，
     * 读外部输入的代码永远要假设"用户可能什么都干得出来"。
     */
    private static String stripBom(String line) {
        if (line != null && !line.isEmpty() && line.charAt(0) == BOM) {
            return line.substring(1);
        }
        return line;
    }

    private void printBanner() {
        out.println();
        out.println("  ╔══════════════════════════════════════════╗");
        out.println("  ║   Pixel Studio · 终端像素画 & 动画工具      ║");
        out.println("  ╚══════════════════════════════════════════╝");
        out.println();
        out.println("  输入 help 看命令列表，quit 退出。");
        out.println("  试试： new 画布 40 16  然后  show --ruler");
        out.println();
    }
}

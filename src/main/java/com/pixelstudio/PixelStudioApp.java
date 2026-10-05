package com.pixelstudio;

import com.pixelstudio.command.CommandRegistry;
import com.pixelstudio.model.Project;
import com.pixelstudio.ui.CommandLine;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * 程序入口。
 *
 * <p><b>这个类想让你练到什么？</b>
 * <ul>
 *   <li>{@code main} 方法只做"组装"：造对象、连起来、启动。真正的逻辑一行都不该出现在这里；</li>
 *   <li><b>依赖倒置</b>：注意 {@code CommandLine} 的参数里没有出现 {@code new}，
 *       它需要什么就传什么，谁负责造是调用方的事。这样写单元测试时，
 *       可以传一个假的 {@code InputStream} 进去喂命令，完全不用启动真的终端；</li>
 *   <li><b>参数解析</b>：{@link #printUsage} 里的 {@code switch} 表达式是 Java 14+ 的写法。</li>
 * </ul>
 *
 * <p>用法：
 * <pre>
 *   java -jar pixel-studio.jar              交互模式
 *   java -jar pixel-studio.jar script.pxs   脚本模式（执行一个命令脚本）
 * </pre>
 */
public final class PixelStudioApp {

    private PixelStudioApp() {
    }

    public static void main(String[] args) {
        // 强制用 UTF-8 输出，免得 Windows 控制台把中文显示成乱码
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);

        Project project = Project.newProject("未命名", 40, 16);
        CommandRegistry registry = CommandRegistry.createDefault();
        CommandLine cli = new CommandLine(project, registry, System.in, out);

        switch (args.length) {
            case 0 -> cli.run();
            case 1 -> cli.runScript(Path.of(args[0]));
            default -> {
                out.println("参数太多啦。");
                printUsage();
            }
        }
    }

    private static void printUsage() {
        System.out.println("""
                用法:
                  java -jar pixel-studio.jar              交互模式，直接开始画
                  java -jar pixel-studio.jar <脚本文件>     批量执行一个命令脚本
                """);
    }
}

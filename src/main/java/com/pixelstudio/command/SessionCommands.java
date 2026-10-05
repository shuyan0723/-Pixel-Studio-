package com.pixelstudio.command;

import com.pixelstudio.exception.StudioException;
import com.pixelstudio.model.Canvas;
import com.pixelstudio.model.Color;
import com.pixelstudio.model.Project;

import java.util.List;

/**
 * 会话级的命令：新建、帮助、信息、调色板、退出。
 *
 * <p><b>为什么一个文件里放这么多类？</b>
 * <p>它们属于同一组功能，而且都只在 {@link CommandRegistry} 内部用得上，
 * 没必要每个都占一个文件、还都变成 {@code public}。
 * 把它们写成<strong>包级私有</strong>（不写 {@code public}），就是 Java 里很地道的"实现细节不外露"。
 */
final class SessionCommands {

    private SessionCommands() {
    }

    static List<Command> all() {
        return List.of(new NewCommand(), new HelpCommand(), new InfoCommand(),
                new ColorCommand(), new RenameCommand(), new QuitCommand());
    }

    // ------------------------------------------------------------------

    static final class NewCommand implements Command {

        @Override
        public String name() {
            return "new";
        }

        @Override
        public String summary() {
            return "新建一个空项目（会丢弃当前内容）";
        }

        @Override
        public String usage() {
            return "new <名称> <宽> <高>    例: new 我的像素画 40 20";
        }

        @Override
        public void execute(CommandContext context, List<String> args) {
            // 命令循环已经把命令名切掉，args 就是剩下的部分。
            // 名称里可能有空格，所以只把最后两个参数当数字，剩下的整体当项目名。
            if (args.size() < 3) {
                throw new StudioException("参数不够，用法：" + usage());
            }
            int width = Args.parseInt(args.get(args.size() - 2), usage());
            int height = Args.parseInt(args.get(args.size() - 1), usage());
            String projectName = String.join(" ", args.subList(0, args.size() - 2));

            context.replaceProject(Project.newProject(projectName, width, height));
            context.info("  已新建项目「%s」%d×%d", projectName, width, height);
        }
    }

    // ------------------------------------------------------------------

    static final class HelpCommand implements Command {

        @Override
        public String name() {
            return "help";
        }

        @Override
        public List<String> aliases() {
            return List.of("?", "h");
        }

        @Override
        public String summary() {
            return "列出所有命令";
        }

        @Override
        public String usage() {
            return "help [命令名]";
        }

        @Override
        public void execute(CommandContext context, List<String> args) {
            if (!args.isEmpty()) {
                Command command = context.registry().require(args.get(0));
                context.info("");
                context.info("  %s  ——  %s", command.name(), command.summary());
                context.info("  用法: %s", command.usage());
                if (!command.aliases().isEmpty()) {
                    context.info("  别名: %s", String.join(", ", command.aliases()));
                }
                context.info("");
                return;
            }

            context.info("");
            context.info("  Pixel Studio 命令一览（括号里是别名）");
            context.info("  " + "-".repeat(56));
            for (Command command : context.registry().all()) {
                String aliases = command.aliases().isEmpty()
                        ? ""
                        : " (" + String.join(", ", command.aliases()) + ")";
                context.info("  %-10s %s%s", command.name(), command.summary(), aliases);
            }
            context.info("  " + "-".repeat(56));
            context.info("  输入 help <命令名> 看某条命令的详细用法");
            context.info("");
        }
    }

    // ------------------------------------------------------------------

    static final class InfoCommand implements Command {

        @Override
        public String name() {
            return "info";
        }

        @Override
        public List<String> aliases() {
            return List.of("i");
        }

        @Override
        public String summary() {
            return "看看当前项目用了多少像素、什么颜色";
        }

        @Override
        public String usage() {
            return "info";
        }

        @Override
        public void execute(CommandContext context, List<String> args) {
            Canvas canvas = context.project().canvas();
            context.info("");
            context.info("  项目     : %s", context.project().name());
            context.info("  画布     : %d × %d", canvas.width(), canvas.height());
            context.info("  当前帧   : %d / %d  (%s)",
                    context.project().animation().currentIndex() + 1,
                    context.project().animation().frameCount(),
                    context.project().animation().currentFrame().name());
            context.info("  帧率     : %d fps  (循环播放: %s)",
                    context.project().animation().fps(),
                    context.project().animation().looping() ? "是" : "否");
            context.info("  当前画笔 : %s", context.currentColor().cn());
            context.info("  已涂像素 : %d / %d", canvas.paintedPixelCount(), canvas.width() * canvas.height());
            context.info("  颜色分布 : %s",
                    canvas.colorHistogram().isEmpty() ? "(还是空的)" : String.join("  ", canvas.colorHistogram()));
            context.info("");
        }
    }

    // ------------------------------------------------------------------

    static final class ColorCommand implements Command {

        @Override
        public String name() {
            return "color";
        }

        @Override
        public List<String> aliases() {
            return List.of("c");
        }

        @Override
        public String summary() {
            return "设置当前画笔颜色；不填参数则显示调色板";
        }

        @Override
        public String usage() {
            return "color [颜色名]    例: color 红  /  color cyan";
        }

        @Override
        public void execute(CommandContext context, List<String> args) {
            if (args.isEmpty()) {
                context.info("  可用颜色: %s", Color.paletteHelp());
                context.info("  当前画笔: %s", context.currentColor().cn());
                return;
            }
            String requested = String.join(" ", args);
            Color color = Color.fromName(requested).orElseThrow(() ->
                    new StudioException("没有这种颜色：" + requested + "。可用的是：" + Color.paletteHelp()));
            context.setCurrentColor(color);
            context.info("  画笔颜色 -> %s", color.cn());
        }
    }

    // ------------------------------------------------------------------

    static final class RenameCommand implements Command {

        @Override
        public String name() {
            return "rename";
        }

        @Override
        public String summary() {
            return "给项目换个名字";
        }

        @Override
        public String usage() {
            return "rename <新名字>";
        }

        @Override
        public void execute(CommandContext context, List<String> args) {
            if (args.isEmpty()) {
                throw new StudioException("参数不够，用法：" + usage());
            }
            context.project().rename(String.join(" ", args));
            context.info("  项目名 -> %s", context.project().name());
        }
    }

    // ------------------------------------------------------------------

    static final class QuitCommand implements Command {

        @Override
        public String name() {
            return "quit";
        }

        @Override
        public List<String> aliases() {
            return List.of("exit", "q");
        }

        @Override
        public String summary() {
            return "退出程序";
        }

        @Override
        public String usage() {
            return "quit";
        }

        @Override
        public void execute(CommandContext context, List<String> args) {
            context.info("  再见！记得 save 一下哦。");
            context.requestExit();
        }
    }
}

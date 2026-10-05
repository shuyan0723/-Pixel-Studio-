package com.pixelstudio.command;

import com.pixelstudio.exception.StudioException;
import com.pixelstudio.model.Canvas;
import com.pixelstudio.model.Color;
import com.pixelstudio.ui.ConsoleRenderer;

import java.util.List;
import java.util.Optional;

/**
 * 画图命令：这些是真正动像素的地方。
 *
 * <p>注意这一组命令都有同一个套路：
 * <ol>
 *   <li>校验参数个数 —— 少给就告诉用户怎么用；</li>
 *   <li>把字符串参数转成数字 —— 转不了就抛一条人话错误；</li>
 *   <li>真正干活。</li>
 * </ol>
 * 三步都写在 {@link #execute} 里看起来有点重复，但这种重复是<strong>必要的</strong>：
 * 每个命令的参数个数、类型都不一样，强行抽成一个通用框架反而更难读。
 * 真正重复的是"取颜色"那一步，所以它被单独抽成了 {@link #resolveColor}。
 */
final class DrawCommands {

    private DrawCommands() {
    }

    static List<Command> all() {
        return List.of(new ClearCommand(), new SetCommand(), new LineCommand(),
                new RectCommand(), new FillCommand(), new CircleCommand(),
                new DiscCommand(), new TextCommand(), new ShowCommand());
    }

    /**
     * 解析颜色参数。
     *
     * <p>{@code Optional} 的标准用法：先拿到一个"可能没有"的结果，
     * 用 {@code orElseThrow} 把"没有"翻译成一个有意义的异常。
     *
     * <p><b>错误信息要能照着做</b>：只说"没有这种颜色：xx"等于没说，
     * 还得让用户自己去猜。直接把调色板列出来，一次就说清楚。
     *
     * @param raw       用户敲的颜色名，可以是 null（表示"用当前画笔色"）
     * @param fallback  没指定时用哪个颜色
     */
    static Color resolveColor(String raw, Color fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        Optional<Color> found = Color.fromName(raw);
        return found.orElseThrow(() -> new StudioException(
                "没有这种颜色：" + raw + "。可用的是：" + Color.paletteHelp()));
    }

    // ------------------------------------------------------------------

    static final class ClearCommand implements Command {

        @Override
        public String name() {
            return "clear";
        }

        @Override
        public String summary() {
            return "清空画布";
        }

        @Override
        public String usage() {
            return "clear [颜色]    不填则清成空白";
        }

        @Override
        public void execute(CommandContext context, List<String> args) {
            Color color = resolveColor(args.isEmpty() ? null : args.get(0), Color.BLANK);
            context.project().canvas().clear(color);
            context.info("  画布已清空 -> %s", color.cn());
        }
    }

    // ------------------------------------------------------------------

    static final class SetCommand implements Command {

        @Override
        public String name() {
            return "set";
        }

        @Override
        public List<String> aliases() {
            return List.of("px");
        }

        @Override
        public String summary() {
            return "画一个点";
        }

        @Override
        public String usage() {
            return "set <x> <y> [颜色]";
        }

        @Override
        public void execute(CommandContext context, List<String> args) {
            int x = Args.parseInt(args.size() > 0 ? args.get(0) : "", usage());
            int y = Args.parseInt(args.size() > 1 ? args.get(1) : "", usage());
            Color color = resolveColor(args.size() > 2 ? args.get(2) : null, context.currentColor());
            // set 是严格的：越界会抛异常。用 drawText 那种"越界自动裁剪"反而会静默画不出来，更难查
            context.project().canvas().set(x, y, color);
        }
    }

    // ------------------------------------------------------------------

    static final class LineCommand implements Command {

        @Override
        public String name() {
            return "line";
        }

        @Override
        public String summary() {
            return "画一条直线";
        }

        @Override
        public String usage() {
            return "line <x0> <y0> <x1> <y1> [颜色]";
        }

        @Override
        public void execute(CommandContext context, List<String> args) {
            String usage = usage();
            int x0 = Args.parseInt(args.size() > 0 ? args.get(0) : "", usage);
            int y0 = Args.parseInt(args.size() > 1 ? args.get(1) : "", usage);
            int x1 = Args.parseInt(args.size() > 2 ? args.get(2) : "", usage);
            int y1 = Args.parseInt(args.size() > 3 ? args.get(3) : "", usage);
            Color color = resolveColor(args.size() > 4 ? args.get(4) : null, context.currentColor());
            context.project().canvas().drawLine(x0, y0, x1, y1, color);
        }
    }

    // ------------------------------------------------------------------

    static final class RectCommand implements Command {

        @Override
        public String name() {
            return "rect";
        }

        @Override
        public String summary() {
            return "画一个空心矩形";
        }

        @Override
        public String usage() {
            return "rect <x> <y> <宽> <高> [颜色]";
        }

        @Override
        public void execute(CommandContext context, List<String> args) {
            String usage = usage();
            int x = Args.parseInt(args.size() > 0 ? args.get(0) : "", usage);
            int y = Args.parseInt(args.size() > 1 ? args.get(1) : "", usage);
            int w = Args.parseInt(args.size() > 2 ? args.get(2) : "", usage);
            int h = Args.parseInt(args.size() > 3 ? args.get(3) : "", usage);
            Color color = resolveColor(args.size() > 4 ? args.get(4) : null, context.currentColor());
            context.project().canvas().strokeRect(x, y, w, h, color);
        }
    }

    // ------------------------------------------------------------------

    static final class FillCommand implements Command {

        @Override
        public String name() {
            return "fill";
        }

        @Override
        public String summary() {
            return "画一个实心矩形";
        }

        @Override
        public String usage() {
            return "fill <x> <y> <宽> <高> [颜色]";
        }

        @Override
        public void execute(CommandContext context, List<String> args) {
            String usage = usage();
            int x = Args.parseInt(args.size() > 0 ? args.get(0) : "", usage);
            int y = Args.parseInt(args.size() > 1 ? args.get(1) : "", usage);
            int w = Args.parseInt(args.size() > 2 ? args.get(2) : "", usage);
            int h = Args.parseInt(args.size() > 3 ? args.get(3) : "", usage);
            Color color = resolveColor(args.size() > 4 ? args.get(4) : null, context.currentColor());
            context.project().canvas().fillRect(x, y, w, h, color);
        }
    }

    // ------------------------------------------------------------------

    static final class CircleCommand implements Command {

        @Override
        public String name() {
            return "circle";
        }

        @Override
        public String summary() {
            return "画一个空心圆";
        }

        @Override
        public String usage() {
            return "circle <中心x> <中心y> <半径> [颜色]";
        }

        @Override
        public void execute(CommandContext context, List<String> args) {
            String usage = usage();
            int cx = Args.parseInt(args.size() > 0 ? args.get(0) : "", usage);
            int cy = Args.parseInt(args.size() > 1 ? args.get(1) : "", usage);
            int r = Args.parseInt(args.size() > 2 ? args.get(2) : "", usage);
            Color color = resolveColor(args.size() > 3 ? args.get(3) : null, context.currentColor());
            if (r < 0) {
                throw new StudioException("半径不能是负数：" + r);
            }
            context.project().canvas().drawCircle(cx, cy, r, color);
        }
    }

    // ------------------------------------------------------------------

    static final class DiscCommand implements Command {

        @Override
        public String name() {
            return "disc";
        }

        @Override
        public String summary() {
            return "画一个实心圆";
        }

        @Override
        public String usage() {
            return "disc <中心x> <中心y> <半径> [颜色]";
        }

        @Override
        public void execute(CommandContext context, List<String> args) {
            String usage = usage();
            int cx = Args.parseInt(args.size() > 0 ? args.get(0) : "", usage);
            int cy = Args.parseInt(args.size() > 1 ? args.get(1) : "", usage);
            int r = Args.parseInt(args.size() > 2 ? args.get(2) : "", usage);
            Color color = resolveColor(args.size() > 3 ? args.get(3) : null, context.currentColor());
            if (r < 0) {
                throw new StudioException("半径不能是负数：" + r);
            }
            context.project().canvas().fillCircle(cx, cy, r, color);
        }
    }

    // ------------------------------------------------------------------

    static final class TextCommand implements Command {

        @Override
        public String name() {
            return "text";
        }

        @Override
        public String summary() {
            return "在画布上写一行字（引号里的空格不会被拆开）";
        }

        @Override
        public String usage() {
            return "text <x> <y> <内容> [颜色]    例: text 2 1 \"HELLO\" 蓝";
        }

        @Override
        public void execute(CommandContext context, List<String> args) {
            String usage = usage();
            int x = Args.parseInt(args.size() > 0 ? args.get(0) : "", usage);
            int y = Args.parseInt(args.size() > 1 ? args.get(1) : "", usage);
            if (args.size() < 3) {
                throw new StudioException("参数不够，用法：" + usage);
            }

            // 麻烦的地方：颜色写在最后，而内容可能包含空格。
            // 办法是"先判断最后那个参数是不是颜色，是就摘出来，剩下的整体当内容"。
            String last = args.get(args.size() - 1);
            Optional<Color> maybeColor = Color.fromName(last);
            int contentEnd = maybeColor.isPresent() ? args.size() - 1 : args.size();

            if (contentEnd < 3) {
                throw new StudioException("参数不够，用法：" + usage);
            }
            Color color = maybeColor.orElse(context.currentColor());
            String content = String.join(" ", args.subList(2, contentEnd));

            if (content.isEmpty()) {
                throw new StudioException("要写点什么呀，用法：" + usage);
            }

            Canvas canvas = context.project().canvas();
            canvas.drawText(x, y, content, color);
            context.info("  写下了「%s」（宽度 %d 像素，从 x=%d 开始）", content,
                    com.pixelstudio.model.BitmapFont.textWidth(content), x);
        }
    }

    // ------------------------------------------------------------------

    static final class ShowCommand implements Command {

        @Override
        public String name() {
            return "show";
        }

        @Override
        public String summary() {
            return "在终端里预览当前帧";
        }

        @Override
        public String usage() {
            return "show [--ruler] [--nocolor]";
        }

        @Override
        public void execute(CommandContext context, List<String> args) {
            boolean ruler = args.contains("--ruler");
            boolean noColor = args.contains("--nocolor");
            context.info("");
            ConsoleRenderer.print(context.out(), context.project().canvas(), !noColor, ruler);
            context.info("");
        }
    }
}

package com.pixelstudio.ui;

import com.pixelstudio.model.Canvas;
import com.pixelstudio.model.Color;

import java.io.PrintStream;

/**
 * 把画布"画"到终端上。
 *
 * <p>所谓上色，就是往输出里插一串看不见的控制字符（ESC 开头），
 * 终端读到它就把后面的字符按指定颜色显示。程序本身完全不知道屏幕长什么样 ——
 * 这就是"输出"和"显示"被分开的好处。
 *
 * <p>本类全部是<strong>静态方法</strong>：它不持有任何状态，只是一个纯函数式的转换器。
 * 无状态的东西用 static 比 new 出来到处传要清楚得多。
 */
public final class ConsoleRenderer {

    private ConsoleRenderer() {
    }

    /**
     * 渲染成带颜色的字符串。
     *
     * @param useColor 传 false 就输出纯文本，方便复制到记事本、或者重定向到文件
     */
    public static String render(Canvas canvas, boolean useColor) {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < canvas.height(); y++) {
            if (useColor) {
                sb.append(Color.ESC).append("[47m").append(Color.ANSI_RESET);
            }
            for (int x = 0; x < canvas.width(); x++) {
                Color pixel = canvas.get(x, y);
                if (useColor) {
                    sb.append(pixel.coloredGlyph());
                } else if (pixel == Color.BLANK) {
                    sb.append(' ');
                } else {
                    sb.append(pixel.glyph());
                }
            }
            if (useColor) {
                sb.append(Color.ESC).append("[47m").append(Color.ANSI_RESET);
            }
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }

    /**
     * 画一个带边框的预览，坐标标在边上，方便对着画。
     *
     * @param ruler 是否显示行列刻度
     */
    public static void print(PrintStream out, Canvas canvas, boolean useColor, boolean ruler) {
        int width = canvas.width();
        int height = canvas.height();

        if (ruler) {
            // 十位和个位分开写，宽度超过 10 之后也不会错位
            out.print("    ");
            for (int x = 0; x < width; x++) {
                out.print(x % 10);
            }
            out.println();
        }

        out.println("   +" + "-".repeat(width) + "+");
        for (int y = 0; y < height; y++) {
            if (ruler) {
                out.printf("%3d|", y);
            } else {
                out.print("   |");
            }
            if (useColor) {
                out.print(Color.ESC + "[47m");
            }
            for (int x = 0; x < width; x++) {
                Color pixel = canvas.get(x, y);
                out.print(useColor ? pixel.coloredGlyph() : (pixel == Color.BLANK ? " " : String.valueOf(pixel.glyph())));
            }
            if (useColor) {
                out.print(Color.ESC + "[47m" + Color.ANSI_RESET);
            }
            out.println("|");
        }
        out.println("   +" + "-".repeat(width) + "+");
    }
}

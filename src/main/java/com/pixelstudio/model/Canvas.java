package com.pixelstudio.model;

import com.pixelstudio.exception.StudioException;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

/**
 * 画布：一块可以画东西的二维像素板。
 *
 * <p><b>这个类想让你练到什么？</b>
 * <ul>
 *   <li><b>二维数组</b> {@code Color[][]}：注意 Java 是"先行后列"，所以是 {@code [高][宽]}，别写成 [宽][高]；</li>
 *   <li><b>不可变字段</b>：width / height 一旦确定就不改，用 final 锁死；</li>
 *   <li><b>值拷贝 vs 引用拷贝</b>：{@link #copy()} 必须逐格复制，直接 {@code new Color[][]} 是不行的；</li>
 *   <li><b>Bresenham 直线算法</b>：只用整数运算画斜线，不会有浮点误差导致锯齿。</li>
 * </ul>
 *
 * <p>约定：坐标从左上角开始，{@code x} 向右增长，{@code y} 向下增长，和所有图形界面一样。
 */
public class Canvas {

    public static final int MIN_SIZE = 4;
    public static final int MAX_SIZE = 400;

    private final int width;
    private final int height;
    private final Color[][] pixels;

    public Canvas(int width, int height) {
        checkSize(width, height);
        this.width = width;
        this.height = height;
        this.pixels = new Color[height][width];
        clear(Color.BLANK);
    }

    /** 新建一块已经被某种颜色铺满的画布。 */
    public static Canvas filled(int width, int height, Color color) {
        Canvas canvas = new Canvas(width, height);
        canvas.clear(Objects.requireNonNull(color, "填充颜色不能为 null"));
        return canvas;
    }

    private static void checkSize(int width, int height) {
        if (width < MIN_SIZE || height < MIN_SIZE) {
            throw new StudioException("画布太小了，宽和高至少都要 >= " + MIN_SIZE);
        }
        if (width > MAX_SIZE || height > MAX_SIZE) {
            throw new StudioException("画布太大了，宽和高都不能超过 " + MAX_SIZE);
        }
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public boolean inBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    private void requireInBounds(int x, int y) {
        if (!inBounds(x, y)) {
            throw new StudioException(String.format(Locale.ROOT,
                    "坐标 (%d, %d) 超出画布范围，画布是 %d×%d（合法范围 x: 0~%d, y: 0~%d）",
                    x, y, width, height, width - 1, height - 1));
        }
    }

    /**
     * 读一个像素。<b>越界时返回 BLANK 而不是抛异常</b>。
     *
     * <p>这是一个刻意的设计：画斜线、画圆的时候经常要碰到边界外的点，
     * 让"读"宽容一点，代码就能简单很多。写才需要严格。
     */
    public Color get(int x, int y) {
        return inBounds(x, y) ? pixels[y][x] : Color.BLANK;
    }

    /** 写一个像素，越界会抛 {@link StudioException}。 */
    public void set(int x, int y, Color color) {
        requireInBounds(x, y);
        pixels[y][x] = color == null ? Color.BLANK : color;
    }

    /** 写一个像素，越界就默默跳过（给"自动裁剪"的绘制操作用）。 */
    public void setIfInBounds(int x, int y, Color color) {
        if (inBounds(x, y)) {
            set(x, y, color);
        }
    }

    /** 整块擦掉重画。 */
    public void clear(Color color) {
        for (int y = 0; y < height; y++) {
            Arrays.fill(pixels[y], color);
        }
    }

    /**
     * 深拷贝。Java 的数组是"引用"，直接赋值只会多一个名字指向同一块内存，
     * 必须像这样逐格新建，才能真正复制出一张独立的新画布。
     */
    public Canvas copy() {
        Canvas other = new Canvas(width, height);
        for (int y = 0; y < height; y++) {
            System.arraycopy(pixels[y], 0, other.pixels[y], 0, width);
        }
        return other;
    }

    // ------------------------------------------------------------------
    //  下面是一组绘图算法，全部"越界自动裁剪"，不会因为画到边上就崩掉
    // ------------------------------------------------------------------

    /** 实心矩形。 */
    public void fillRect(int x, int y, int w, int h, Color color) {
        if (w <= 0 || h <= 0) {
            return;
        }
        for (int dy = 0; dy < h; dy++) {
            for (int dx = 0; dx < w; dx++) {
                setIfInBounds(x + dx, y + dy, color);
            }
        }
    }

    /** 矩形边框（空心）。 */
    public void strokeRect(int x, int y, int w, int h, Color color) {
        if (w <= 0 || h <= 0) {
            return;
        }
        for (int dx = 0; dx < w; dx++) {
            setIfInBounds(x + dx, y, color);
            setIfInBounds(x + dx, y + h - 1, color);
        }
        for (int dy = 0; dy < h; dy++) {
            setIfInBounds(x, y + dy, color);
            setIfInBounds(x + w - 1, y + dy, color);
        }
    }

    /**
     * Bresenham 直线算法：从 (x0, y0) 到 (x1, y1) 画一条直线。
     *
     * <p>核心思路是维护一个"误差" {@code err}：每画一个点就更新一次误差，
     * 误差够大就把另一个方向的坐标也 +1。只用整数运算，所以横线、竖线、斜线一视同仁。
     */
    public void drawLine(int x0, int y0, int x1, int y1, Color color) {
        int dx = Math.abs(x1 - x0);
        int dy = -Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;

        while (true) {
            setIfInBounds(x0, y0, color);
            if (x0 == x1 && y0 == y1) {
                break;
            }
            int e2 = 2 * err;
            if (e2 >= dy) {
                err += dy;
                x0 += sx;
            }
            if (e2 <= dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    /** 空心圆（圆周）。用的是中点判圆算法，不是三角函数，所以快且没有误差。 */
    public void drawCircle(int cx, int cy, int r, Color color) {
        if (r < 0) {
            return;
        }
        if (r == 0) {
            setIfInBounds(cx, cy, color);
            return;
        }
        int x = r;
        int y = 0;
        int err = 1 - r;
        while (x >= y) {
            setIfInBounds(cx + x, cy + y, color);
            setIfInBounds(cx + y, cy + x, color);
            setIfInBounds(cx - y, cy + x, color);
            setIfInBounds(cx - x, cy + y, color);
            setIfInBounds(cx - x, cy - y, color);
            setIfInBounds(cx - y, cy - x, color);
            setIfInBounds(cx + y, cy - x, color);
            setIfInBounds(cx + x, cy - y, color);
            y++;
            if (err < 0) {
                err += 2 * y + 1;
            } else {
                x--;
                err += 2 * (y - x) + 1;
            }
        }
    }

    /** 实心圆（从圆心往外扫每一行，填一段水平线段）。 */
    public void fillCircle(int cx, int cy, int r, Color color) {
        if (r < 0) {
            return;
        }
        for (int dy = -r; dy <= r; dy++) {
            int span = (int) Math.floor(Math.sqrt(r * r - dy * dy));
            for (int dx = -span; dx <= span; dx++) {
                setIfInBounds(cx + dx, cy + dy, color);
            }
        }
    }

    /** 用点阵字体写一行字，超出画布的部分会被裁掉。 */
    public void drawText(int x, int y, String text, Color color) {
        if (text == null || text.isEmpty()) {
            return;
        }
        int cursor = x;
        for (char ch : text.toCharArray()) {
            String[] glyph = BitmapFont.glyphOf(ch);
            if (glyph != null) {
                for (int row = 0; row < BitmapFont.GLYPH_H; row++) {
                    String line = glyph[row];
                    for (int col = 0; col < BitmapFont.GLYPH_W; col++) {
                        if (line.charAt(col) == '#') {
                            setIfInBounds(cursor + col, y + row, color);
                        }
                    }
                }
            }
            cursor += BitmapFont.GLYPH_W + BitmapFont.SPACING;
        }
    }

    // ------------------------------------------------------------------
    //  统计信息：用 Stream 处理集合
    // ------------------------------------------------------------------

    /** 统计每种颜色各有多少个像素，按像素数从多到少排。返回的是 {@code "红×12" } 这样的描述列表。 */
    public java.util.List<String> colorHistogram() {
        return Arrays.stream(pixels)
                .flatMap(Arrays::stream)
                .filter(c -> c != Color.BLANK)
                .collect(java.util.stream.Collectors.groupingBy(
                        c -> c,
                        java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .map(e -> e.getKey().cn() + "×" + e.getValue())
                .collect(java.util.stream.Collectors.toList());
    }

    /** 一共有多少个非空像素。 */
    public long paintedPixelCount() {
        return Arrays.stream(pixels)
                .flatMap(Arrays::stream)
                .filter(c -> c != Color.BLANK)
                .count();
    }

    @Override
    public String toString() {
        return "Canvas[" + width + "×" + height + "]";
    }
}

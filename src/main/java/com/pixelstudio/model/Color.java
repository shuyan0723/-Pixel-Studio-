package com.pixelstudio.model;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 调色板。
 *
 * <p>这是一个 {@code enum}（枚举）。用枚举表示调色板的好处是：
 * 颜色集合在编译期就固定了，不可能出现 {@code "深灰色"} 这种拼写错误，
 * 拼错了编译器会直接报错，而不是等到运行时才发现。
 *
 * <p>每个颜色带三样东西：
 * <ul>
 *   <li>{@code glyph} —— 用来"画"这个颜色的字符。之所以给每个颜色分配<strong>不同</strong>的字符，
 *       是为了保证保存成文件之后能 100% 原样读回来，同时在没有颜色的终端里也能看出深浅层次。</li>
 *   <li>{@code ansi} —— ANSI 转义序列里的颜色编号，用来在支持颜色的终端里上色。</li>
 *   <li>{@code cn} —— 中文名，让用户输入 {@code color 红} 也能认。</li>
 * </ul>
 */
public enum Color {

    BLANK(' ', 0, "空白"),
    BLACK('@', 30, "黑"),
    RED('R', 31, "红"),
    GREEN('G', 32, "绿"),
    YELLOW('Y', 33, "黄"),
    BLUE('B', 34, "蓝"),
    MAGENTA('M', 35, "品红"),
    CYAN('C', 36, "青"),
    WHITE('W', 97, "白"),
    GRAY('x', 90, "灰"),
    LIGHT('o', 37, "浅灰"),
    BROWN('n', 33, "棕"),
    PINK('p', 35, "粉"),
    LIME('l', 92, "亮绿"),
    OLIVE('v', 33, "橄榄"),
    NAVY('y', 34, "藏青"),
    TEAL('t', 36, "蓝绿"),
    PURPLE('u', 35, "紫");

    /** ESC 是终端用来识别"后面这一段要特殊显示"的控制字符。 */
    public static final String ESC = "\u001B";

    /** 复位颜色。BLANK 的 ansi 是 0，表示不着色。 */
    public static final String ANSI_RESET = ESC + "[0m";

    private final char glyph;
    private final int ansi;
    private final String cn;

    Color(char glyph, int ansi, String cn) {
        this.glyph = glyph;
        this.ansi = ansi;
        this.cn = cn;
    }

    /** 用来绘制和保存文件的字符。 */
    public char glyph() {
        return glyph;
    }

    /** ANSI 颜色编号。 */
    public int ansi() {
        return ansi;
    }

    public String cn() {
        return cn;
    }

    /** 包上 ANSI 转义序列，让终端把这个字符显示成对应颜色。 */
    public String coloredGlyph() {
        return ansi == 0 ? String.valueOf(glyph) : ESC + "[" + ansi + "m" + glyph + ANSI_RESET;
    }

    /**
     * 根据字符反查颜色。保存文件后再读回来时用得上。
     *
     * @return 找不到时返回 {@link Optional#empty()}，而不是直接抛异常或者返回 null
     */
    public static Optional<Color> fromGlyph(char glyph) {
        return Arrays.stream(values())
                .filter(c -> c.glyph == glyph)
                .findFirst();
    }

    /**
     * 根据名字查颜色，中英文都行，大小写不敏感。
     *
     * <p>还额外接受<b>单个字符</b>：用户看到屏幕上画的是 {@code x}，
     * 很自然会直接敲 {@code x} 来指代灰色。与其报错让人困惑，不如一并认了。
     *
     * @return 找不到时返回 {@link Optional#empty()}
     */
    public static Optional<Color> fromName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        String trimmed = name.trim();

        // ① 先按「原样一个字符」匹配。这样 " "（空白色）也能认出来。
        //    注意这个检查必须在 isBlank 之前：String.isBlank() 对 " " 返回 true，
        //    如果先判 isBlank 就提前返回了，空白色永远认不出来。顺序不能颠倒。
        if (name.length() == 1) {
            Optional<Color> byGlyph = fromGlyph(name.charAt(0));
            if (byGlyph.isPresent()) {
                return byGlyph;
            }
        }

        // ② 再按「去掉空白后一个字符」匹配，让 " x " 这种也能用
        if (trimmed.length() == 1) {
            Optional<Color> byGlyph = fromGlyph(trimmed.charAt(0));
            if (byGlyph.isPresent()) {
                return byGlyph;
            }
        }

        // ③ 最后按名字匹配（英文名或中文名）
        String key = trimmed.toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(c -> c.name().toLowerCase(Locale.ROOT).equals(key) || c.cn.equals(trimmed))
                .findFirst();
    }

    /** 给用户看的颜色清单，按调色板顺序。 */
    public static String paletteHelp() {
        return Arrays.stream(values())
                .filter(c -> c != BLANK)
                .map(c -> c.cn + "(" + c.name().toLowerCase(Locale.ROOT) + ")")
                .collect(Collectors.joining("  "));
    }
}

package com.pixelstudio.io;

import com.pixelstudio.exception.ProjectFormatException;
import com.pixelstudio.exception.StudioException;
import com.pixelstudio.model.Animation;
import com.pixelstudio.model.Canvas;
import com.pixelstudio.model.Color;
import com.pixelstudio.model.Frame;
import com.pixelstudio.model.Project;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 项目文件的存取。
 *
 * <p><b>为什么要自己定义文件格式？</b>
 * <p>Java 自带两种现成方案，各有各的问题：
 * <ul>
 *   <li><b>{@code ObjectOutputStream}</b>：能直接把对象存下来，但它记的是<b>字段名</b>。
 *       以后你把字段 {@code width} 改名成 {@code w}，所有老文件就全读不出来了。</li>
 *   <li><b>JSON</b>：需要第三方库（或者自己手搓一个），对学 Java 来说反而把注意力引开了。</li>
 * </ul>
 * <p>所以这里手写一个<strong>纯文本</strong>格式：文件长这样，用记事本打开就能看懂、能手改，
 * 程序升级也不影响老文件。这在真实项目里叫"向前兼容的格式设计"。
 *
 * <pre>
 *   pixelstudio 1
 *   name=我的像素画
 *   fps=8
 *   loop=true
 *   frames=2
 *   frame 0
 *   20 10
 *   @@@@...            ← 每行一个像素字符
 *   frame 1
 *   20 10
 *   ...
 * </pre>
 */
public final class ProjectStore {

    /** 文件头。带版本号，以后格式变了也好认。 */
    public static final String MAGIC = "pixelstudio";
    public static final int FORMAT_VERSION = 1;

    private ProjectStore() {
    }

    /**
     * 写文件之前，先把父目录建出来。
     *
     * <p>{@code export out/幽灵.bmp} 里的 {@code out/} 可能根本不存在。
     * 很多程序到这儿就报一句费解的 "NoSuchFileException" 就崩了 ——
     * 但用户的真实意图明明是"帮我存到这个路径"，那就帮他把目录建好。
     *
     * <p><b>这是一个通用思路</b>：命令行工具收到一个文件路径时，
     * 要么自动创建缺失的中间目录，要么给一条明确到能照着做的错误提示，
     * 千万别甩一句原始异常。
     */
    public static Path prepareTarget(Path file) {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null && !Files.isDirectory(parent)) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new StudioException("建不了目录 " + parent + "（" + e.getMessage() + "）", e);
            }
        }
        return file;
    }

    // ------------------------------------------------------------------
    //  存
    // ------------------------------------------------------------------

    public static void save(Project project, Path file) {
        prepareTarget(file);
        Animation animation = project.animation();
        try (PrintWriter writer = new PrintWriter(
                Files.newBufferedWriter(file, StandardCharsets.UTF_8))) {

            writer.println(MAGIC + " " + FORMAT_VERSION);
            writer.println("name=" + project.name());
            writer.println("fps=" + animation.fps());
            writer.println("loop=" + animation.looping());
            writer.println("frames=" + animation.frameCount());

            for (int i = 0; i < animation.frameCount(); i++) {
                Frame frame = animation.frames().get(i);
                Canvas canvas = frame.canvas();
                writer.println("frame " + i + " " + frame.name());
                writer.println(canvas.width() + " " + canvas.height());
                for (int y = 0; y < canvas.height(); y++) {
                    StringBuilder row = new StringBuilder(canvas.width());
                    for (int x = 0; x < canvas.width(); x++) {
                        row.append(canvas.get(x, y).glyph());
                    }
                    writer.println(row);
                }
            }
        } catch (IOException e) {
            // 把 IOException 包成自己的异常：调用方（命令层）就不用再 import java.io 了
            throw new StudioException("保存失败：" + file + "（" + e.getMessage() + "）", e);
        }
    }

    // ------------------------------------------------------------------
    //  读
    // ------------------------------------------------------------------

    public static Project load(Path file) {
        if (!Files.exists(file)) {
            throw new StudioException("文件不存在：" + file);
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (header == null || !header.startsWith(MAGIC)) {
                throw new ProjectFormatException("这不是一个 Pixel Studio 文件（第一行应该是 \"" + MAGIC + " 1\"）");
            }

            String name = "untitled";
            int fps = 8;
            boolean loop = true;
            int frameCount = 0;

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("frame ")) {
                    break;                      // 从第一帧开始，剩下的交给下面的循环
                }
                if (!line.isBlank() && line.contains("=")) {
                    String[] parts = line.split("=", 2);   // limit=2 很重要，名字里可能带 =
                    switch (parts[0].trim()) {
                        case "name" -> name = parts[1].trim();
                        case "fps" -> fps = parseIntSafe(parts[1], fps);
                        case "loop" -> loop = Boolean.parseBoolean(parts[1].trim());
                        case "frames" -> frameCount = parseIntSafe(parts[1], 0);
                        default -> throw new ProjectFormatException("看不懂这一行：" + line);
                    }
                }
            }

            if (frameCount <= 0) {
                throw new ProjectFormatException("文件里没有帧（frames=" + frameCount + "）");
            }

            List<Frame> frames = new ArrayList<>();
            for (int i = 0; i < frameCount; i++) {
                if (line == null || !line.startsWith("frame ")) {
                    throw new ProjectFormatException("文件在第 " + (i + 1) + " 帧之前就结束了");
                }
                frames.add(readFrame(reader, line));
                line = reader.readLine();
            }

            // 用工厂方法组装，规则（名字非空、至少一帧）都由 Project/Animation 自己把关
            Project project = Project.of(name, Animation.of(frames));
            Animation animation = project.animation();
            animation.setFps(fps);
            animation.setLooping(loop);
            animation.selectFrame(0);
            return project;

        } catch (IOException e) {
            throw new StudioException("读取失败：" + file + "（" + e.getMessage() + "）", e);
        }
    }

    private static Frame readFrame(BufferedReader reader, String headerLine) throws IOException {
        // headerLine 形如：frame 0 frame1
        String[] parts = headerLine.trim().split("\\s+");
        if (parts.length < 3) {
            throw new ProjectFormatException("帧头部格式不对：" + headerLine);
        }
        String frameName = parts[2];

        String sizeLine = reader.readLine();
        if (sizeLine == null) {
            throw new ProjectFormatException("读到 \"" + frameName + "\" 时文件就没了");
        }
        String[] size = sizeLine.trim().split("\\s+");
        if (size.length != 2) {
            throw new ProjectFormatException("画布尺寸行格式不对：" + sizeLine);
        }
        int width = parseIntSafe(size[0], -1);
        int height = parseIntSafe(size[1], -1);
        if (width <= 0 || height <= 0) {
            throw new ProjectFormatException("画布尺寸不合法：" + sizeLine);
        }

        Canvas canvas = new Canvas(width, height);
        for (int y = 0; y < height; y++) {
            String row = reader.readLine();
            if (row == null) {
                throw new ProjectFormatException("画布只读到第 " + y + " 行就结束了（应该是 " + height + " 行）");
            }
            for (int x = 0; x < width; x++) {
                char glyph = x < row.length() ? row.charAt(x) : ' ';
                // 遇到不认识的字符就当空白，而不是直接崩掉 —— 容错让手改文件也不至于全废
                canvas.set(x, y, Color.fromGlyph(glyph).orElse(Color.BLANK));
            }
        }
        return new Frame(frameName, canvas);
    }

    private static int parseIntSafe(String raw, int fallback) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // ------------------------------------------------------------------
    //  导出
    // ------------------------------------------------------------------

    /**
     * 导出成纯文本（没有 ANSI 控制字符），适合贴到聊天窗口、README、Word 里。
     *
     * <p>注意这里写死用 {@code \n} 而不是 {@link System#lineSeparator()}：
     * 导出文件是要给别人看的，Windows 的 {@code \r\n} 贴到聊天窗口或者网页上会变成乱码一样的空行。
     * <b>"显示给终端"和"写进文件"用不同的换行符</b>，这是很容易踩但很少有人注意的坑。
     */
    public static String exportAsText(Canvas canvas) {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < canvas.height(); y++) {
            for (int x = 0; x < canvas.width(); x++) {
                Color pixel = canvas.get(x, y);
                sb.append(pixel == Color.BLANK ? ' ' : pixel.glyph());
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * 导出成 PPM 图片（P3，纯文本格式的便携像素图）。
     *
     * <p>PPM 是几十年前就有的图片格式，格式简单到可以用几行代码写出来，
     * 而且 Windows 画图、浏览器、GIMP 全都能直接打开。
     * 比起为了导出一张图去引入一个图形库，这条路对学习更友好。
     *
     * <pre>
     *   P3
     *   宽 高
     *   255
     *   R G B   R G B   ...     ← 每个像素三个 0~255 的数
     * </pre>
     */
    public static void exportAsPpm(Canvas canvas, Path file) {
        prepareTarget(file);
        try (PrintWriter writer = new PrintWriter(
                Files.newBufferedWriter(file, StandardCharsets.UTF_8))) {
            writer.println("P3");
            writer.println(canvas.width() + " " + canvas.height());
            writer.println("255");
            for (int y = 0; y < canvas.height(); y++) {
                StringBuilder row = new StringBuilder();
                for (int x = 0; x < canvas.width(); x++) {
                    int[] rgb = toRgb(canvas.get(x, y));
                    if (x > 0) {
                        row.append(' ');
                    }
                    row.append(rgb[0]).append(' ').append(rgb[1]).append(' ').append(rgb[2]);
                }
                writer.println(row);
            }
        } catch (IOException e) {
            throw new StudioException("导出失败：" + file + "（" + e.getMessage() + "）", e);
        }
    }

    /** 把调色板里的颜色近似成 RGB。教学项目够用了，真实项目这里应该查一张色彩表。 */
    static int[] toRgb(Color color) {
        return switch (color) {
            case BLANK -> new int[]{255, 255, 255};
            case BLACK -> new int[]{0, 0, 0};
            case RED -> new int[]{205, 49, 49};
            case GREEN -> new int[]{13, 188, 121};
            case YELLOW -> new int[]{229, 229, 16};
            case BLUE -> new int[]{36, 114, 200};
            case MAGENTA -> new int[]{188, 63, 188};
            case CYAN -> new int[]{17, 168, 205};
            case WHITE -> new int[]{229, 229, 229};
            case GRAY -> new int[]{128, 128, 128};
            case LIGHT -> new int[]{179, 179, 179};
            case BROWN -> new int[]{139, 69, 19};
            case PINK -> new int[]{255, 105, 180};
            case LIME -> new int[]{154, 205, 50};
            case OLIVE -> new int[]{128, 128, 0};
            case NAVY -> new int[]{0, 0, 128};
            case TEAL -> new int[]{0, 128, 128};
            case PURPLE -> new int[]{160, 32, 240};
        };
    }

    /**
     * 导出成 BMP 图片 —— Windows「画图」、浏览器、Word 都能直接双击打开。
     *
     * <p><b>和 PPM 有什么不同？</b>PPM 是纯文本，好读但文件大；BMP 是二进制，
     * 结构固定、没有任何压缩，几十行代码就能写出来。
     * <b>二进制文件的所有坑都在这里</b>，值得亲手踩一遍：
     * <ul>
     *   <li><b>字节序</b>：BMP 的每个多字节整数都是「小端」（低位在前），
     *       而 Java 的文件流默认是「大端」，所以必须用 {@link #writeInt32LE} 自己转；</li>
     *   <li><b>行对齐</b>：每行像素的字节数必须是 4 的倍数，不够就得补 0，
     *       少了这个补位，文件就打不开了；</li>
     *   <li><b>像素顺序</b>：BMP 的第一行是图片的<strong>最下面</strong>一行（自下而上），
     *       而且颜色是 BGR 不是 RGB。</li>
     * </ul>
     */
    public static void exportAsBmp(Canvas canvas, Path file) {
        int width = canvas.width();
        int height = canvas.height();
        int rowBytes = (width * 3 + 3) / 4 * 4;      // 每行补齐到 4 的倍数
        int pixelDataSize = rowBytes * height;
        int headerSize = 14 + 40;                    // 文件头 + BITMAPINFOHEADER
        int fileSize = headerSize + pixelDataSize;

        prepareTarget(file);
        try (DataOutputStream out = new DataOutputStream(
                Files.newOutputStream(file))) {

            // ---- 文件头（14 字节）----
            out.write('B');
            out.write('M');
            writeInt32LE(out, fileSize);
            writeInt32LE(out, 0);            // 保留字段，写 0
            writeInt32LE(out, headerSize);   // 像素数据从第 54 字节开始

            // ---- BITMAPINFOHEADER（40 字节）----
            writeInt32LE(out, 40);           // 这个头本身的长度
            writeInt32LE(out, width);
            writeInt32LE(out, height);       // 正数 = 自下而上存
            writeInt16LE(out, 1);            // 颜色平面数
            writeInt16LE(out, 24);           // 每个像素 24 位（8 位蓝、8 位绿、8 位红）
            writeInt32LE(out, 0);            // 压缩方式：0 = 不压缩
            writeInt32LE(out, pixelDataSize);
            writeInt32LE(out, 2835);         // 水平分辨率 72 DPI
            writeInt32LE(out, 2835);         // 垂直分辨率
            writeInt32LE(out, 0);            // 调色板颜色数（真彩色不用）
            writeInt32LE(out, 0);            // 重要颜色数

            // ---- 像素数据 ----
            for (int y = height - 1; y >= 0; y--) {     // 从最后一行开始写
                for (int x = 0; x < width; x++) {
                    int[] rgb = toRgb(canvas.get(x, y));
                    out.write(rgb[2]);       // B
                    out.write(rgb[1]);       // G
                    out.write(rgb[0]);       // R
                }
                // 补齐这一行剩下的字节
                for (int pad = width * 3; pad < rowBytes; pad++) {
                    out.write(0);
                }
            }
        } catch (IOException e) {
            throw new StudioException("导出失败：" + file + "（" + e.getMessage() + "）", e);
        }
    }

    /** BMP 用「小端」存整数：低位字节在前。 */
    private static void writeInt32LE(DataOutputStream out, int value) throws IOException {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write((value >> 24) & 0xFF);
    }

    /** 16 位版本，同理。 */
    private static void writeInt16LE(DataOutputStream out, int value) throws IOException {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
    }

    /** 工具：判断扩展名，返回小写。 */
    public static String extensionOf(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1);
    }
}

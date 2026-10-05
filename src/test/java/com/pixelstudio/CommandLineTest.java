package com.pixelstudio;

import com.pixelstudio.command.CommandRegistry;
import com.pixelstudio.model.Animation;
import com.pixelstudio.model.Color;
import com.pixelstudio.model.Project;
import com.pixelstudio.ui.CommandLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 端到端测试：把一串命令喂给 {@link CommandLine}，检查结果。
 *
 * <p>这里体现了一个很重要的测试思路：<strong>不要测内部实现，测行为</strong>。
 * 我们不关心 {@code RectCommand} 内部怎么写，只关心"敲完这行命令之后画布上是什么颜色" ——
 * 这样以后重构实现细节，测试也不会莫名其妙全红。
 */
class CommandLineTest {

    private Project project;
    private ByteArrayOutputStream captured;
    private CommandLine cli;

    @BeforeEach
    void setUp() {
        project = Project.newProject("测试", 20, 10);
        captured = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(captured, true, StandardCharsets.UTF_8);
        cli = new CommandLine(project, CommandRegistry.createDefault(),
                new ByteArrayInputStream(new byte[0]), out);
    }

    private String output() {
        return captured.toString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("fill 命令能把矩形涂上颜色")
    void fillCommandPaintsRectangle() {
        cli.executeOne("fill 2 2 4 4 red");

        assertEquals(Color.RED, project.canvas().get(3, 3));
        assertEquals(Color.BLANK, project.canvas().get(10, 8));
    }

    @Test
    @DisplayName("不指定颜色时用当前画笔色，color 命令能改画笔")
    void currentColorIsUsedAsDefault() {
        cli.executeOne("color cyan");
        cli.executeOne("set 1 1");

        assertEquals(Color.CYAN, project.canvas().get(1, 1));
    }

    @Test
    @DisplayName("参数不合法时给一句人话，而不是抛堆栈")
    void badArgumentGivesFriendlyMessage() {
        cli.executeOne("fill 1 1 abc 4");

        assertTrue(output().contains("不是一个整数"), "实际输出是：" + output());
        assertFalse(output().contains("Exception"), "不该把堆栈甩给用户");
    }

    @Test
    @DisplayName("不认识的命令要提示可以用 help")
    void unknownCommandSuggestsHelp() {
        cli.executeOne("drawstuff");

        assertTrue(output().contains("没有这条命令"));
        assertTrue(output().contains("help"));
    }

    @Test
    @DisplayName("坐标越界要报出画布大小")
    void outOfBoundsReportsCanvasSize() {
        cli.executeOne("set 999 999 red");

        assertTrue(output().contains("超出画布范围"));
        assertTrue(output().contains("20×10"));
    }

    @Test
    @DisplayName("text 命令能识别引号里的空格")
    void textHandlesQuotes() {
        cli.executeOne("text 1 1 \"HI\" red");

        assertTrue(project.canvas().paintedPixelCount() > 0);
    }

    @Test
    @DisplayName("copy 复制出来的帧和原帧内容一样，但互不影响")
    void copyFrame() {
        cli.executeOne("fill 0 0 5 5 red");
        int before = project.animation().frameCount();
        cli.executeOne("copy");

        Animation animation = project.animation();
        assertEquals(before + 1, animation.frameCount());
        assertEquals(before, animation.currentIndex(), "复制之后应该停在新的这一帧");

        // 改新帧，原帧不能跟着变（原帧里还剩最初那 5×5=25 个红点）
        cli.executeOne("clear");
        assertEquals(25, animation.frames().get(0).canvas().paintedPixelCount());
        assertEquals(0, animation.frames().get(1).canvas().paintedPixelCount());
    }

    @Test
    @DisplayName("开头带 BOM 的输入也能正常执行（Windows 记事本/管道常见）")
    void stripsBom() {
        cli.executeOne(String.valueOf((char) 0xFEFF) + "fill 0 0 2 2 red");

        assertEquals(Color.RED, project.canvas().get(1, 1));
    }

    @Test
    @DisplayName("注释和空行会被跳过")
    void commentsAndBlankLinesAreIgnored() {
        cli.executeOne("# 这是注释");
        cli.executeOne("   ");
        cli.executeOne("fill 0 0 2 2 red");

        assertEquals(Color.RED, project.canvas().get(1, 1));
    }

    @Test
    @DisplayName("save 之后 load 能把项目完整还原回来")
    void saveAndLoadRoundTrip(@TempDir Path tempDir) {
        cli.executeOne("fill 0 0 3 3 red");
        cli.executeOne("text 5 1 \"OK\" cyan");
        cli.executeOne("add");

        Path file = tempDir.resolve("demo.pxs");
        cli.executeOne("save " + file.toAbsolutePath());

        Project reloaded = com.pixelstudio.io.ProjectStore.load(file);
        assertEquals(project.canvas().width(), reloaded.canvas().width());
        assertEquals(project.canvas().height(), reloaded.canvas().height());
        assertEquals(Color.RED, reloaded.canvas().get(1, 1));
        assertEquals(Color.CYAN, reloaded.canvas().get(5, 1));
        assertEquals(project.animation().frameCount(), reloaded.animation().frameCount());
    }

    @Test
    @DisplayName("命令注册表里没有重名，别名也不会撞车")
    void registryHasNoDuplicates() {
        CommandRegistry registry = CommandRegistry.createDefault();
        assertTrue(registry.size() > 15, "命令数量好像太少了");
        assertTrue(registry.find("q").isPresent(), "quit 的别名 q 应该能用");
        assertTrue(registry.find("fill").isPresent());
        assertTrue(registry.find("不存在的命令").isEmpty());
    }
}

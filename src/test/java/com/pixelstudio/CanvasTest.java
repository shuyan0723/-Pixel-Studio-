package com.pixelstudio;

import com.pixelstudio.exception.StudioException;
import com.pixelstudio.model.BitmapFont;
import com.pixelstudio.model.Canvas;
import com.pixelstudio.model.Color;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 画布的单元测试。
 *
 * <p>这些测试也是学习材料 —— 顺便可以学一下 JUnit 5 的写法。
 */
class CanvasTest {

    @Test
    @DisplayName("新建的画布全是空白，且尺寸正确")
    void newCanvasIsBlank() {
        Canvas canvas = new Canvas(20, 10);

        assertEquals(20, canvas.width());
        assertEquals(10, canvas.height());
        assertEquals(0, canvas.paintedPixelCount());
        assertEquals(Color.BLANK, canvas.get(0, 0));
        assertEquals(Color.BLANK, canvas.get(19, 9));
    }

    @Test
    @DisplayName("画布太小要报错")
    void tooSmallCanvasRejected() {
        StudioException e = assertThrows(StudioException.class, () -> new Canvas(2, 2));
        assertTrue(e.getMessage().contains("太小"));
    }

    @Test
    @DisplayName("set 越界要抛异常，但 get 越界只返回空白")
    void boundsBehaviour() {
        Canvas canvas = new Canvas(10, 10);

        assertThrows(StudioException.class, () -> canvas.set(10, 0, Color.RED));
        assertThrows(StudioException.class, () -> canvas.set(0, -1, Color.RED));
        assertEquals(Color.BLANK, canvas.get(100, 100));
    }

    @Test
    @DisplayName("实心矩形的四个角和中心都该被填上")
    void fillRect() {
        Canvas canvas = new Canvas(10, 10);
        canvas.fillRect(2, 2, 4, 4, Color.RED);

        assertEquals(Color.RED, canvas.get(2, 2));
        assertEquals(Color.RED, canvas.get(5, 5));
        assertEquals(Color.RED, canvas.get(3, 4));
        assertEquals(Color.BLANK, canvas.get(1, 1));
        assertEquals(Color.BLANK, canvas.get(6, 6));
        assertEquals(16, canvas.paintedPixelCount());
    }

    @Test
    @DisplayName("空心矩形只有边框被填上")
    void strokeRect() {
        Canvas canvas = new Canvas(10, 10);
        canvas.strokeRect(1, 1, 6, 6, Color.BLUE);

        assertEquals(Color.BLUE, canvas.get(1, 1));
        assertEquals(Color.BLUE, canvas.get(6, 6));
        assertEquals(Color.BLANK, canvas.get(3, 3));     // 中心是空的
        // 6×6 的边框 = 周长 - 4 个重复计算的角
        assertEquals(6 * 2 + (6 - 2) * 2, canvas.paintedPixelCount());
    }

    @Test
    @DisplayName("直线要经过起点和终点")
    void drawLine() {
        Canvas canvas = new Canvas(20, 20);
        canvas.drawLine(0, 0, 19, 19, Color.GREEN);

        assertEquals(Color.GREEN, canvas.get(0, 0));
        assertEquals(Color.GREEN, canvas.get(19, 19));
        assertEquals(Color.GREEN, canvas.get(10, 10));
        // 斜线 45 度，长度就是 20 个像素
        assertEquals(20, canvas.paintedPixelCount());
    }

    @Test
    @DisplayName("横线就是一条直线")
    void drawHorizontalLine() {
        Canvas canvas = new Canvas(30, 10);
        canvas.drawLine(5, 3, 9, 3, Color.CYAN);

        assertEquals(5, canvas.paintedPixelCount());
        assertEquals(Color.CYAN, canvas.get(5, 3));
        assertEquals(Color.CYAN, canvas.get(9, 3));
        assertEquals(Color.BLANK, canvas.get(10, 3));
    }

    @Test
    @DisplayName("实心圆内部的点都是实心的")
    void fillCircle() {
        Canvas canvas = new Canvas(20, 20);
        canvas.fillCircle(10, 10, 5, Color.YELLOW);

        assertEquals(Color.YELLOW, canvas.get(10, 10));
        assertEquals(Color.YELLOW, canvas.get(10, 12));
        assertEquals(Color.BLANK, canvas.get(0, 0));
        // 半径 5 的圆面积约 π·25 ≈ 78.5，误差在几格之内都算正常
        assertTrue(canvas.paintedPixelCount() > 70 && canvas.paintedPixelCount() < 90,
                "实心圆的像素数应该接近 πr²，实际是 " + canvas.paintedPixelCount());
    }

    @Test
    @DisplayName("copy 必须是真的复制，改副本不能影响原图")
    void copyIsDeep() {
        Canvas original = new Canvas(10, 10);
        original.set(5, 5, Color.RED);

        Canvas duplicate = original.copy();
        assertEquals(Color.RED, duplicate.get(5, 5));

        duplicate.set(5, 5, Color.BLUE);
        duplicate.set(0, 0, Color.GREEN);

        assertEquals(Color.RED, original.get(5, 5), "原图不应该被改到");
        assertEquals(Color.BLANK, original.get(0, 0), "原图不应该被改到");
    }

    @Test
    @DisplayName("clear 之后所有像素都变成同一个颜色")
    void clearFillsEverything() {
        Canvas canvas = Canvas.filled(10, 10, Color.RED);
        canvas.clear(Color.GREEN);

        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                assertEquals(Color.GREEN, canvas.get(x, y));
            }
        }
    }

    @Test
    @DisplayName("文字宽度按 3 像素一个字符加 1 像素间隔算")
    void textWidth() {
        assertEquals(0, BitmapFont.textWidth(""));
        assertEquals(3, BitmapFont.textWidth("A"));
        assertEquals(7, BitmapFont.textWidth("AB"));
        assertEquals(11, BitmapFont.textWidth("ABC"));
    }

    @Test
    @DisplayName("画文字之后，那几个位置应该有像素")
    void drawTextPaintsPixels() {
        Canvas canvas = new Canvas(20, 10);
        canvas.drawText(0, 0, "A", Color.RED);

        assertTrue(canvas.paintedPixelCount() > 0, "画了字却一个像素都没有");
        assertEquals(Color.RED, canvas.get(0, 0));
    }

    @Test
    @DisplayName("颜色能按名字和字符来回转换（保存文件靠这个）")
    void colorRoundTrip() {
        for (Color color : Color.values()) {
            assertEquals(color, Color.fromGlyph(color.glyph()).orElseThrow());
            assertEquals(color, Color.fromName(color.name()).orElseThrow());
            assertEquals(color, Color.fromName(color.cn()).orElseThrow());
            // 单个字符也要能认：用户看到屏幕上的 x，直接敲 x 就该是灰色
            assertEquals(color, Color.fromName(String.valueOf(color.glyph())).orElseThrow());
        }
        assertTrue(Color.fromName("不存在的颜色").isEmpty());
        assertTrue(Color.fromName("z").isEmpty(), "z 不是任何颜色的字符");
    }
}

package com.pixelstudio.model;

import java.util.Objects;

/**
 * 一帧 = 一个名字 + 一张画布。
 *
 * <p>动画就是"很多帧按顺序快速切换"。这个类本身很薄，
 * 重点看它怎么把 {@code name} 和 {@code canvas} 封装成一个整体（组合优于继承）。
 */
public class Frame {

    private String name;
    private final Canvas canvas;

    public Frame(String name, Canvas canvas) {
        this.name = Objects.requireNonNull(name, "帧名不能为空");
        this.canvas = Objects.requireNonNull(canvas, "画布不能为空");
    }

    /** 复制一帧（换个名字，内容深拷贝）。做"复制当前帧"这个功能时用。 */
    public static Frame copyOf(Frame source, String newName) {
        return new Frame(newName, source.canvas.copy());
    }

    public String name() {
        return name;
    }

    public void rename(String name) {
        this.name = Objects.requireNonNull(name, "帧名不能为空");
    }

    public Canvas canvas() {
        return canvas;
    }

    @Override
    public String toString() {
        return "Frame[" + name + ", " + canvas + "]";
    }
}

package com.pixelstudio.model;

import com.pixelstudio.exception.StudioException;

import java.util.Objects;

/**
 * 一个项目 = 名字 + 动画。
 *
 * <p>{@link #newProject(String, int, int)} 是个静态工厂方法。
 * 它的好处是把"构造一个合法项目"这件事收在一处：以后要加校验（比如名字不能为空），
 * 只改这一个地方，不用去改每一个 {@code new Project(...)} 的调用点。
 */
public class Project {

    private String name;
    private final Animation animation;

    private Project(String name, Animation animation) {
        this.name = Objects.requireNonNull(name, "项目名不能为空");
        this.animation = Objects.requireNonNull(animation, "动画不能为空");
    }

    public static Project newProject(String name, int width, int height) {
        return of(name, new Animation(new Frame("frame1", new Canvas(width, height))));
    }

    /**
     * 用一个现成的动画组装项目 —— 读文件时用这条路径。
     *
     * <p>有了这个方法，"名字不能为空"这条规则就只在 {@link #of} 里写了一次，
     * 不管项目是从零新建的还是从磁盘读来的，都会经过同一道检查。
     */
    public static Project of(String name, Animation animation) {
        if (name == null || name.isBlank()) {
            throw new StudioException("项目名不能为空");
        }
        return new Project(name.trim(), animation);
    }

    public String name() {
        return name;
    }

    public void rename(String name) {
        if (name == null || name.isBlank()) {
            throw new StudioException("项目名不能为空");
        }
        this.name = name.trim();
    }

    public Animation animation() {
        return animation;
    }

    /** 当前正在编辑的那张画布。 */
    public Canvas canvas() {
        return animation.canvas();
    }

    @Override
    public String toString() {
        return "Project[" + name + ", " + animation.frameCount() + "帧, "
                + canvas().width() + "×" + canvas().height() + "]";
    }
}

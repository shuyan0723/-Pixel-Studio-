package com.pixelstudio.model;

import com.pixelstudio.exception.StudioException;

import java.util.ArrayList;
import java.util.List;

/**
 * 动画：一个<strong>有序</strong>的帧列表，外加播放设置。
 *
 * <p><b>这个类想让你练到什么？</b>
 * <ul>
 *   <li><b>封装（Encapsulation）</b>：内部的 {@code frames} 列表对外只读，
 *       想改必须走 {@code addFrame} / {@code removeFrame}，这样"不能删到零帧"这种规则就能守住；</li>
 *   <li><b>受检不变式</b>：任何时候都得至少留一帧；</li>
 *   <li><b>索引越界</b>：Java 的 {@code List.get} 越界抛的是 {@code IndexOutOfBoundsException}，
 *       这里主动检查并换成更友好的提示。</li>
 * </ul>
 */
public class Animation {

    public static final int MIN_FPS = 1;
    public static final int MAX_FPS = 60;

    private final List<Frame> frames = new ArrayList<>();
    private int currentIndex;
    private int fps = 8;
    private boolean looping = true;

    public Animation(Frame first) {
        frames.add(first);
    }

    /**
     * 用一组现成的帧组装一个动画。
     *
     * <p>这个工厂方法存在的理由是 {@link Project#newProject} 只能造"一张空白帧"的动画，
     * 从文件读回来的动画必须走另一条路。校验"至少一帧"的规则只写一次，两边都受用。
     */
    public static Animation of(List<Frame> source) {
        if (source == null || source.isEmpty()) {
            throw new StudioException("动画至少要有一帧");
        }
        Animation animation = new Animation(source.get(0));
        source.subList(1, source.size()).forEach(animation::addFrame);
        return animation;
    }

    /** 只读视图：外部拿到的是副本，改它不会影响动画内部。 */
    public List<Frame> frames() {
        return List.copyOf(frames);
    }

    public int frameCount() {
        return frames.size();
    }

    public int currentIndex() {
        return currentIndex;
    }

    public Frame currentFrame() {
        return frames.get(currentIndex);
    }

    public Canvas canvas() {
        return currentFrame().canvas();
    }

    public int fps() {
        return fps;
    }

    public boolean looping() {
        return looping;
    }

    public void setFps(int fps) {
        if (fps < MIN_FPS || fps > MAX_FPS) {
            throw new StudioException("帧率要在 " + MIN_FPS + " ~ " + MAX_FPS + " 之间，你输入的是 " + fps);
        }
        this.fps = fps;
    }

    public void setLooping(boolean looping) {
        this.looping = looping;
    }

    public void addFrame(Frame frame) {
        frames.add(frame);
    }

    /** 在当前帧后面插一帧，自动生成不重名的帧名。 */
    public void insertAfterCurrent(Canvas canvas) {
        frames.add(currentIndex + 1, new Frame(nextFrameName(), canvas));
        currentIndex++;
    }

    /** 删掉一帧。最后一帧不能删 —— 画布总得有个底。 */
    public void removeFrame(int index) {
        if (frames.size() <= 1) {
            throw new StudioException("至少要留一帧，删不掉了");
        }
        checkIndex(index);
        frames.remove(index);
        if (currentIndex >= frames.size()) {
            currentIndex = frames.size() - 1;
        }
    }

    /** 切换到第 index 帧（0 开始数）。 */
    public void selectFrame(int index) {
        checkIndex(index);
        currentIndex = index;
    }

    /** 播放动画时用：往前一帧，走到头就按 looping 决定停下来还是绕回去。 */
    public boolean advance() {
        if (currentIndex + 1 < frames.size()) {
            currentIndex++;
            return true;
        }
        if (looping) {
            currentIndex = 0;
            return true;
        }
        return false;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= frames.size()) {
            throw new StudioException("没有第 " + index + " 帧，当前一共 " + frames.size()
                    + " 帧，合法范围 0 ~ " + (frames.size() - 1));
        }
    }

    private String nextFrameName() {
        for (int i = 1; i <= frames.size() + 1; i++) {
            String candidate = "frame" + i;
            boolean taken = frames.stream().anyMatch(f -> f.name().equals(candidate));
            if (!taken) {
                return candidate;
            }
        }
        return "frame" + frames.size();
    }
}

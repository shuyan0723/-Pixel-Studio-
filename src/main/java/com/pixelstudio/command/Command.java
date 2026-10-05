package com.pixelstudio.command;

import java.util.List;

/**
 * 一条命令。
 *
 * <p><b>这个接口想让你练到什么？</b>这是典型的<strong>面向接口编程</strong>：
 * 命令循环完全不知道 {@code RectCommand} 长什么样，它只知道"我手里有一个 {@code Command}"，
 * 拿到就调 {@link #execute}。以后加 100 条新命令，循环那行代码<strong>一个字都不用改</strong>。
 *
 * <p>用接口而不是继承一个抽象类，是因为命令之间不需要"is-a"的关系，只需要"都会用"。
 */
public interface Command {

    /** 命令名，用户敲的单词，比如 {@code rect}。 */
    String name();

    /** 一句话说明这条命令干什么，会被列在 {@code help} 里。 */
    String summary();

    /** 用法提示，比如 {@code rect <x> <y> <宽> <高> <颜色>}。出错时打印给用户看。 */
    String usage();

    /**
     * 快捷别名，可以没有。
     *
     * <p>{@code default} 方法是 Java 8 引入的：接口里可以直接写<strong>带实现</strong>的方法，
     * 实现类不想管就不用写。比"每个类都实现一遍返回空列表"省事得多。
     */
    default List<String> aliases() {
        return List.of();
    }

    /**
     * 真正干活。
     *
     * @param context 当前项目 + 当前画笔颜色 + 输出流
     * @param args    已经切分好的参数（引号里的内容不会被切开）
     * @throws com.pixelstudio.exception.StudioException 参数不对、坐标越界等"用户犯的错"
     */
    void execute(CommandContext context, List<String> args);
}

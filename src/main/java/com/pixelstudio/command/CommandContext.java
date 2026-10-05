package com.pixelstudio.command;

import com.pixelstudio.model.Color;
import com.pixelstudio.model.Project;

import java.io.PrintStream;

/**
 * 执行命令需要的所有东西，打包传进去。
 *
 * <p><b>为什么要搞这么一个"上下文"对象？</b>
 * <p>如果 {@code Command.execute} 的签名写成
 * {@code execute(Project p, Color c, PrintStream out, List<String> args)}，
 * 那么以后想再加一个东西（比如撤销栈、配色方案），就得把<strong>所有</strong>命令的签名都改一遍。
 * 打包成一个对象之后，命令的签名永远只有 {@code (context, args)} 两个参数。
 *
 * <p>这个思路在 Spring 里叫 <strong>依赖注入</strong>，在 Web 里叫"把 request 传给 handler"，
 * 是一模一样的套路。写多了你就会发现，Java 里凡是"参数多到不想一个个传"的地方，
 * 就该考虑封一个 context。
 */
public class CommandContext {

    private final CommandRegistry registry;
    private final PrintStream out;
    private Project project;
    private Color currentColor = Color.WHITE;

    /** 循环靠这个标志决定要不要退出。用可变字段表达"状态"是这里想让你体会的东西。 */
    private boolean running = true;

    public CommandContext(Project project, CommandRegistry registry, PrintStream out) {
        this.project = project;
        this.registry = registry;
        this.out = out;
    }

    public Project project() {
        return project;
    }

    /** {@code new} 命令用它把整个项目换掉。 */
    public void replaceProject(Project project) {
        this.project = project;
        this.currentColor = Color.WHITE;
    }

    /** 命令想查别的命令（比如 help）时用它。 */
    public CommandRegistry registry() {
        return registry;
    }

    public PrintStream out() {
        return out;
    }

    public Color currentColor() {
        return currentColor;
    }

    public void setCurrentColor(Color color) {
        this.currentColor = color;
    }

    public boolean isRunning() {
        return running;
    }

    public void requestExit() {
        this.running = false;
    }

    /** 普通提示。内部已经处理了换行，所以可以放心用 String.format 拼长文本。 */
    public void info(String format, Object... args) {
        out.println(args.length == 0 ? format : String.format(format, args));
    }

    public void raw(String text) {
        out.print(text);
    }

    public void println() {
        out.println();
    }
}

package com.pixelstudio.command;

import com.pixelstudio.exception.StudioException;

import java.util.ArrayList;
import java.util.List;

/**
 * 命令参数的小工具：把一行字符串切成 {@code List<String>}，并做类型转换。
 *
 * <p><b>这个类想让你练到什么？</b>
 * <ul>
 *   <li><b>不可变</b>：构造完就把列表冻结成 {@code List.copyOf}，外部改不了；</li>
 *   <li><b>包装底层异常</b>：见 {@link #parseInt} 的注释；</li>
 *   <li><b>该用 null 还是 Optional？</b>：见 README「常见设计问题」一节。</li>
 * </ul>
 */
public final class Args {

    private final List<String> values;

    private Args(List<String> values) {
        this.values = values;
    }

    /**
     * 切分一行命令。支持双引号：{@code text "HELLO WORLD"} 会得到 3 个参数。
     *
     * <p>用状态机来扫，而不是直接 {@code split(" ")}，因为 split 无法处理引号，
     * 而且连续空格会切出空串。
     */
    public static Args parse(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        boolean hasToken = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;      // 进入/离开引号
                hasToken = true;        // 记住这个引号里确实有东西
            } else if (Character.isWhitespace(c) && !inQuote) {
                if (hasToken) {
                    out.add(current.toString());
                    current.setLength(0);
                    hasToken = false;
                }
            } else {
                current.append(c);
                hasToken = true;
            }
        }
        if (inQuote) {
            throw new StudioException("引号没有成对闭合： " + line);
        }
        if (hasToken) {
            out.add(current.toString());
        }
        return new Args(List.copyOf(out));
    }

    public List<String> values() {
        return values;
    }

    public int size() {
        return values.size();
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    /** 越界时给一条"你少敲了几个参数"并且顺带告诉用户怎么用的话。 */
    public String require(int index, String usage) {
        if (index >= values.size()) {
            throw new StudioException("参数不够，用法：" + usage);
        }
        return values.get(index);
    }

    public int requireInt(int index, String usage) {
        return parseInt(require(index, usage), usage);
    }

    /** 取一个"可有可无"的整数参数，缺失就返回默认值。 */
    public int intOrDefault(int index, int fallback) {
        if (index >= values.size()) {
            return fallback;
        }
        return parseInt(values.get(index), "这里应该是一个整数");
    }

    /** 取一个"可有可无"的整数参数，缺失返回 null（由调用方决定怎么办）。 */
    public Integer optInt(int index) {
        if (index >= values.size()) {
            return null;
        }
        return parseInt(values.get(index), "这里应该是一个整数");
    }

    public String get(int index) {
        return values.get(index);
    }

    /**
     * 把字符串转成 int，出错时抛一条<strong>能看懂</strong>的错。
     *
     * <p>{@code Integer.parseInt} 自己抛的 {@code NumberFormatException} 消息是
     * {@code For input string: "abc"}，对终端用户来说毫无意义。
     * 这里演示了怎么在底层异常外面包一层自己的异常，把底层细节藏起来。
     */
    public static int parseInt(String raw, String usage) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new StudioException("\"" + raw + "\" 不是一个整数。用法：" + usage, e);
        }
    }

    /**
     * 校验参数个数。允许"必须有"的和"可以有"的两种，所以传一个范围。
     *
     * @param min 最少要几个
     * @param max 最多几个
     */
    public void checkSize(int min, int max, String usage) {
        if (values.size() < min || values.size() > max) {
            throw new StudioException("参数个数不对（给了 " + values.size() + " 个），用法：" + usage);
        }
    }

    /**
     * 把从第 index 个参数开始的所有参数重新拼回一个字符串。
     *
     * <p>{@code text 2 1 "HELLO WORLD"} 切分之后是 4 个参数，
     * 写文字的时候需要后两个原封不动地拼回来，所以要这么个方法。
     */
    public String joinFrom(int index) {
        if (index >= values.size()) {
            return "";
        }
        return String.join(" ", values.subList(index, values.size()));
    }
}

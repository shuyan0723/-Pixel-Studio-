package com.pixelstudio.command;

import com.pixelstudio.exception.StudioException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 命令注册表：命令名 -&gt; 命令实现。
 *
 * <p><b>这个类想让你练到什么？</b>
 * <ul>
 *   <li><b>Map + 泛型</b>：{@code Map<String, Command>} 让"按名字找命令"变成 O(1)；
 *       如果用 {@code List<Command>} 就得每次从头扫一遍比名字；</li>
 *   <li><b>LinkedHashMap</b>：和 HashMap 一样快，但额外保证插入顺序，
 *       所以 {@code help} 打印出来的命令顺序是可控的（{@code Map.of} 是无序的，不能用在这里）；</li>
 *   <li><b>工厂方法</b>：{@link #createDefault()} 负责把命令攒起来，
 *       调用方拿到的就是一个已经装好的对象，不用知道有哪几条命令；</li>
 *   <li><b>返回 Optional</b>：{@link #find} 在找不到时返回 {@code Optional.empty()}，
 *       调用方用 {@code orElseThrow} 把"找不到"变成一条漂亮的错误消息。</li>
 * </ul>
 */
public class CommandRegistry {

    private final Map<String, Command> byName = new LinkedHashMap<>();
    private final Map<String, String> aliasToName = new LinkedHashMap<>();

    public CommandRegistry(List<Command> commands) {
        for (Command command : commands) {
            register(command);
        }
    }

    /** 装进本程序全部内置命令。 */
    public static CommandRegistry createDefault() {
        List<Command> commands = new ArrayList<>();
        commands.addAll(SessionCommands.all());
        commands.addAll(DrawCommands.all());
        commands.addAll(FrameCommands.all());
        commands.addAll(FileCommands.all());
        return new CommandRegistry(commands);
    }

    private void register(Command command) {
        byName.put(command.name().toLowerCase(Locale.ROOT), command);
        for (String alias : command.aliases()) {
            aliasToName.put(alias.toLowerCase(Locale.ROOT), command.name().toLowerCase(Locale.ROOT));
        }
    }

    /** 找一条命令，别名也能认。 */
    public Optional<Command> find(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        Command direct = byName.get(key);
        if (direct != null) {
            return Optional.of(direct);
        }
        String target = aliasToName.get(key);
        return target == null ? Optional.empty() : Optional.ofNullable(byName.get(target));
    }

    /** 找不到就抛异常，把"没这个命令"和"命令名打错了"两种情况合并成一条友好提示。 */
    public Command require(String name) {
        return find(name).orElseThrow(() -> new StudioException(
                "没有这条命令：" + name + "（输入 help 看有哪些命令）"));
    }

    /** 按注册顺序返回全部命令，给 help 用。 */
    public List<Command> all() {
        return List.copyOf(byName.values());
    }

    public int size() {
        return byName.size();
    }
}

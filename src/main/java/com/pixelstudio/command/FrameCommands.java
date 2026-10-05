package com.pixelstudio.command;

import com.pixelstudio.exception.StudioException;
import com.pixelstudio.model.Animation;
import com.pixelstudio.model.Canvas;
import com.pixelstudio.model.Frame;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 帧和动画的命令。
 *
 * <p>这组命令里最值得看的是 {@link PlayCommand}：它演示了
 * <b>怎么在 Java 里"睡一会儿"</b>，以及怎么在循环里优雅地处理"用户按了 Ctrl+C"。
 */
final class FrameCommands {

    private FrameCommands() {
    }

    static List<Command> all() {
        return List.of(new FramesCommand(), new FrameCommand(), new AddCommand(),
                new CopyCommand(), new DelCommand(), new FpsCommand(),
                new LoopCommand(), new PlayCommand());
    }

    // ------------------------------------------------------------------

    static final class FramesCommand implements Command {

        @Override
        public String name() {
            return "frames";
        }

        @Override
        public String summary() {
            return "列出所有帧";
        }

        @Override
        public String usage() {
            return "frames";
        }

        @Override
        public void execute(CommandContext context, List<String> args) {
            Animation animation = context.project().animation();
            context.info("");
            context.info("  共 %d 帧（* 表示当前帧）", animation.frameCount());
            // Stream 的 map / Collectors.joining，是 Java 8 之后最顺手的循环写法
            String list = java.util.stream.IntStream.range(0, animation.frameCount())
                    .mapToObj(i -> {
                        Frame f = animation.frames().get(i);
                        String mark = i == animation.currentIndex() ? "*" : " ";
                        return String.format("   %s %2d. %-12s %d×%d  已涂 %d 像素",
                                mark, i, f.name(), f.canvas().width(), f.canvas().height(),
                                f.canvas().paintedPixelCount());
                    })
                    .collect(Collectors.joining(System.lineSeparator()));
            context.info("%s", list);
            context.info("");
        }
    }

    // ------------------------------------------------------------------

    static final class FrameCommand implements Command {

        @Override
        public String name() {
            return "frame";
        }

        @Override
        public String summary() {
            return "切换到某一帧";
        }

        @Override
        public String usage() {
            return "frame <序号>    序号从 0 开始；也支持 next / prev";
        }

        @Override
        public void execute(CommandContext context, List<String> args) {
            if (args.isEmpty()) {
                throw new StudioException("参数不够，用法：" + usage());
            }
            Animation animation = context.project().animation();
            String which = args.get(0).toLowerCase();

            switch (which) {
                case "next" -> {
                    if (animation.currentIndex() + 1 >= animation.frameCount()) {
                        throw new StudioException("已经是最后一帧了");
                    }
                    animation.selectFrame(animation.currentIndex() + 1);
                }
                case "prev" -> {
                    if (animation.currentIndex() - 1 < 0) {
                        throw new StudioException("已经是第一帧了");
                    }
                    animation.selectFrame(animation.currentIndex() - 1);
                }
                default -> animation.selectFrame(Args.parseInt(args.get(0), usage()));
            }
            Frame frame = animation.currentFrame();
            context.info("  切到第 %d 帧：%s", animation.currentIndex(), frame.name());
        }
    }

    // ------------------------------------------------------------------

    static final class AddCommand implements Command {

        @Override
        public String name() {
            return "add";
        }

        @Override
        public String summary() {
            return "在当前帧后面插入一张空白帧";
        }

        @Override
        public String usage() {
            return "add";
        }

        @Override
        public void execute(CommandContext context, List<String> args) {
            Animation animation = context.project().animation();
            Canvas blank = new Canvas(animation.canvas().width(), animation.canvas().height());
            animation.insertAfterCurrent(blank);
            context.info("  新建了空白帧，现在是第 %d 帧", animation.currentIndex());
        }
    }

    // ------------------------------------------------------------------

    static final class CopyCommand implements Command {

        @Override
        public String name() {
            return "copy";
        }

        @Override
        public String summary() {
            return "复制当前帧（做动画的关键一步）";
        }

        @Override
        public String usage() {
            return "copy";
        }

        @Override
        public void execute(CommandContext context, List<String> args) {
            Animation animation = context.project().animation();
            Canvas duplicate = animation.currentFrame().canvas().copy();
            animation.insertAfterCurrent(duplicate);
            context.info("  已复制，现在是第 %d 帧。改这一帧就能做出动画效果了", animation.currentIndex());
        }
    }

    // ------------------------------------------------------------------

    static final class DelCommand implements Command {

        @Override
        public String name() {
            return "del";
        }

        @Override
        public List<String> aliases() {
            return List.of("rm");
        }

        @Override
        public String summary() {
            return "删掉一帧";
        }

        @Override
        public String usage() {
            return "del <序号>";
        }

        @Override
        public void execute(CommandContext context, List<String> args) {
            if (args.isEmpty()) {
                throw new StudioException("参数不够，用法：" + usage());
            }
            Animation animation = context.project().animation();
            int index = Args.parseInt(args.get(0), usage());
            animation.removeFrame(index);
            context.info("  已删除第 %d 帧，现在剩 %d 帧", index, animation.frameCount());
        }
    }

    // ------------------------------------------------------------------

    static final class FpsCommand implements Command {

        @Override
        public String name() {
            return "fps";
        }

        @Override
        public String summary() {
            return "设置播放帧率";
        }

        @Override
        public String usage() {
            return "fps <数字>    范围 " + Animation.MIN_FPS + " ~ " + Animation.MAX_FPS;
        }

        @Override
        public void execute(CommandContext context, List<String> args) {
            if (args.isEmpty()) {
                throw new StudioException("参数不够，用法：" + usage());
            }
            Animation animation = context.project().animation();
            animation.setFps(Args.parseInt(args.get(0), usage()));
            context.info("  帧率 -> %d fps（每帧 %.0f 毫秒）",
                    animation.fps(), 1000.0 / animation.fps());
        }
    }

    // ------------------------------------------------------------------

    static final class LoopCommand implements Command {

        @Override
        public String name() {
            return "loop";
        }

        @Override
        public String summary() {
            return "开关循环播放";
        }

        @Override
        public String usage() {
            return "loop <on|off>";
        }

        @Override
        public void execute(CommandContext context, List<String> args) {
            if (args.isEmpty()) {
                throw new StudioException("参数不够，用法：" + usage());
            }
            Animation animation = context.project().animation();
            boolean on = switch (args.get(0).toLowerCase()) {
                case "on", "true", "1", "yes" -> true;
                case "off", "false", "0", "no" -> false;
                default -> throw new StudioException("只能填 on 或 off，用法：" + usage());
            };
            animation.setLooping(on);
            context.info("  循环播放 -> %s", on ? "开" : "关");
        }
    }

    // ------------------------------------------------------------------

    static final class PlayCommand implements Command {

        @Override
        public String name() {
            return "play";
        }

        @Override
        public String summary() {
            return "播放动画（Ctrl+C 可以中断）";
        }

        @Override
        public String usage() {
            return "play [播几轮]    不填就一直循环到 Ctrl+C";
        }

        @Override
        public void execute(CommandContext context, List<String> args) {
            Animation animation = context.project().animation();
            int rounds = 0;                    // 0 表示无限循环
            if (!args.isEmpty()) {
                rounds = Args.parseInt(args.get(0), usage());
                if (rounds < 1) {
                    throw new StudioException("轮数至少是 1，用法：" + usage());
                }
            }

            long frameDelay = Math.round(1000.0 / animation.fps());
            context.info("  开始播放 %s fps 的动画（%d 帧），Ctrl+C 停止",
                    animation.fps(), animation.frameCount());

            // AtomicBoolean：多线程共享一个标志位时用它，比裸的 boolean 安全。
            // 顺便练一下 lambda + try-finally 的配合。
            AtomicBoolean stopped = new AtomicBoolean(false);
            Thread watcher = new Thread(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        Thread.sleep(50);
                    }
                } catch (InterruptedException ignored) {
                    // 被中断是正常收尾，不用管
                } finally {
                    stopped.set(true);
                }
            }, "interrupt-watcher");
            // 守护线程：主线程一结束它就跟着走，不会让程序卡住不退出
            watcher.setDaemon(true);
            watcher.start();

            int played = 0;
            try {
                while (!stopped.get() && (rounds == 0 || played < rounds)) {
                    context.info("  —— 第 %d 帧 / %s ——", animation.currentIndex() + 1,
                            animation.currentFrame().name());
                    com.pixelstudio.ui.ConsoleRenderer.print(
                            context.out(), animation.canvas(), true, false);
                    Thread.sleep(frameDelay);
                    animation.advance();
                    played++;
                }
            } catch (InterruptedException e) {
                // 被 Ctrl+C 打断很正常，不算错误，安静收场就好
                Thread.currentThread().interrupt();
            } finally {
                // 无论如何都要停掉监视线程，否则它会一直赖着不走
                watcher.interrupt();
            }
            context.info("  播放结束。");
        }
    }
}

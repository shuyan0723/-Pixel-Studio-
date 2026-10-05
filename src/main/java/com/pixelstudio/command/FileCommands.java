package com.pixelstudio.command;

import com.pixelstudio.exception.StudioException;
import com.pixelstudio.io.ProjectStore;
import com.pixelstudio.model.Project;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 文件命令：保存、读取、导出。
 */
final class FileCommands {

    private FileCommands() {
    }

    static List<Command> all() {
        return List.of(new SaveCommand(), new LoadCommand(), new ExportCommand());
    }

    // ------------------------------------------------------------------

    static final class SaveCommand implements Command {

        @Override
        public String name() {
            return "save";
        }

        @Override
        public List<String> aliases() {
            return List.of("s");
        }

        @Override
        public String summary() {
            return "把项目存成 .pxs 文件";
        }

        @Override
        public String usage() {
            return "save <文件名.pxs>    不填扩展名会自动补上";
        }

        @Override
        public void execute(CommandContext context, List<String> args) {
            if (args.isEmpty()) {
                throw new StudioException("参数不够，用法：" + usage());
            }
            Path file = withExtension(Path.of(String.join(" ", args)), "pxs");
            ProjectStore.save(context.project(), file);
            Project project = context.project();
            context.info("  已保存到 %s（%s，%d 帧）",
                    file.toAbsolutePath(), project.name(), project.animation().frameCount());
        }
    }

    // ------------------------------------------------------------------

    static final class LoadCommand implements Command {

        @Override
        public String name() {
            return "load";
        }

        @Override
        public List<String> aliases() {
            return List.of("open", "l");
        }

        @Override
        public String summary() {
            return "读取一个 .pxs 文件（会丢弃当前内容）";
        }

        @Override
        public String usage() {
            return "load <文件名.pxs>";
        }

        @Override
        public void execute(CommandContext context, List<String> args) {
            if (args.isEmpty()) {
                throw new StudioException("参数不够，用法：" + usage());
            }
            Path file = withExtension(Path.of(String.join(" ", args)), "pxs");
            Project project = ProjectStore.load(file);
            context.replaceProject(project);
            context.info("  已载入「%s」：%d×%d，%d 帧",
                    project.name(), project.canvas().width(), project.canvas().height(),
                    project.animation().frameCount());
        }
    }

    // ------------------------------------------------------------------

    static final class ExportCommand implements Command {

        @Override
        public String name() {
            return "export";
        }

        @Override
        public String summary() {
            return "把当前帧导出成 .txt（字符画）、.ppm 或 .bmp（图片）";
        }

        @Override
        public String usage() {
            return "export <文件名.txt|.ppm|.bmp>";
        }

        @Override
        public void execute(CommandContext context, List<String> args) {
            if (args.isEmpty()) {
                throw new StudioException("参数不够，用法：" + usage());
            }
            Path file = Path.of(String.join(" ", args));
            String extension = ProjectStore.extensionOf(file);

            switch (extension) {
                case "txt" -> {
                    writeText(file, ProjectStore.exportAsText(context.project().canvas()));
                    context.info("  已导出字符画到 %s（复制粘贴即可分享）", file.toAbsolutePath());
                }
                case "ppm" -> {
                    ProjectStore.exportAsPpm(context.project().canvas(), file);
                    context.info("  已导出 PPM 到 %s（纯文本像素图，适合用脚本再加工）", file.toAbsolutePath());
                }
                case "bmp" -> {
                    ProjectStore.exportAsBmp(context.project().canvas(), file);
                    context.info("  已导出图片到 %s（双击用「画图」打开）", file.toAbsolutePath());
                }
                default -> throw new StudioException("只支持 .txt / .ppm / .bmp 扩展名，你给的是："
                        + (extension.isEmpty() ? "(没有扩展名)" : extension));
            }
        }

        private void writeText(Path file, String text) {
            ProjectStore.prepareTarget(file);
            try {
                Files.writeString(file, text, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new StudioException("导出失败：" + file + "（" + e.getMessage() + "）", e);
            }
        }
    }

    // ------------------------------------------------------------------

    /** 没写扩展名就补一个，省得用户纠结该敲什么后缀。 */
    private static Path withExtension(Path file, String extension) {
        return ProjectStore.extensionOf(file).isEmpty()
                ? file.resolveSibling(file.getFileName() + "." + extension)
                : file;
    }
}

package com.pixelstudio.exception;

/**
 * 读项目文件时格式不对（少了尺寸行、字符不认识、帧数对不上……）。
 *
 * <p>单独拆一个子类，是想练一下"异常继承"：上层既可以精确地 catch 它、给出"你的文件坏了"这种提示，
 * 也可以退一步 catch {@link StudioException} 把所有错误一锅端。
 */
public class ProjectFormatException extends StudioException {

    public ProjectFormatException(String message) {
        super(message);
    }

    public ProjectFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}

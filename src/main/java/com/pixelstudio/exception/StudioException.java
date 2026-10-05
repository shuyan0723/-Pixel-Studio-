package com.pixelstudio.exception;

/**
 * 本项目所有"可预期的业务错误"统一抛这个异常。
 *
 * <p><b>为什么要自己定义异常？</b>
 * <p>JDK 自带的异常（{@code IllegalArgumentException} 之类）描述的是"语言层面的错误"，
 * 而用户敲错了命令、画布坐标越界这类事，是"这个程序特有的错误"。
 * 定义自己的异常，好处有三个：
 * <ol>
 *   <li>上层只要 catch 一句 {@code StudioException}，就知道所有"用户犯的错"都长什么样；</li>
 *   <li>意外的空指针、越界这类<strong>bug</strong> 不会被误当成"用户输错了"而被吞掉；</li>
 *   <li>给终端看的提示语可以统一从这里带出去，不用在每个地方重复写。</li>
 * </ol>
 *
 * <p>继承 {@link RuntimeException}（非受检异常）是有意为之：
 * 业务错误不是"编译期就该拦下来的问题"，让它强制每个方法都写 throws 会让代码很难看。
 */
public class StudioException extends RuntimeException {

    public StudioException(String message) {
        super(message);
    }

    public StudioException(String message, Throwable cause) {
        super(message, cause);
    }
}

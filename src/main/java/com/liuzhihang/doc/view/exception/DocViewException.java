package com.liuzhihang.doc.view.exception;

/**
 * DocView 异常类
 *
 * @author liuzhihang
 * @since 2023/8/5 17:13
 */
public class DocViewException extends RuntimeException {

    /**
     * 空参构造
     */
    public DocViewException() {
    }

    /**
     * 消息构造器
     *
     * @param message 消息
     */
    public DocViewException(String message) {
        super(message);
    }

    /**
     * 堆栈 消息构造
     *
     * @param message 消息
     * @param cause   堆栈
     */
    public DocViewException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 堆栈
     *
     * @param cause 堆栈
     */
    public DocViewException(Throwable cause) {
        super(cause);
    }

}

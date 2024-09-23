package com.hztech.fastgpt.util;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.thread.ThreadUtil;
import com.hztech.exception.HzRuntimeException;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

/**
 * 重试工具类
 *
 * @author: boboo
 * @Date: 2023/5/5 13:48
 **/
@Slf4j
public final class RetryUtils {
    private RetryUtils() {

    }

    private static final int DEFAULT_RETRY_TIMES = 1;

    /**
     * 在重试机制下执行某业务 默认在Exception异常内重试1次
     *
     * @param runnable 重试业务逻辑
     */
    public static void tryExecute(Runnable runnable, String description) {
        tryExecute(DEFAULT_RETRY_TIMES, runnable, description);
    }

    /**
     * 在重试机制下执行某业务 默认在Exception异常内重试
     *
     * @param retryTimes 重试总次数
     * @param runnable   重试业务逻辑
     */
    public static void tryExecute(int retryTimes, Runnable runnable, String description) {
        tryExecute(retryTimes, runnable, Collections.singletonList(Exception.class), description);
    }

    /**
     * 在重试机制下执行某业务 需要指定期望的异常集合 不在此范围内则向上抛出
     *
     * @param retryTimes       重试总次数
     * @param runnable         重试业务逻辑
     * @param expectExceptions 期望重试的异常,不在此范围内则向上抛出
     */
    public static void tryExecute(int retryTimes, Runnable runnable, List<Class<? extends Exception>> expectExceptions, String description) {
        doTryExecute(retryTimes, runnable, expectExceptions, description);
    }

    /**
     * 在重试机制下执行某业务 需要指定期望的异常集合 不在此范围内则向上抛出
     *
     * @param retryTimes       重试总次数
     * @param runnable         重试业务逻辑
     * @param expectExceptions 期望重试的异常,不在此范围内则向上抛出
     */
    @SuppressWarnings("unchecked")
    private static void doTryExecute(int retryTimes, Runnable runnable, List<Class<? extends Exception>> expectExceptions, String description) {
        if (retryTimes < 0) {
            for (int current = 0; ; ) {
                try {
                    if (current > 0) {
                        ThreadUtil.safeSleep(3000);
                        log.info("{}开始第{}次重试", description, current);
                        if (current > 15) {
                            log.info("{}超时15次重试，睡眠半分钟", description);
                            ThreadUtil.safeSleep(30000);
                        }
                    }
                    runnable.run();
                    // 业务正常执行 跳出循环
                    break;
                } catch (Throwable throwable) {
                    if (expectExceptions.stream().anyMatch(clazz -> ExceptionUtil.getCausedBy(throwable, clazz) != null)) {
                        // 是期望中的异常 进行重试
                        // 当前重试次数
                        if ("com.spire.office.packages.sprhhla".equals(throwable.getClass().getName()) && "No have this value 10".equals(throwable.getMessage())) {
                            break;
                        }
                        current++;
                    } else {
                        // 不是期望中的异常 向上抛出
                        throw new HzRuntimeException(throwable);
                    }
                }
            }
        } else {
            for (int current = 0; current <= retryTimes; ) {
                try {
                    if (current > 0) {
                        ThreadUtil.safeSleep(3000);
                        log.info("{}开始第{}次重试", description, current);
                    }
                    runnable.run();
                    // 业务正常执行 跳出循环
                    break;
                } catch (Throwable throwable) {
                    if (expectExceptions.stream().anyMatch(clazz -> ExceptionUtil.getCausedBy(throwable, clazz) != null)) {
                        // 是期望中的异常 进行重试
                        // 当前重试次数
                        current++;
                    } else {
                        // 不是期望中的异常 向上抛出
                        throw new HzRuntimeException(throwable);
                    }
                }
            }
        }
    }
}

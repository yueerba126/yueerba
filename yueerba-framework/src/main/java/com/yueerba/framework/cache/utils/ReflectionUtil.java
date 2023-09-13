package com.yueerba.framework.cache.utils;

import java.lang.reflect.Method;

/**
 * Description:
 *
 * ReflectionUtil 类提供了一系列反射相关的工具方法。
 * 该类旨在简化反射操作，提供统一的异常处理，以及提高代码的可读性。
 *
 * Author: yueerba
 * Date: 2023/9/12
 */
public class ReflectionUtil {

    /**
     * 使用反射从给定对象中调用指定名称的方法。
     *
     * @param clazz      要反射的类
     * @param methodName 要调用的方法名
     * @param target     要调用方法的目标对象
     * @param args       方法参数
     * @return 方法的返回值
     */
    public static Object invokeMethod(Class<?> clazz, String methodName, Object target, Object... args) {
        try {
            // 查找方法
            Method method = clazz.getDeclaredMethod(methodName, getParameterTypes(args));

            // 使方法可访问（如果它是私有的）
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }

            // 调用方法并返回结果
            return method.invoke(target, args);
        } catch (Exception e) {
            // 抛出运行时异常，简化异常处理
            throw new RuntimeException("Error invoking method " + methodName + " on " + target, e);
        }
    }

    /**
     * 从对象数组中获取类类型数组。
     *
     * @param args 对象数组
     * @return 类型数组
     */
    private static Class<?>[] getParameterTypes(Object[] args) {
        Class<?>[] parameterTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            parameterTypes[i] = args[i].getClass();
        }
        return parameterTypes;
    }
}


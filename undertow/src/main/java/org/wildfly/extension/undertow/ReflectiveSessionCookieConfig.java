/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;


import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionCookieConfig;
import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.SessionConfigWrapper;

/**
 * Temporary reflective workaround to deal with the fact that SessionCookieConfig
 * is binary-incompatible in the Undertow versions used for EE 10 and EE 11.
 *
 * @deprecated will be removed as soon as WildFly settles on undertow-core 2.4.
 */
@Deprecated(forRemoval = true)
public final class ReflectiveSessionCookieConfig {
    private static final Constructor<?> CONSTRUCTOR;
    private static final Method SET_COOKIE_NAME;
    private static final Method SET_DOMAIN;
    private static final Method SET_HTTP_ONLY;
    private static final Method SET_SECURE;
    private static final Method SET_MAX_AGE;
    private static final Method SET_PATH;
    private static final Method WRAP;

    static {
        Class<?> ssc = SessionCookieConfig.class;
        try {
            CONSTRUCTOR = ssc.getConstructor();
            SET_COOKIE_NAME = ssc.getMethod("setCookieName", String.class);
            SET_DOMAIN = ssc.getMethod("setDomain", String.class);
            SET_HTTP_ONLY = ssc.getMethod("setHttpOnly", boolean.class);
            SET_SECURE = ssc.getMethod("setSecure", boolean.class);
            SET_MAX_AGE = ssc.getMethod("setMaxAge", int.class);
            SET_PATH = ssc.getMethod("setPath", String.class);
            WRAP = SessionConfigWrapper.class.getMethod("wrap", SessionConfig.class, Deployment.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private final SessionConfig target;

    public ReflectiveSessionCookieConfig() {
        try {
            target = (SessionConfig) CONSTRUCTOR.newInstance();
        } catch (InstantiationException | IllegalAccessException  | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public void setCookieName(String name) {
        invoke(SET_COOKIE_NAME, target, name);
    }

    public void setDomain(String domain) {
        invoke(SET_DOMAIN, target, domain);
    }

    public void setHttpOnly(boolean httpOnly) {
        invoke(SET_HTTP_ONLY, target, httpOnly);
    }

    public void setSecure(boolean secure) {
        invoke(SET_SECURE, target, secure);
    }

    public void setMaxAge(int maxAge) {
        invoke(SET_MAX_AGE, target, maxAge);
    }

    void setPath(String path) {
        invoke(SET_PATH, target, path);
    }

    public SessionConfig getTarget() {
        return target;
    }

    public SessionConfig wrap(SessionConfigWrapper wrapper) {
        return (SessionConfig) invoke(WRAP, wrapper, target, null);
    }

    private Object invoke(Method method, Object target, Object... args) {
        try {
            return method.invoke(target, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}

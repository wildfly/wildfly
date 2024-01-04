/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.web.common;


/**
 *
 * Contains context information that is exposed on startup via a thread local
 *
 * @author Stuart Douglas
 */
public class StartupContext {

    private static final ThreadLocal<WebInjectionContainer> INJECTION_CONTAINER = new ThreadLocal<WebInjectionContainer>();

    public static void setInjectionContainer(final WebInjectionContainer injectionContainer) {
        INJECTION_CONTAINER.set(injectionContainer);
    }

    public static WebInjectionContainer getInjectionContainer() {
        return INJECTION_CONTAINER.get();
    }

    public static void removeInjectionContainer() {
        INJECTION_CONTAINER.remove();
    }

}

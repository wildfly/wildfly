/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.web.suspend.servlet;

import java.util.function.BiConsumer;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.annotation.WebListener;

/**
 * @author Paul Ferraro
 */
@WebListener
public class SuspendStateServletRequestListener implements ServletRequestListener {
    public static final String INIT_EVENT_NAME = "init";
    public static final String DESTROY_EVENT_NAME = "destroy";

    private final BiConsumer<ServletRequest, String> recorder = new SuspendStateRecorder();

    @Override
    public void requestInitialized(ServletRequestEvent event) {
        this.recorder.accept(event.getServletRequest(), INIT_EVENT_NAME);
    }

    @Override
    public void requestDestroyed(ServletRequestEvent event) {
        this.recorder.accept(event.getServletRequest(), DESTROY_EVENT_NAME);
    }
}

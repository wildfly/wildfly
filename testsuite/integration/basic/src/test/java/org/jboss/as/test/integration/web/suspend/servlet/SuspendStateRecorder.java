/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.web.suspend.servlet;

import java.lang.management.ManagementFactory;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiConsumer;

import javax.management.JMException;
import javax.management.ObjectName;

import jakarta.servlet.ServletRequest;

/**
 * @author Paul Ferraro
 */
public class SuspendStateRecorder implements BiConsumer<ServletRequest, String> {
    private static final PrivilegedExceptionAction<String> GET_SUSPEND_STATE = new PrivilegedExceptionAction<>() {
        @Override
        public String run() throws JMException {
            return (String) ManagementFactory.getPlatformMBeanServer().getAttribute(ObjectName.getInstance("jboss.as", "management-root", "server"), "suspendState");
        }
    };

    private static final BlockingQueue<Map.Entry<String, String>> EVENTS = new LinkedBlockingQueue<>();

    @Override
    public void accept(ServletRequest request, String event) {
        Duration delay = Optional.ofNullable(request.getParameter(event)).map(Duration::parse).orElse(Duration.ZERO);
        if (!delay.isZero()) {
            try {
                Thread.sleep(delay.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        try {
            EVENTS.add(Map.entry(event, AccessController.doPrivileged(GET_SUSPEND_STATE)));
        } catch (PrivilegedActionException e) {
            throw new IllegalStateException(e.getCause());
        }
    }

    static List<Map.Entry<String, String>> drainEvents() {
        List<Map.Entry<String, String>> events = new ArrayList<>(3);
        EVENTS.drainTo(events);
        return events;
    }
}

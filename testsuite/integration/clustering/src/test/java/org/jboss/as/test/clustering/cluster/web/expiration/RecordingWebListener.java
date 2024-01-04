/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.web.expiration;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;

import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionAttributeListener;
import jakarta.servlet.http.HttpSessionBindingEvent;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

@WebListener
public class RecordingWebListener implements HttpSessionListener, HttpSessionAttributeListener {

    public static final BlockingQueue<String> createdSessions = new LinkedBlockingQueue<>();
    public static final BlockingQueue<String> destroyedSessions = new LinkedBlockingQueue<>();
    public static final ConcurrentMap<String, BlockingQueue<String>> addedAttributes = new ConcurrentHashMap<>();
    public static final ConcurrentMap<String, BlockingQueue<String>> removedAttributes = new ConcurrentHashMap<>();
    public static final ConcurrentMap<String, BlockingQueue<String>> replacedAttributes = new ConcurrentHashMap<>();

    private static void record(HttpSessionBindingEvent event, ConcurrentMap<String, BlockingQueue<String>> attributes) {
        BlockingQueue<String> set = new LinkedBlockingQueue<>();
        BlockingQueue<String> existing = attributes.putIfAbsent(event.getSession().getId(), set);
        ((existing != null) ? existing : set).add(event.getName());
    }

    @Override
    public void attributeAdded(HttpSessionBindingEvent event) {
        record(event, addedAttributes);
    }

    @Override
    public void attributeRemoved(HttpSessionBindingEvent event) {
        record(event, removedAttributes);
    }

    @Override
    public void attributeReplaced(HttpSessionBindingEvent event) {
        record(event, replacedAttributes);
    }

    private static void record(HttpSessionEvent event, BlockingQueue<String> sessions) {
        sessions.add(event.getSession().getId());
    }

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        record(event, createdSessions);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        record(event, destroyedSessions);
    }
}

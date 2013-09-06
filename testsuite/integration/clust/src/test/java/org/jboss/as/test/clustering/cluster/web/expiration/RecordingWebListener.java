/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.clustering.cluster.web.expiration;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;

import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

@WebListener
public class RecordingWebListener implements HttpSessionListener, HttpSessionAttributeListener {

    public static BlockingQueue<String> createdSessions = new LinkedBlockingQueue<>();
    public static BlockingQueue<String> destroyedSessions = new LinkedBlockingQueue<>();
    public static ConcurrentMap<String, BlockingQueue<String>> addedAttributes = new ConcurrentHashMap<>();
    public static ConcurrentMap<String, BlockingQueue<String>> removedAttributes = new ConcurrentHashMap<>();
    public static ConcurrentMap<String, BlockingQueue<String>> replacedAttributes = new ConcurrentHashMap<>();

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

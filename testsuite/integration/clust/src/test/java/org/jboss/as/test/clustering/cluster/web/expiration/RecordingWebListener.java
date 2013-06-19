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

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

@WebListener
public class RecordingWebListener implements HttpSessionListener, HttpSessionAttributeListener {

    public static List<String> createdSessions = new CopyOnWriteArrayList<String>();
    public static List<String> destroyedSessions = new CopyOnWriteArrayList<String>();
    public static ConcurrentMap<String, List<String>> addedAttributes = new ConcurrentHashMap<String, List<String>>();
    public static ConcurrentMap<String, List<String>> removedAttributes = new ConcurrentHashMap<String, List<String>>();
    public static ConcurrentMap<String, List<String>> replacedAttributes = new ConcurrentHashMap<String, List<String>>();

    private void record(HttpSessionBindingEvent event, ConcurrentMap<String, List<String>> attributes) {
        List<String> set = new CopyOnWriteArrayList<String>();
        List<String> existing = attributes.putIfAbsent(event.getSession().getId(), set);
        ((existing != null) ? existing : set).add(event.getName());
    }

    @Override
    public void attributeAdded(HttpSessionBindingEvent event) {
        this.record(event, addedAttributes);
    }

    @Override
    public void attributeRemoved(HttpSessionBindingEvent event) {
        this.record(event, removedAttributes);
    }

    @Override
    public void attributeReplaced(HttpSessionBindingEvent event) {
        this.record(event, replacedAttributes);
    }

    private void record(HttpSessionEvent event, List<String> sessions) {
        sessions.add(event.getSession().getId());
    }

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        this.record(event, createdSessions);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        this.record(event, destroyedSessions);
    }
}

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
package org.wildfly.clustering.web.catalina.session;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Valve;
import org.apache.catalina.session.ManagerBase;
import org.apache.catalina.util.LifecycleSupport;
import org.jboss.metadata.web.jboss.ReplicationConfig;
import org.wildfly.clustering.web.session.RoutingSupport;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionManager;

/**
 * {@link Manager} facade for a {@link SessionManager}.
 * @author Paul Ferraro
 */
public class ManagerFacade extends ManagerBase implements Lifecycle, RoutingSupport {
    private final SessionManager<LocalSessionContext> manager;
    private final ReplicationConfig config;
    private final LifecycleSupport support = new LifecycleSupport(this);
    private volatile Valve transactionValve;
    private volatile Valve lockValve;
    private volatile Valve routingValve;
    private volatile int rejectedSessions = 0;
    private volatile boolean started = false;

    public ManagerFacade(SessionManager<LocalSessionContext> manager, ReplicationConfig config) {
        this.manager = manager;
        this.config = config;
    }

    @Override
    public Map.Entry<String, String> parse(String id) {
        int index = id.indexOf('.');
        return (index < 0) ? new AbstractMap.SimpleImmutableEntry<String, String>(id, null) : new AbstractMap.SimpleImmutableEntry<>(id.substring(0, index), id.substring(index + 1));
    }

    @Override
    public String format(String sessionId, String routeId) {
        return (routeId != null) ? String.format("%s.%s", sessionId, routeId) : sessionId;
    }

    @Override
    public String locate(String sessionId) {
        return this.manager.locate(sessionId);
    }

    @Override
    public synchronized void start() throws LifecycleException {
        // JBoss Web likes to start the manager both when it is set in the context and again when the context is started
        // so we need to defend against sloppy lifecycle behavior
        if (this.started) return;

        this.transactionValve = new BatchValve(this.manager.getBatcher());
        this.addContextValve(this.transactionValve);
        if (this.config.getUseJK()) {
            this.routingValve = new RoutingValve(this);
            this.addContextValve(this.routingValve);
        }
        this.manager.start();
        this.started = true;
    }

    private void addContextValve(Valve valve) {
        if (this.container instanceof Pipeline) {
            ((Pipeline) this.container).addValve(valve);
        } else {
            this.container.getPipeline().addValve(valve);
        }
    }

    @Override
    public synchronized void stop() throws LifecycleException {
        if (!this.started) return;

        this.manager.stop();
        if (this.transactionValve != null) {
            this.removeContextValve(this.transactionValve);
        }
        if (this.routingValve != null) {
            this.removeContextValve(this.routingValve);
        }
        if (this.lockValve != null) {
            this.removeContextValve(this.lockValve);
        }
        this.started = false;
    }

    private void removeContextValve(Valve valve) {
        if (this.container instanceof Pipeline) {
            ((Pipeline) this.container).removeValve(valve);
        }
    }

    @Override
    public int getMaxInactiveInterval() {
        return (int) this.manager.getDefaultMaxInactiveInterval(TimeUnit.SECONDS);
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        this.manager.setDefaultMaxInactiveInterval(interval, TimeUnit.SECONDS);
    }

    @Override
    public void addLifecycleListener(LifecycleListener listener) {
        this.support.addLifecycleListener(listener);
    }

    @Override
    public LifecycleListener[] findLifecycleListeners() {
        return this.support.findLifecycleListeners();
    }

    @Override
    public void removeLifecycleListener(LifecycleListener listener) {
        this.support.removeLifecycleListener(listener);
    }

    @Override
    public void backgroundProcess() {
        // Do nothing
    }

    @Override
    public int getRejectedSessions() {
        return this.rejectedSessions;
    }

    @Override
    public void setRejectedSessions(int rejectedSessions) {
        this.rejectedSessions = rejectedSessions;
    }

    @Override
    public void load() throws ClassNotFoundException, IOException {
        // Do nothing
    }

    @Override
    public void unload() throws IOException {
        // Do nothing
    }

    @Override
    public void add(org.apache.catalina.Session session) {
        // Do nothing
    }

    @Override
    public org.apache.catalina.Session findSession(String id) {
        Session<LocalSessionContext> session = this.manager.findSession(id);
        return (session != null) ? new SessionFacade(session) : null;
    }

    @Override
    public org.apache.catalina.Session[] findSessions() {
        return new org.apache.catalina.Session[0];
    }

    @Override
    public void remove(org.apache.catalina.Session session) {
        // Do nothing
    }

    @Override
    public org.apache.catalina.Session createSession(String sessionId, Random random) {
        String id = (sessionId != null) ? sessionId : this.manager.createSessionId();
        org.apache.catalina.Session session = new SessionFacade(this.manager.createSession(id));
        session.setManager(this);
        return session;
    }

    @Override
    public int getActiveSessions() {
        return this.manager.size();
    }

    @Override
    public String listSessionIds() {
        return null;
    }

    @Override
    public String getSessionAttribute(String sessionId, String key) {
        Session<LocalSessionContext> session = this.manager.findSession(sessionId);
        if (session == null) return null;
        Object attribute = session.getAttributes().getAttribute(key);
        return (attribute != null) ? attribute.toString() : null;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public HashMap getSession(String sessionId) {
        Session session = this.manager.findSession(sessionId);
        if (session == null) return null;
        HashMap<String, Object> attributes = new HashMap<>();
        for (String name: session.getAttributes().getAttributeNames()) {
            attributes.put(name, session.getAttributes().getAttribute(name));
        }
        return attributes;
    }

    @Override
    public void expireSession(String sessionId) {
        Session<LocalSessionContext> session = this.manager.findSession(sessionId);
        if (session != null) {
            session.invalidate();
        }
    }

    @Override
    public String getLastAccessedTime(String sessionId) {
        Session<LocalSessionContext> session = this.manager.findSession(sessionId);
        if (session == null) return null;
        return session.getMetaData().getLastAccessedTime().toString();
    }

    @Override
    public String getCreationTime(String sessionId) {
        Session<LocalSessionContext> session = this.manager.findSession(sessionId);
        if (session == null) return null;
        return session.getMetaData().getCreationTime().toString();
    }
}

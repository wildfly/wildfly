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

import java.security.Principal;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpSession;

import org.apache.catalina.Manager;
import org.apache.catalina.SessionListener;
import org.wildfly.clustering.web.Batcher;
import org.wildfly.clustering.web.session.HttpSessionAdapter;
import org.wildfly.clustering.web.session.Session;

/**
 * {@link org.apache.catalina.Session} facade for a {@link Session}.
 * @author Paul Ferraro
 */
public class SessionFacade implements org.apache.catalina.Session {

    private final Manager manager;
    private final Session<LocalSessionContext> session;
    private final String internalId;
    private final Batcher batcher;

    public SessionFacade(Manager manager, Session<LocalSessionContext> session, String internalId, Batcher batcher) {
        this.manager = manager;
        this.session = session;
        this.internalId = internalId;
        this.batcher = batcher;
    }

    @Override
    public String getAuthType() {
        return this.session.getLocalContext().getAuthType();
    }

    @Override
    public void setAuthType(String authType) {
        this.session.getLocalContext().setAuthType(authType);
    }

    @Override
    public long getCreationTime() {
        return this.session.getMetaData().getCreationTime().getTime();
    }

    @Override
    public void setCreationTime(long time) {
        // Do nothing
    }

    @Override
    public String getId() {
        return this.session.getId();
    }

    @Override
    public String getIdInternal() {
        return this.internalId;
    }

    @Override
    public void setId(String id) {
        // Do nothing
    }

    @Override
    public String getInfo() {
        return String.format("%s/1.0", this.getClass().getSimpleName());
    }

    @Override
    public long getLastAccessedTime() {
        return this.session.getMetaData().getLastAccessedTime().getTime();
    }

    @Override
    public long getLastAccessedTimeInternal() {
        return this.getLastAccessedTime();
    }

    @Override
    public Manager getManager() {
        return this.manager;
    }

    @Override
    public void setManager(Manager manager) {
        // Do nothing
    }

    @Override
    public int getMaxInactiveInterval() {
        return (int) this.session.getMetaData().getMaxInactiveInterval(TimeUnit.SECONDS);
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        this.session.getMetaData().setMaxInactiveInterval(interval, TimeUnit.SECONDS);
    }

    @Override
    public void setNew(boolean isNew) {
        // Do nothing
    }

    @Override
    public Principal getPrincipal() {
        return this.session.getLocalContext().getPrincipal();
    }

    @Override
    public void setPrincipal(Principal principal) {
        this.session.getLocalContext().setPrincipal(principal);
    }

    @Override
    public HttpSession getSession() {
        return new HttpSessionAdapter(this.session);
    }

    @Override
    public void setValid(boolean isValid) {
        // Do nothing
    }

    @Override
    public boolean isValid() {
        return this.session.isValid();
    }

    @Override
    public boolean isValidInternal() {
        return this.isValid();
    }

    @Override
    public void addSessionListener(SessionListener listener) {
        this.session.getLocalContext().getSessionListeners().add(listener);
    }

    @Override
    public void removeSessionListener(SessionListener listener) {
        this.session.getLocalContext().getSessionListeners().remove(listener);
    }

    @Override
    public void access() {
        // Do nothing
    }

    @Override
    public void endAccess() {
        this.session.close();
        this.batcher.endBatch(true);
    }

    @Override
    public void expire() {
        this.session.invalidate();
        this.batcher.endBatch(true);
    }

    @Override
    public Object getNote(String name) {
        return this.session.getLocalContext().getNotes().get(name);
    }

    @Override
    public Iterator<String> getNoteNames() {
        return this.session.getLocalContext().getNotes().keySet().iterator();
    }

    @Override
    public void recycle() {
        // Do nothing
    }

    @Override
    public void removeNote(String name) {
        this.session.getLocalContext().getNotes().remove(name);
    }

    @Override
    public void setNote(String name, Object value) {
        this.session.getLocalContext().getNotes().put(name, value);
    }
}

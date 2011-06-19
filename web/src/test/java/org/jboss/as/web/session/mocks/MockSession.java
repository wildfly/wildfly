/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.web.session.mocks;

import javax.servlet.http.HttpSession;

import org.apache.catalina.session.StandardSessionFacade;
import org.jboss.as.clustering.web.OutgoingDistributableSessionData;
import org.jboss.as.web.session.ClusteredSession;
import org.jboss.as.web.session.notification.ClusteredSessionNotificationCause;

/**
 * @author Brian Stansberry
 * 
 */
public class MockSession extends ClusteredSession<OutgoingDistributableSessionData> {
    /**
     * Create a new MockSession.
     * 
     * @param manager
     */
    public MockSession(MockClusteredSessionManager manager) {
        super(manager);
    }

    @Override
    public void tellNew(ClusteredSessionNotificationCause cause) {
        // no-op
    }

    @Override
    public String getId() {
        // bypass any expiration logic
        return getIdInternal();
    }

    @Override
    public HttpSession getSession() {
        return new StandardSessionFacade(this);
    }

    @Override
    public String getInfo() {
        return info;
    }

    // Inherited abstract methods

    @Override
    protected Object getAttributeInternal(String name) {
        return null;
    }

    @Override
    public void processSessionReplication() {
    }

    @Override
    public void removeMyself() {
    }

    @Override
    public void removeMyselfLocal() {
    }

    @Override
    protected Object setAttributeInternal(String name, Object value) {
        return null;
    }

    @Override
    protected Object removeAttributeInternal(String name, boolean localCall, boolean localOnly) {
        return null;
    }

    @Override
    protected OutgoingDistributableSessionData getOutgoingSessionData() {
        return null;
    }

}

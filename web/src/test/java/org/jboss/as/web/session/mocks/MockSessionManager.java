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

import java.io.IOException;
import java.util.Random;

import org.apache.catalina.Session;
import org.jboss.as.web.session.AbstractSessionManager;
import org.jboss.metadata.web.jboss.JBossWebMetaData;

/**
 * Stubs out all the abstract JBossManager methods.
 * 
 * @author Brian Stansberry
 */
public class MockSessionManager extends AbstractSessionManager {

    /**
     * Create a new MockJBossManager.
     * 
     */
    public MockSessionManager() {
        super(new JBossWebMetaData());
    }

    @Override
    protected int getTotalActiveSessions() {
        return 0;
    }

    @Override
    protected void processExpirationPassivation() {
        // no-op
    }

    public void removeLocal(Session session) {
        // no-op
    }

    public boolean storeSession(Session session) {
        return false;
    }

    public void add(Session session) {
        // no-op
    }

    public void changeSessionId(Session session) {
        // no-op
    }

    public Session createEmptySession() {
        return null;
    }

    public Session createSession() {
        return null;
    }

    public Session createSession(String sessionId) {
        return null;
    }

    public Session findSession(String id) throws IOException {
        return null;
    }

    public Session[] findSessions() {
        return null;
    }

    public String getInfo() {
        return "MockJBossManager";
    }

    public void remove(Session session) {
        // no-op
    }

    @Override
    public String locate(String sessionId) {
        return this.getJvmRoute();
    }

    @Override
    public void changeSessionId(Session session, Random random) {
        // TODO Auto-generated method stub

    }

    @Override
    public Session createSession(String sessionId, Random random) {
        return this.createSession(sessionId);
    }
}

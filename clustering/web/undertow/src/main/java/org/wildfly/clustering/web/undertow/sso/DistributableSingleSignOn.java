/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.web.undertow.sso;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.wildfly.clustering.web.Batch;
import org.wildfly.clustering.web.sso.SSO;
import org.wildfly.clustering.web.sso.Sessions;

import io.undertow.security.api.AuthenticatedSessionManager.AuthenticatedSession;
import io.undertow.security.idm.Account;
import io.undertow.security.impl.SingleSignOn;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionManager;

/**
 * Adapts an {@link SSO} to a {@link SingleSignOn}.
 * @author Paul Ferraro
 */
public class DistributableSingleSignOn implements SingleSignOn {

    private final SSO<AuthenticatedSession, String, Void> sso;
    private final SessionManagerRegistry registry;
    private final Batch batch;

    public DistributableSingleSignOn(SSO<AuthenticatedSession, String, Void> sso, SessionManagerRegistry registry, Batch batch) {
        this.sso = sso;
        this.registry = registry;
        this.batch = batch;
    }

    @Override
    public String getId() {
        return this.sso.getId();
    }

    @Override
    public Account getAccount() {
        return this.sso.getAuthentication().getAccount();
    }

    @Override
    public String getMechanismName() {
        return this.sso.getAuthentication().getMechanism();
    }

    @Override
    public Iterator<Session> iterator() {
        Sessions<String> sessions = this.sso.getSessions();
        Set<String> deployments = sessions.getDeployments();
        List<Session> result = new ArrayList<>(deployments.size());
        for (String deployment: deployments) {
            SessionManager manager = this.registry.getSessionManager(deployment);
            if (manager != null) {
                String sessionId = sessions.getSession(deployment);
                if (sessionId != null) {
                    Session session = manager.getSession(sessions.getSession(deployment));
                    if (session != null) {
                        result.add(session);
                    }
                }
            }
        }
        return result.iterator();
    }

    @Override
    public boolean contains(Session session) {
        return this.sso.getSessions().getDeployments().contains(session.getSessionManager().getDeploymentName());
    }

    @Override
    public void add(Session session) {
        this.sso.getSessions().addSession(session.getSessionManager().getDeploymentName(), session.getId());
    }

    @Override
    public void remove(Session session) {
        this.sso.getSessions().removeSession(session.getSessionManager().getDeploymentName());
    }

    @Override
    public Session getSession(SessionManager manager) {
        String sessionId = this.sso.getSessions().getSession(manager.getDeploymentName());
        return (sessionId != null) ? manager.getSession(sessionId) : null;
    }

    @Override
    public void close() {
        this.batch.close();
    }
}

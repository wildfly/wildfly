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
package org.wildfly.clustering.web.catalina.sso;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.authenticator.SingleSignOn;
import org.apache.catalina.authenticator.SingleSignOnEntry;
import org.wildfly.clustering.web.sso.WebApplication;
import org.wildfly.clustering.web.sso.AuthenticationType;
import org.wildfly.clustering.web.sso.Credentials;
import org.wildfly.clustering.web.sso.SSO;
import org.wildfly.clustering.web.sso.Sessions;

/**
 * {@link SingleSignOnEntry} facade for an {@link SSO}.
 * @author Paul Ferraro
 */
public class SingleSignOnEntryFacade extends SingleSignOnEntry {
    private final SSO<LocalSSOContext> sso;
    private final ManagerRegistry managerRegistry;

    public SingleSignOnEntryFacade(SSO<LocalSSOContext> sso, ManagerRegistry managerRegistry) {
        this.sso = sso;
        this.managerRegistry = managerRegistry;
    }

    @Override
    public String getAuthType() {
        return this.sso.getCredentials().getAuthenticationType().name();
    }

    @Override
    public String getPassword() {
        return this.sso.getCredentials().getPassword();
    }

    @Override
    public Principal getPrincipal() {
        return this.sso.getLocalContext().getPrincipal();
    }

    @Override
    public String getUsername() {
        return this.sso.getCredentials().getUser();
    }

    @Override
    public synchronized void addSession(SingleSignOn sso, Session session) {
        this.sso.getSessions().addSession(this.managerRegistry.getApplication(session.getManager()), session.getId());
    }

    @Override
    public synchronized void removeSession(Session session) {
        this.sso.getSessions().removeSession(this.managerRegistry.getApplication(session.getManager()));
    }

    @Override
    public synchronized Session[] findSessions() {
        Sessions sessions = this.sso.getSessions();
        Set<WebApplication> applications = sessions.getApplications();
        List<Session> results = new ArrayList<>(applications.size());
        for (WebApplication application: applications) {
            String sessionId = sessions.getSession(application);
            Manager manager = this.managerRegistry.getManager(application);
            if (manager != null) {
                try {
                    results.add(manager.findSession(sessionId));
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        return results.toArray(new Session[results.size()]);
    }

    @Override
    public void updateCredentials(Principal principal, String authType, String username, String password) {
        this.sso.getLocalContext().setPrincipal(principal);
        Credentials credentials = this.sso.getCredentials();
        credentials.setAuthenticationType(AuthenticationType.valueOf(authType));
        credentials.setUser(username);
        credentials.setPassword(password);
    }
}

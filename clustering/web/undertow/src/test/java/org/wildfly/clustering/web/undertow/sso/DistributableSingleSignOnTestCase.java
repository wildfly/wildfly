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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import io.undertow.security.idm.Account;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionManager;

import java.util.Collections;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.wildfly.clustering.web.Batcher;
import org.wildfly.clustering.web.sso.Authentication;
import org.wildfly.clustering.web.sso.AuthenticationType;
import org.wildfly.clustering.web.sso.SSO;
import org.wildfly.clustering.web.sso.Sessions;

/**
 * Unit test for {@link DistributableSingleSignOn}
 * @author Paul Ferraro
 */
public class DistributableSingleSignOnTestCase {

    private final SSO<Account, String, Void> sso = mock(SSO.class);
    private final SessionManagerRegistry registry = mock(SessionManagerRegistry.class);
    private final Batcher batcher = mock(Batcher.class);
    private final DistributableSingleSignOn subject = new DistributableSingleSignOn(this.sso, this.registry, this.batcher);

    @Test
    public void getId() {
        String id = "sso";

        when(this.sso.getId()).thenReturn(id);

        String result = this.subject.getId();

        assertSame(id, result);
    }

    @Test
    public void getAccount() {
        Account account = mock(Account.class);
        Authentication<Account> authentication = mock(Authentication.class);

        when(this.sso.getAuthentication()).thenReturn(authentication);
        when(authentication.getIdentity()).thenReturn(account);

        Account result = this.subject.getAccount();

        assertSame(account, result);
    }

    @Test
    public void getMechanismName() {
        AuthenticationType type = AuthenticationType.CLIENT_CERT;
        Authentication<Account> authentication = mock(Authentication.class);

        when(this.sso.getAuthentication()).thenReturn(authentication);
        when(authentication.getType()).thenReturn(type);

        String result = this.subject.getMechanismName();

        assertEquals(HttpServletRequest.CLIENT_CERT_AUTH, result);
    }

    @Test
    public void iterator() {
        Sessions<String> sessions = mock(Sessions.class);
        SessionManager manager = mock(SessionManager.class);
        Session session = mock(Session.class);
        String deployment = "deployment";
        String sessionId = "session";

        when(this.sso.getSessions()).thenReturn(sessions);
        when(sessions.getDeployments()).thenReturn(Collections.singleton(deployment));
        when(sessions.getSession(deployment)).thenReturn(sessionId);
        when(this.registry.getSessionManager(deployment)).thenReturn(manager);
        when(manager.getSession(sessionId)).thenReturn(session);

        Iterator<Session> result = this.subject.iterator();

        assertTrue(result.hasNext());
        assertSame(session, result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void contains() {
        String deployment = "deployment";
        Session session = mock(Session.class);
        SessionManager manager = mock(SessionManager.class);
        Sessions<String> sessions = mock(Sessions.class);

        when(session.getSessionManager()).thenReturn(manager);
        when(manager.getDeploymentName()).thenReturn(deployment);
        when(this.sso.getSessions()).thenReturn(sessions);
        when(sessions.getDeployments()).thenReturn(Collections.<String>emptySet());

        boolean result = this.subject.contains(session);

        assertFalse(result);

        when(sessions.getDeployments()).thenReturn(Collections.singleton(deployment));

        result = this.subject.contains(session);

        assertTrue(result);
    }

    @Test
    public void add() {
        String deployment = "deployment";
        String sessionId = "session";
        Session session = mock(Session.class);
        SessionManager manager = mock(SessionManager.class);
        Sessions<String> sessions = mock(Sessions.class);

        when(session.getId()).thenReturn(sessionId);
        when(session.getSessionManager()).thenReturn(manager);
        when(manager.getDeploymentName()).thenReturn(deployment);
        when(this.sso.getSessions()).thenReturn(sessions);

        this.subject.add(session);

        verify(sessions).addSession(deployment, sessionId);
    }

    @Test
    public void remove() {
        String deployment = "deployment";
        Session session = mock(Session.class);
        SessionManager manager = mock(SessionManager.class);
        Sessions<String> sessions = mock(Sessions.class);

        when(session.getSessionManager()).thenReturn(manager);
        when(manager.getDeploymentName()).thenReturn(deployment);
        when(this.sso.getSessions()).thenReturn(sessions);

        this.subject.remove(session);

        verify(sessions).removeSession(deployment);
    }

    @Test
    public void getSession() {
        String deployment = "deployment";
        String sessionId = "session";
        Session session = mock(Session.class);
        SessionManager manager = mock(SessionManager.class);
        Sessions<String> sessions = mock(Sessions.class);

        when(session.getSessionManager()).thenReturn(manager);
        when(manager.getDeploymentName()).thenReturn(deployment);
        when(this.sso.getSessions()).thenReturn(sessions);
        when(sessions.getSession(deployment)).thenReturn(sessionId);
        when(manager.getSession(sessionId)).thenReturn(session);

        Session result = this.subject.getSession(manager);

        assertSame(session, result);
    }
}

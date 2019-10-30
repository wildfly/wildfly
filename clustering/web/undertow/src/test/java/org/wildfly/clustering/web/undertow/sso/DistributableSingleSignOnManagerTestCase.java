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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import io.undertow.security.api.AuthenticatedSessionManager.AuthenticatedSession;
import io.undertow.security.idm.Account;
import io.undertow.security.impl.SingleSignOn;
import io.undertow.security.impl.SingleSignOnManager;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.web.sso.SSO;
import org.wildfly.clustering.web.sso.SSOManager;

/**
 * Unit test for {@link DistributableSingleSignOnManager}
 *
 * @author Paul Ferraro
 */
public class DistributableSingleSignOnManagerTestCase {

    private final SSOManager<AuthenticatedSession, String, String, Void, Batch> manager = mock(SSOManager.class);
    private final SessionManagerRegistry registry = mock(SessionManagerRegistry.class);

    private final SingleSignOnManager subject = new DistributableSingleSignOnManager(this.manager, this.registry);

    @Test
    public void createSingleSignOn() {
        String id = "sso";
        Batcher<Batch> batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);
        Account account = mock(Account.class);
        String mechanism = HttpServletRequest.BASIC_AUTH;
        SSO<AuthenticatedSession, String, String, Void> sso = mock(SSO.class);
        ArgumentCaptor<AuthenticatedSession> authenticationCaptor = ArgumentCaptor.forClass(AuthenticatedSession.class);

        when(this.manager.createIdentifier()).thenReturn(id);
        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.createBatch()).thenReturn(batch);
        when(this.manager.createSSO(same(id), authenticationCaptor.capture())).thenReturn(sso);

        SingleSignOn result = this.subject.createSingleSignOn(account, mechanism);

        verify(batcher).suspendBatch();

        assertNotNull(result);

        AuthenticatedSession capturedAuthentication = authenticationCaptor.getValue();
        assertNotNull(capturedAuthentication);
        assertSame(capturedAuthentication.getAccount(), account);
        assertSame(capturedAuthentication.getMechanism(), mechanism);
    }

    @Test
    public void findSingleSignOnNotExists() {
        String id = "sso";
        Batcher<Batch> batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);

        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.createBatch()).thenReturn(batch);
        when(this.manager.findSSO(id)).thenReturn(null);

        SingleSignOn result = this.subject.findSingleSignOn(id);

        assertNull(result);

        verify(batch).close();
        verify(batcher, never()).suspendBatch();
    }

    @Test
    public void findSingleSignOnInvalidCharacters() {
        String id = "sso+";

        SingleSignOn result = this.subject.findSingleSignOn(id);

        assertNull(result);

        verifyZeroInteractions(this.manager);
    }

    @Test
    public void findSingleSignOn() {
        String id = "sso";
        Batcher<Batch> batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);
        SSO<AuthenticatedSession, String, String, Void> sso = mock(SSO.class);

        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.createBatch()).thenReturn(batch);
        when(this.manager.findSSO(id)).thenReturn(sso);

        SingleSignOn result = this.subject.findSingleSignOn(id);

        assertNotNull(result);

        verify(batcher).suspendBatch();
    }

    @Test
    public void removeSingleSignOn() {
        InvalidatableSingleSignOn sso = mock(InvalidatableSingleSignOn.class);

        this.subject.removeSingleSignOn(sso);

        verify(sso).invalidate();
    }
}

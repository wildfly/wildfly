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

import javax.servlet.http.HttpServletRequest;

import io.undertow.security.idm.Account;
import io.undertow.security.impl.SingleSignOn;
import io.undertow.security.impl.SingleSignOnManager;

import org.junit.Test;
import org.wildfly.clustering.web.Batch;
import org.wildfly.clustering.web.Batcher;
import org.wildfly.clustering.web.sso.Authentication;
import org.wildfly.clustering.web.sso.AuthenticationType;
import org.wildfly.clustering.web.sso.SSO;
import org.wildfly.clustering.web.sso.SSOManager;

/**
 * Unit test for {@link DistributableSingleSignOnManager}
 * @author Paul Ferraro
 */
public class DistributableSingleSignOnManagerTestCase {

    private final SSOManager<Account, String, Void> manager = mock(SSOManager.class);
    private final SessionManagerRegistry registry = mock(SessionManagerRegistry.class);

    private final SingleSignOnManager subject = new DistributableSingleSignOnManager(this.manager, this.registry);

    @Test
    public void createSingleSignOn() {
        String id = "sso";
        Batcher batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);
        Account account = mock(Account.class);
        SSO<Account, String, Void> sso = mock(SSO.class);
        Authentication<Account> authentication = mock(Authentication.class);
        String authenticationType = HttpServletRequest.BASIC_AUTH;

        when(this.manager.createIdentifier()).thenReturn(id);
        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.startBatch()).thenReturn(batch);
        when(this.manager.createSSO(id)).thenReturn(sso);
        when(sso.getAuthentication()).thenReturn(authentication);

        SingleSignOn result = this.subject.createSingleSignOn(account, authenticationType);

        assertNotNull(result);

        verify(authentication).setIdentity(account);
        verify(authentication).setType(AuthenticationType.BASIC);
        verify(batch).close();
    }

    @Test
    public void findSingleSignOn() {
        String id = "sso";
        Batcher batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);

        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.startBatch()).thenReturn(batch);
        when(this.manager.findSSO(id)).thenReturn(null);

        SingleSignOn result = this.subject.findSingleSignOn(id);

        assertNull(result);

        verify(batch).close();
        reset(batch);

        SSO<Account, String, Void> sso = mock(SSO.class);

        when(this.manager.findSSO(id)).thenReturn(sso);

        result = this.subject.findSingleSignOn(id);

        assertNotNull(result);

        verify(batch).close();
    }

    @Test
    public void removeSingleSignOn() {
        String id = "sso";
        Batcher batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);

        when(this.manager.getBatcher()).thenReturn(batcher);
        when(batcher.startBatch()).thenReturn(batch);
        when(this.manager.findSSO(id)).thenReturn(null);

        this.subject.removeSingleSignOn(id);

        verify(batch).close();
        reset(batch);

        SSO<Account, String, Void> sso = mock(SSO.class);

        when(this.manager.findSSO(id)).thenReturn(sso);

        this.subject.removeSingleSignOn(id);

        verify(sso).invalidate();
        verify(batch).close();
    }
}

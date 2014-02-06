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

package org.wildfly.clustering.web.infinispan.sso.coarse;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.jboss.as.clustering.infinispan.invoker.Mutator;
import org.junit.Test;
import org.wildfly.clustering.web.sso.Authentication;
import org.wildfly.clustering.web.sso.AuthenticationType;

/**
 * @author Paul Ferraro
 */
public class CoarseAuthenticationTestCase {
    private final Mutator mutator = mock(Mutator.class);
    private final CoarseAuthenticationEntry<Object, ?, ?> entry = mock(CoarseAuthenticationEntry.class);

    private final Authentication<Object> authentication = new CoarseAuthentication<>(this.entry, this.mutator);

    @Test
    public void getIdentity() {
        Object identity = new Object();

        when(this.entry.getIdentity()).thenReturn(identity);

        Object result = this.authentication.getIdentity();

        assertSame(identity, result);

        verify(this.mutator, never()).mutate();
    }

    @Test
    public void setIdentity() {
        Object identity = new Object();

        this.authentication.setIdentity(identity);

        verify(this.entry).setIdentity(identity);
        verify(this.mutator).mutate();
    }

    @Test
    public void getType() {
        AuthenticationType type = AuthenticationType.BASIC;

        when(this.entry.getType()).thenReturn(type);

        Object result = this.authentication.getType();

        assertSame(type, result);

        verify(this.mutator, never()).mutate();
    }

    @Test
    public void setType() {
        AuthenticationType type = AuthenticationType.BASIC;

        this.authentication.setType(type);

        verify(this.entry).setAuthenticationType(type);
        verify(this.mutator).mutate();
    }
}

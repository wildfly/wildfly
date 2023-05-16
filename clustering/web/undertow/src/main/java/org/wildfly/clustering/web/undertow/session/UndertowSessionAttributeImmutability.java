/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.undertow.session;

import java.util.Arrays;

import org.wildfly.clustering.ee.Immutability;
import org.wildfly.clustering.ee.immutable.SimpleImmutability;
import org.wildfly.elytron.web.undertow.server.servlet.ServletSecurityContextImpl.IdentityContainer;
import org.wildfly.security.cache.CachedIdentity;

import io.undertow.security.api.AuthenticatedSessionManager.AuthenticatedSession;
import io.undertow.servlet.util.SavedRequest;

/**
 * @author Paul Ferraro
 */
public enum UndertowSessionAttributeImmutability implements Immutability {
    CLASSES(new SimpleImmutability(Arrays.asList(AuthenticatedSession.class, SavedRequest.class, CachedIdentity.class, IdentityContainer.class))),
    ;
    private final Immutability immutability;

    UndertowSessionAttributeImmutability(Immutability immutability) {
        this.immutability = immutability;
    }

    @Override
    public boolean test(Object object) {
        return this.immutability.test(object);
    }
}

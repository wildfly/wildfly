/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

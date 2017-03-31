/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow.security;

import java.util.UUID;

import org.jboss.as.security.DigestCredential;

/**
 * Implementation to map from the Undertow variant to the WildFly variant.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
final class DigestCredentialImpl implements DigestCredential {

    private final String uniqueValue = UUID.randomUUID().toString();
    private final io.undertow.security.idm.DigestCredential wrapped;

    DigestCredentialImpl(final io.undertow.security.idm.DigestCredential wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public boolean verifyHA1(byte[] ha1) {
        return wrapped.verifyHA1(ha1);
    }

    @Override
    public String getRealm() {
        return wrapped.getRealm();
    }

    @Override
    public String toString() {
        // Not actually used for validation in a correctly configured installation, however a randomly generated UUID is
        // returned for bad configurations.
        return uniqueValue;
    }

}

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

package org.wildfly.test.integration.security.picketlink.idm.credentials;

import org.picketlink.idm.credential.Credentials;
import org.picketlink.idm.credential.handler.CredentialHandler;
import org.picketlink.idm.credential.handler.annotations.SupportsCredentials;
import org.picketlink.idm.model.Account;
import org.picketlink.idm.spi.CredentialStore;
import org.picketlink.idm.spi.IdentityContext;

import java.util.Date;

/**
 * @author Pedro Igor
 */
@SupportsCredentials(
    credentialClass = { CustomCredential.class, CustomCredential.class },
    credentialStorage = SupportsCredentials.NO_CREDENTIAL_STORAGE.class
)
public class CustomCredentialHandler<S extends CredentialStore<?>, V extends CustomCredential, U extends CustomCredential> implements CredentialHandler<S, V, U> {

    @Override
    public void validate(IdentityContext context, V credentials, S store) {
        credentials.setStatus(Credentials.Status.INVALID);

        if ("valid_token".equals(credentials.getToken())) {
            credentials.setStatus(Credentials.Status.VALID);
        }
    }

    @Override
    public void update(IdentityContext context, Account account, U credential, S store, Date effectiveDate, Date expiryDate) {
        // no-op
    }

    @Override
    public void setup(S store) {

    }
}

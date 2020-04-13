/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.IndexSerializer;
import org.wildfly.extension.undertow.security.AccountImpl;

import io.undertow.security.api.AuthenticatedSessionManager.AuthenticatedSession;
import io.undertow.security.idm.Account;

/**
 * Externalizer for an {@link AuthenticatedSession}.
 * @author Paul Ferraro
 */
@MetaInfServices(Externalizer.class)
public class AuthenticatedSessionExternalizer implements Externalizer<AuthenticatedSession> {

    @Override
    public void writeObject(ObjectOutput output, AuthenticatedSession session) throws IOException {
        AccountImpl account = (AccountImpl) session.getAccount();
        output.writeUTF(session.getMechanism());
        output.writeUTF(account.getPrincipal().getName());
        Set<String> roles = account.getRoles();
        IndexSerializer.VARIABLE.writeInt(output, roles.size());
        for (String role : roles) {
            output.writeUTF(role);
        }
        output.writeObject(account.getCredential());
        Principal original = account.getOriginalPrincipal();
        output.writeUTF((original != null) ? original.getName() : null);
    }

    @Override
    public AuthenticatedSession readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        String mechanism = input.readUTF();
        Principal principal = new AccountImpl(input.readUTF()).getPrincipal();
        int roleCount = IndexSerializer.VARIABLE.readInt(input);
        List<String> roles = new ArrayList<>(roleCount);
        for (int i = 0; i < roleCount; ++i) {
            roles.add(input.readUTF());
        }
        Object credential = input.readObject();
        Principal original = new AccountImpl(input.readUTF()).getPrincipal();
        Account account = new AccountImpl(principal, new CopyOnWriteArraySet<>(roles), credential, original);
        return new AuthenticatedSession(account, mechanism);
    }

    @Override
    public Class<AuthenticatedSession> getTargetClass() {
        return AuthenticatedSession.class;
    }
}

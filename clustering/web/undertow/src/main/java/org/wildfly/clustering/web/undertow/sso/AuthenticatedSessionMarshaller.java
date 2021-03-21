/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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
import java.security.Principal;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.servlet.http.HttpServletRequest;

import org.infinispan.protostream.impl.WireFormat;
import org.wildfly.clustering.marshalling.protostream.Any;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.extension.undertow.security.AccountImpl;

import io.undertow.security.api.AuthenticatedSessionManager.AuthenticatedSession;
import io.undertow.security.idm.Account;

/**
 * @author Paul Ferraro
 */
public class AuthenticatedSessionMarshaller implements ProtoStreamMarshaller<AuthenticatedSession> {

    private static final int MECHANISM_INDEX = 1;
    private static final int PRINCIPAL_INDEX = 2;
    private static final int ROLE_INDEX = 3;
    private static final int CREDENTIAL_INDEX = 4;
    private static final int ORIGINAL_INDEX = 5;

    private static final String DEFAULT_MECHANISM = HttpServletRequest.FORM_AUTH;

    @Override
    public AuthenticatedSession readFrom(ProtoStreamReader reader) throws IOException {
        String mechanism = DEFAULT_MECHANISM;
        Principal principal = null;
        List<String> roles = new LinkedList<>();
        Object credential = null;
        Principal original = null;
        boolean reading = true;
        while (reading) {
            int tag = reader.readTag();
            switch (WireFormat.getTagFieldNumber(tag)) {
                case MECHANISM_INDEX:
                    mechanism = reader.readString();
                    break;
                case PRINCIPAL_INDEX:
                    principal = new AccountImpl(reader.readString()).getPrincipal();
                    break;
                case ROLE_INDEX:
                    roles.add(reader.readString());
                    break;
                case CREDENTIAL_INDEX:
                    credential = reader.readObject(Any.class).get();
                    break;
                case ORIGINAL_INDEX:
                    original = new AccountImpl(reader.readString()).getPrincipal();
                    break;
                default:
                    reading = reader.ignoreField(tag);
            }
        }
        Account account = new AccountImpl(principal, new CopyOnWriteArraySet<>(roles), credential, original);
        return new AuthenticatedSession(account, mechanism);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, AuthenticatedSession auth) throws IOException {
        String mechanism = auth.getMechanism();
        if (!mechanism.equals(DEFAULT_MECHANISM)) {
            writer.writeString(MECHANISM_INDEX, mechanism);
        }
        AccountImpl account = (AccountImpl) auth.getAccount();
        Principal principal = account.getPrincipal();
        if (principal != null) {
            writer.writeString(PRINCIPAL_INDEX, principal.getName());
        }
        for (String role : account.getRoles()) {
            writer.writeString(ROLE_INDEX, role);
        }
        Object credential = account.getCredential();
        if (credential != null) {
            writer.writeObject(CREDENTIAL_INDEX, new Any(credential));
        }
        Principal original = account.getOriginalPrincipal();
        if (original != null) {
            writer.writeString(ORIGINAL_INDEX, original.getName());
        }
    }

    @Override
    public Class<? extends AuthenticatedSession> getJavaClass() {
        return AuthenticatedSession.class;
    }
}

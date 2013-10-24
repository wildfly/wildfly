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
package org.jboss.as.controller.remote;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.InetAddress;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.security.auth.Subject;

import org.jboss.as.controller.client.impl.ModelControllerProtocol;
import org.jboss.as.controller.security.InetAddressPrincipal;
import org.jboss.as.core.security.RealmGroup;
import org.jboss.as.core.security.RealmRole;
import org.jboss.as.core.security.RealmUser;
import org.jboss.as.protocol.mgmt.ProtocolUtils;

/**
 * Utility for writing and reading Subjects.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class SubjectProtocolUtil {

    private static final byte REALM_USER_PRINCIPAL = 0x01;
    private static final byte REALM_GROUP_PRINCIPAL = 0x02;
    private static final byte REALM_ROLE_PRINCIPAL = 0x03;
    private static final byte INET_ADDRESS_PRINCIPAL = 0x04;

    private static final byte PRINCIPALS_PARAM = 0x05;

    private static final byte REALM_PARAM = 0x06;
    private static final byte NAME_PARAM = 0x07;
    private static final byte HOST_PARAM = 0x08;
    private static final byte ADDR_PARAM = 0x09;


    private static final Map<Byte, PrincipalHandlerFactory> HANDLERS;

    static {
        Map<Byte, PrincipalHandlerFactory> handlers = new HashMap<Byte, PrincipalHandlerFactory>(4);
        handlers.put(REALM_USER_PRINCIPAL, new RealmUserHandlerFactory());
        handlers.put(REALM_GROUP_PRINCIPAL, new RealmGroupHandlerFactory());
        handlers.put(REALM_ROLE_PRINCIPAL, new RealmRoleHandlerFactory());
        handlers.put(INET_ADDRESS_PRINCIPAL, new InetAddressHandlerFactory());

        HANDLERS = Collections.unmodifiableMap(handlers);
    }

    static void write(DataOutput output, Subject subject) throws IOException {
        output.writeByte(ModelControllerProtocol.PARAM_SUBJECT_LENGTH);
        if (subject != null) {
            Collection<Principal> principals = subject.getPrincipals();
            Collection<PrincipalWriter> writers = new ArrayList<PrincipalWriter>(principals.size());
            for (Principal current : principals) {
                PrincipalWriter writer = findWriter(current);
                if (writer != null) {
                    writers.add(writer);
                }
            }

            output.writeInt(1); // 1 Subject (Would never be more than 1!)
            output.write(PRINCIPALS_PARAM);
            output.writeInt(writers.size()); // Number of principals being written.
            for (PrincipalWriter current : writers) {
                current.write(output);
            }
        } else {
            output.writeInt(0);
        }
    }

    private static PrincipalWriter findWriter(final Principal principal) {
        PrincipalWriter writer = null;
        Iterator<PrincipalHandlerFactory> it = HANDLERS.values().iterator();
        while (it.hasNext() && writer == null) {
            writer = it.next().handlerFor(principal);
        }
        return writer;
    }

    static Subject read(DataInput input) throws IOException {
        ProtocolUtils.expectHeader(input, ModelControllerProtocol.PARAM_SUBJECT_LENGTH);
        final int size = input.readInt();
        final Subject subject;
        if (size == 1) {
            subject = new Subject();
            Collection<Principal> principals = subject.getPrincipals();
            ProtocolUtils.expectHeader(input, PRINCIPALS_PARAM);
            int principalCount = input.readInt();

            for (int i = 0; i < principalCount; i++) {
                byte type = input.readByte();
                PrincipalReader reader = findReader(type);
                if (reader == null) {
                    throw MESSAGES.unsupportedPrincipalType(type);
                }
                principals.add(reader.read(input));
            }
        } else {
            subject = null;
        }
        return subject;
    }

    private static PrincipalReader findReader(final byte type) {
        PrincipalReader reader = null;
        PrincipalHandlerFactory handlerFactory = HANDLERS.get(type);
        if (handlerFactory != null) {
            reader = handlerFactory.handlerFor(type);
        }
        return reader;
    }

    private interface PrincipalWriter {

        void write(DataOutput out) throws IOException;
    }

    private interface PrincipalReader {

        Principal read(DataInput in) throws IOException;

    }

    private interface PrincipalHandlerFactory {

        PrincipalWriter handlerFor(final Principal principal);

        PrincipalReader handlerFor(final byte value);

    }

    private static class RealmUserHandlerFactory implements PrincipalHandlerFactory {

        private PrincipalReader READER = new PrincipalReader() {

            @Override
            public Principal read(DataInput in) throws IOException {
                byte paramType = in.readByte();
                String realm = null;
                String name = null;
                if (paramType == REALM_PARAM) {
                    realm = in.readUTF();
                    paramType = in.readByte();
                }
                if (paramType == NAME_PARAM) {
                    name = in.readUTF();
                } else {
                    throw MESSAGES.unsupportedPrincipalParameter(paramType, REALM_USER_PRINCIPAL);
                }

                return realm == null ? new RealmUser(name) : new RealmUser(realm, name);
            }
        };

        @Override
        public PrincipalWriter handlerFor(final Principal principal) {
            if (principal instanceof RealmUser) {
                return new PrincipalWriter() {

                    @Override
                    public void write(DataOutput out) throws IOException {
                        RealmUser user = (RealmUser) principal;
                        out.write(REALM_USER_PRINCIPAL);

                        String realm = user.getRealm();
                        if (realm != null) {
                            out.write(REALM_PARAM);
                            out.writeUTF(realm);
                        }
                        out.write(NAME_PARAM);
                        out.writeUTF(user.getName());
                    }
                };
            }
            return null;
        }

        @Override
        public PrincipalReader handlerFor(byte value) {
            if (value == REALM_USER_PRINCIPAL) {
                return READER;
            }
            return null;
        }

    }

    private static class RealmGroupHandlerFactory implements PrincipalHandlerFactory {

        private PrincipalReader READER = new PrincipalReader() {

            @Override
            public Principal read(DataInput in) throws IOException {
                byte paramType = in.readByte();
                String realm = null;
                String name = null;
                if (paramType == REALM_PARAM) {
                    realm = in.readUTF();
                    paramType = in.readByte();
                }
                if (paramType == NAME_PARAM) {
                    name = in.readUTF();
                } else {
                    throw MESSAGES.unsupportedPrincipalParameter(paramType, REALM_GROUP_PRINCIPAL);
                }

                return realm == null ? new RealmGroup(name) : new RealmGroup(realm, name);
            }
        };

        @Override
        public PrincipalWriter handlerFor(final Principal principal) {
            if (principal instanceof RealmGroup) {
                return new PrincipalWriter() {

                    @Override
                    public void write(DataOutput out) throws IOException {
                        RealmGroup group = (RealmGroup) principal;
                        out.write(REALM_GROUP_PRINCIPAL);

                        String realm = group.getRealm();
                        if (realm != null) {
                            out.write(REALM_PARAM);
                            out.writeUTF(realm);
                        }
                        out.write(NAME_PARAM);
                        out.writeUTF(group.getName());
                    }
                };
            }
            return null;
        }

        @Override
        public PrincipalReader handlerFor(byte value) {
            if (value == REALM_GROUP_PRINCIPAL) {
                return READER;
            }
            return null;
        }

    }

    private static class RealmRoleHandlerFactory implements PrincipalHandlerFactory {

        private PrincipalReader READER = new PrincipalReader() {

            @Override
            public Principal read(DataInput in) throws IOException {
                byte paramType = in.readByte();
                String name = null;
                if (paramType == NAME_PARAM) {
                    name = in.readUTF();
                } else {
                    throw MESSAGES.unsupportedPrincipalParameter(paramType, REALM_ROLE_PRINCIPAL);
                }

                return new RealmRole(name);
            }
        };

        @Override
        public PrincipalWriter handlerFor(final Principal principal) {
            if (principal instanceof RealmRole) {
                return new PrincipalWriter() {

                    @Override
                    public void write(DataOutput out) throws IOException {
                        RealmRole role = (RealmRole) principal;
                        out.write(REALM_ROLE_PRINCIPAL);

                        out.write(NAME_PARAM);
                        out.writeUTF(role.getName());
                    }
                };
            }
            return null;
        }

        @Override
        public PrincipalReader handlerFor(byte value) {
            if (value == REALM_ROLE_PRINCIPAL) {
                return READER;
            }
            return null;
        }

    }

    private static class InetAddressHandlerFactory implements PrincipalHandlerFactory {

        private PrincipalReader READER = new PrincipalReader() {

            @Override
            public Principal read(DataInput in) throws IOException {
                byte paramType = in.readByte();
                String host;
                byte[] addr;
                if (paramType == HOST_PARAM) {
                    host = in.readUTF();
                } else {
                    throw MESSAGES.unsupportedPrincipalParameter(paramType, INET_ADDRESS_PRINCIPAL);
                }

                paramType = in.readByte();
                if (paramType == ADDR_PARAM) {
                    int length = in.readInt();
                    addr = new byte[length];
                    in.readFully(addr);
                } else {
                    throw MESSAGES.unsupportedPrincipalParameter(paramType, INET_ADDRESS_PRINCIPAL);
                }

                InetAddress address = InetAddress.getByAddress(host, addr);

                return new InetAddressPrincipal(address);
            }
        };

        @Override
        public PrincipalWriter handlerFor(final Principal principal) {
            if (principal instanceof InetAddressPrincipal) {
                return new PrincipalWriter() {

                    @Override
                    public void write(DataOutput out) throws IOException {
                        InetAddressPrincipal inetPrin = (InetAddressPrincipal) principal;
                        out.write(INET_ADDRESS_PRINCIPAL);

                        InetAddress address = inetPrin.getInetAddress();
                        String host = address.getHostName();
                        byte[] addr = address.getAddress();

                        out.write(HOST_PARAM);
                        out.writeUTF(host);
                        out.write(ADDR_PARAM);
                        out.writeInt(addr.length);
                        out.write(addr);
                    }
                };
            }
            return null;
        }

        @Override
        public PrincipalReader handlerFor(byte value) {
            if (value == INET_ADDRESS_PRINCIPAL) {
                return READER;
            }
            return null;
        }

    }
}

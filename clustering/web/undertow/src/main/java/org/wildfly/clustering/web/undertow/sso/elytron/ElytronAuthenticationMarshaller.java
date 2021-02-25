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

package org.wildfly.clustering.web.undertow.sso.elytron;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.infinispan.protostream.impl.WireFormat;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Marshaller for {@link ElytronAuthentication}.
 * @author Paul Ferraro
 */
public class ElytronAuthenticationMarshaller implements ProtoStreamMarshaller<ElytronAuthentication> {

    private static final int MECHANISM_INDEX = 1;
    private static final int NAME_INDEX = 2;
    private static final int PROGRAMMATIC_INDEX = 3;

    private static final String DEFAULT_MECHANISM = HttpServletRequest.FORM_AUTH;

    @Override
    public ElytronAuthentication readFrom(ProtoStreamReader reader) throws IOException {
        String mechanism = DEFAULT_MECHANISM;
        String name = null;
        boolean programmatic = false;
        boolean reading = true;
        while (reading) {
            int tag = reader.readTag();
            switch (WireFormat.getTagFieldNumber(tag)) {
                case MECHANISM_INDEX:
                    mechanism = reader.readString();
                    break;
                case NAME_INDEX:
                    name = reader.readString();
                    break;
                case PROGRAMMATIC_INDEX:
                    programmatic = reader.readBool();
                    break;
                default:
                    reading = (tag != 0) && reader.skipField(tag);
            }
        }
        return new ElytronAuthentication(mechanism, programmatic, name);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, ElytronAuthentication auth) throws IOException {
        String mechanism = auth.getMechanism();
        if (!mechanism.equals(DEFAULT_MECHANISM)) {
            writer.writeString(MECHANISM_INDEX, mechanism);
        }
        String name = auth.getName();
        if (name != null) {
            writer.writeString(NAME_INDEX, name);
        }
        boolean programmatic = auth.isProgrammatic();
        if (programmatic) {
            writer.writeBool(PROGRAMMATIC_INDEX, programmatic);
        }
    }

    @Override
    public Class<? extends ElytronAuthentication> getJavaClass() {
        return ElytronAuthentication.class;
    }
}

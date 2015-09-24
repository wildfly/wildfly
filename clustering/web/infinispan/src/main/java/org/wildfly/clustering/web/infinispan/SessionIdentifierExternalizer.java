/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.infinispan;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Base64;

import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.jboss.IndexExternalizer;

/**
 * Strategies for externalizing a session identifier.
 * @author Paul Ferraro
 */
public enum SessionIdentifierExternalizer implements Externalizer<String> {
    UTF8() {
        @Override
        public void writeObject(ObjectOutput output, String id) throws IOException {
            output.writeUTF(id);
        }

        @Override
        public String readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            return input.readUTF();
        }
    },
    /**
     * Specific optimization for identifiers created by {@link io.undertow.server.session.SecureRandomSessionIdGenerator}.
     */
    BASE64() {
        @Override
        public void writeObject(ObjectOutput output, String id) throws IOException {
            byte[] bytes = Base64.getUrlDecoder().decode(id);
            IndexExternalizer.UNSIGNED_BYTE.writeObject(output, bytes.length);
            output.write(bytes);
        }

        @Override
        public String readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            byte[] decoded = new byte[IndexExternalizer.UNSIGNED_BYTE.readObject(input)];
            input.read(decoded);
            return Base64.getUrlEncoder().encodeToString(decoded);
        }
    },
    ;

    @Override
    public Class<? extends String> getTargetClass() {
        return String.class;
    }
}

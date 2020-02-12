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
package org.wildfly.clustering.web.hotrod.session.fine;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.UUID;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.DefaultExternalizer;
import org.wildfly.clustering.marshalling.spi.Serializer;
import org.wildfly.clustering.web.cache.SessionIdentifierSerializer;

/**
 * Externalizer for a {@link SessionAttributeKey}.
 * @author Paul Ferraro
 */
@MetaInfServices(Externalizer.class)
public class SessionAttributeKeyExternalizer implements Externalizer<SessionAttributeKey> {
    private static final Serializer<String> IDENTIFIER_SERIALIZER = SessionIdentifierSerializer.INSTANCE;

    @Override
    public void writeObject(ObjectOutput output, SessionAttributeKey key) throws IOException {
        IDENTIFIER_SERIALIZER.write(output, key.getId());
        DefaultExternalizer.UUID.cast(UUID.class).writeObject(output, key.getAttributeId());
    }

    @Override
    public SessionAttributeKey readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        String id = IDENTIFIER_SERIALIZER.read(input);
        UUID attributeId = DefaultExternalizer.UUID.cast(UUID.class).readObject(input);
        return new SessionAttributeKey(id, attributeId);
    }

    @Override
    public Class<SessionAttributeKey> getTargetClass() {
        return SessionAttributeKey.class;
    }
}

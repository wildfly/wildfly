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

package org.wildfly.clustering.web.infinispan.session;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.time.Duration;
import java.time.Instant;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.IndexExternalizer;
import org.wildfly.clustering.marshalling.spi.time.InstantExternalizer;

/**
 * Externalizer for {@link SessionCreationMetaDataEntry}
 * @author Paul Ferraro
 */
@MetaInfServices(Externalizer.class)
public class SessionCreationMetaDataEntryExternalizer implements Externalizer<SessionCreationMetaDataEntry<Object>> {
    private static final Externalizer<Instant> INSTANT_EXTERNALIZER = new InstantExternalizer();

    @Override
    public void writeObject(ObjectOutput output, SessionCreationMetaDataEntry<Object> entry) throws IOException {
        SessionCreationMetaData metaData = entry.getMetaData();
        INSTANT_EXTERNALIZER.writeObject(output, metaData.getCreationTime());
        IndexExternalizer.VARIABLE.writeData(output, (int) metaData.getMaxInactiveInterval().getSeconds());
    }

    @Override
    public SessionCreationMetaDataEntry<Object> readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        SessionCreationMetaData metaData = new SimpleSessionCreationMetaData(INSTANT_EXTERNALIZER.readObject(input));
        metaData.setMaxInactiveInterval(Duration.ofSeconds(IndexExternalizer.VARIABLE.readData(input)));
        return new SessionCreationMetaDataEntry<>(metaData);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<SessionCreationMetaDataEntry<Object>> getTargetClass() {
        return (Class<SessionCreationMetaDataEntry<Object>>) (Class<?>) SessionCreationMetaDataEntry.class;
    }
}

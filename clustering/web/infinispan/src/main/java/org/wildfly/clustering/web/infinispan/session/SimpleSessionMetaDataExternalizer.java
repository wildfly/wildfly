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
package org.wildfly.clustering.web.infinispan.session;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.web.session.SessionMetaData;

/**
 * Externalizer for session meta data.
 * @author Paul Ferraro
 */
public class SimpleSessionMetaDataExternalizer implements Externalizer<SessionMetaData> {
    private static final TimeUnit SERIALIZED_UNIT = TimeUnit.SECONDS;

    @Override
    public void writeObject(ObjectOutput output, SessionMetaData metaData) throws IOException {
        output.writeLong(metaData.getCreationTime().getTime());
        output.writeLong(metaData.getLastAccessedTime().getTime());
        output.writeInt((int) metaData.getMaxInactiveInterval(SERIALIZED_UNIT));
    }

    @Override
    public SessionMetaData readObject(ObjectInput input) throws IOException {
        Date creationTime = new Date(input.readLong());
        Date lastAccessedTime = new Date(input.readLong());
        Time maxInactiveInterval = new Time(input.readInt(), SERIALIZED_UNIT);
        return new SimpleSessionMetaData(creationTime, lastAccessedTime, maxInactiveInterval);
    }

    @Override
    public Class<? extends SessionMetaData> getTargetClass() {
        return SimpleSessionMetaData.class;
    }
}

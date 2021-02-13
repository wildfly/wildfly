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

package org.wildfly.clustering.web.cache.session;

import java.io.IOException;
import java.time.Duration;

import org.infinispan.protostream.impl.WireFormat;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * @author Paul Ferraro
 */
public class SessionAccessMetaDataMarshaller implements ProtoStreamMarshaller<SimpleSessionAccessMetaData> {

    // Optimize for new sessions
    private static final Duration DEFAULT_SINCE_CREATION = Duration.ZERO;
    // Optimize for sub-second request duration
    private static final Duration DEFAULT_LAST_ACCESS = Duration.ofSeconds(1);

    private static final int SINCE_CREATION_INDEX = 1;
    private static final int LAST_ACCESS_INDEX = 2;

    @Override
    public SimpleSessionAccessMetaData readFrom(ProtoStreamReader reader) throws IOException {
        Duration sinceCreation = DEFAULT_SINCE_CREATION;
        Duration lastAccess = DEFAULT_LAST_ACCESS;
        boolean reading = true;
        while (reading) {
            int tag = reader.readTag();
            switch (WireFormat.getTagFieldNumber(tag)) {
                case SINCE_CREATION_INDEX:
                    sinceCreation = reader.readObject(Duration.class);
                    break;
                case LAST_ACCESS_INDEX:
                    lastAccess = reader.readObject(Duration.class);
                    break;
                default:
                    reading = (tag != 0) && reader.skipField(tag);
            }
        }
        SimpleSessionAccessMetaData metaData = new SimpleSessionAccessMetaData();
        metaData.setLastAccessDuration(sinceCreation, lastAccess);
        return metaData;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, SimpleSessionAccessMetaData metaData) throws IOException {
        Duration sinceCreation = metaData.getSinceCreationDuration();
        if (!sinceCreation.equals(DEFAULT_SINCE_CREATION)) {
            writer.writeObject(SINCE_CREATION_INDEX, sinceCreation);
        }

        Duration lastAccess = metaData.getLastAccessDuration();
        if (!lastAccess.equals(DEFAULT_LAST_ACCESS)) {
            writer.writeObject(LAST_ACCESS_INDEX, lastAccess);
        }
    }

    @Override
    public Class<? extends SimpleSessionAccessMetaData> getJavaClass() {
        return SimpleSessionAccessMetaData.class;
    }
}

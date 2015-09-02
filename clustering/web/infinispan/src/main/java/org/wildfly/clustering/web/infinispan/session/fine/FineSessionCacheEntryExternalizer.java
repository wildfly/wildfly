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
package org.wildfly.clustering.web.infinispan.session.fine;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.web.infinispan.session.SimpleSessionMetaDataExternalizer;
import org.wildfly.clustering.web.session.SessionMetaData;

/**
 * Factory for creating and externalizing fine granularity session cache entries.
 * @author Paul Ferraro
 */
public class FineSessionCacheEntryExternalizer<L> implements Externalizer<FineSessionCacheEntry<L>> {

    private final Externalizer<SessionMetaData> externalizer = new SimpleSessionMetaDataExternalizer();

    @Override
    public void writeObject(ObjectOutput output, FineSessionCacheEntry<L> entry) throws IOException {
        this.externalizer.writeObject(output, entry.getMetaData());
    }

    @Override
    public FineSessionCacheEntry<L> readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        return new FineSessionCacheEntry<>(this.externalizer.readObject(input));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Class<FineSessionCacheEntry<L>> getTargetClass() {
        Class targetClass = FineSessionCacheEntry.class;
        return targetClass;
    }
}

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
import java.util.Set;

import org.jboss.as.clustering.infinispan.io.AbstractSimpleExternalizer;
import org.wildfly.clustering.web.infinispan.session.SimpleSessionMetaData;
import org.wildfly.clustering.web.infinispan.session.SimpleSessionMetaDataExternalizer;

/**
 * Factory for creating and externalizing fine granularity session cache entries.
 * @author Paul Ferraro
 */
public class FineSessionCacheEntryExternalizer extends AbstractSimpleExternalizer<FineSessionCacheEntry<Object>> {
    private static final long serialVersionUID = -7199387053366359377L;

    private final SimpleSessionMetaDataExternalizer externalizer = new SimpleSessionMetaDataExternalizer();

    public FineSessionCacheEntryExternalizer() {
        this(FineSessionCacheEntry.class);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private FineSessionCacheEntryExternalizer(Class targetClass) {
        super(targetClass);
    }

    @Override
    public void writeObject(ObjectOutput output, FineSessionCacheEntry<Object> entry) throws IOException {
        this.externalizer.writeObject(output, (SimpleSessionMetaData) entry.getMetaData());
        Set<String> attributes = entry.getAttributes();
        output.writeInt(attributes.size());
        for (String attribute: attributes) {
            output.writeUTF(attribute);
        }
    }

    @Override
    public FineSessionCacheEntry<Object> readObject(ObjectInput input) throws IOException {
        FineSessionCacheEntry<Object> entry = new FineSessionCacheEntry<>(this.externalizer.readObject(input));
        Set<String> attributes = entry.getAttributes();
        int size = input.readInt();
        for (int i = 0; i < size; ++i) {
            attributes.add(input.readUTF());
        }
        return entry;
    }
}

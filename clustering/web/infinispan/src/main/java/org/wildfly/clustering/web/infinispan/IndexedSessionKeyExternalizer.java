/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
import java.util.function.BiFunction;
import java.util.function.ToIntFunction;

import org.wildfly.clustering.infinispan.spi.distribution.Key;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.IndexSerializer;

/**
 * @author Paul Ferraro
 */
public class IndexedSessionKeyExternalizer<K extends Key<String>> implements Externalizer<K> {

    private final Class<K> targetClass;
    private final BiFunction<String, Integer, K> resolver;
    private final ToIntFunction<K> index;

    protected IndexedSessionKeyExternalizer(Class<K> targetClass, ToIntFunction<K> index, BiFunction<String, Integer, K> resolver) {
        this.targetClass = targetClass;
        this.index = index;
        this.resolver = resolver;
    }

    @Override
    public void writeObject(ObjectOutput output, K key) throws IOException {
        SessionKeyExternalizer.SESSION_ID_SERIALIZER.write(output, key.getValue());
        IndexSerializer.VARIABLE.writeInt(output, this.index.applyAsInt(key));
    }

    @Override
    public K readObject(ObjectInput input) throws IOException {
        String id = SessionKeyExternalizer.SESSION_ID_SERIALIZER.read(input);
        int index = IndexSerializer.VARIABLE.readInt(input);
        return this.resolver.apply(id, index);
    }

    @Override
    public Class<K> getTargetClass() {
        return this.targetClass;
    }
}

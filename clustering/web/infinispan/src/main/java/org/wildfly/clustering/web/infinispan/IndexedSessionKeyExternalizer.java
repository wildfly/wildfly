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
import org.wildfly.clustering.infinispan.spi.persistence.DelimitedKeyFormat;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.IndexExternalizer;

/**
 * @author Paul Ferraro
 */
public class IndexedSessionKeyExternalizer<K extends Key<String>> extends DelimitedKeyFormat<K> implements Externalizer<K> {

    private static final Externalizer<String> EXTERNALIZER = SessionKeyExternalizer.EXTERNALIZER;

    private final BiFunction<String, Integer, K> resolver;
    private final ToIntFunction<K> index;

    protected IndexedSessionKeyExternalizer(Class<K> targetClass, ToIntFunction<K> index, BiFunction<String, Integer, K> resolver) {
        super(targetClass, "#", parts -> resolver.apply(parts[0], Integer.valueOf(parts[1])), key -> new String[] { key.getValue(), Integer.toString(index.applyAsInt(key)) });
        this.index = index;
        this.resolver = resolver;
    }

    @Override
    public void writeObject(ObjectOutput output, K key) throws IOException {
        EXTERNALIZER.writeObject(output, key.getValue());
        IndexExternalizer.VARIABLE.writeData(output, this.index.applyAsInt(key));
    }

    @Override
    public K readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        String id = EXTERNALIZER.readObject(input);
        int index = IndexExternalizer.VARIABLE.readData(input);
        return this.resolver.apply(id, index);
    }
}

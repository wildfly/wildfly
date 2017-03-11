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
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import org.wildfly.clustering.infinispan.spi.distribution.Key;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.web.IdentifierExternalizerProvider;

/**
 * Base externalizer for cache keys containing session identifiers.
 * @author Paul Ferraro
 */
public abstract class SessionKeyExternalizer<K extends Key<String>> implements Externalizer<K> {

    private static final Externalizer<String> EXTERNALIZER = StreamSupport.stream(ServiceLoader.load(IdentifierExternalizerProvider.class, IdentifierExternalizerProvider.class.getClassLoader()).spliterator(), false).findFirst().get().getExternalizer();

    protected interface KeyFactory<K extends Key<String>> {
        K createKey(String id, ObjectInput input) throws IOException, ClassNotFoundException;
    }

    private final Class<K> targetClass;
    private final KeyFactory<K> factory;

    protected SessionKeyExternalizer(Class<K> targetClass, Function<String, K> factory) {
        this(targetClass, (id, input) -> factory.apply(id));
    }

    protected SessionKeyExternalizer(Class<K> targetClass, KeyFactory<K> factory) {
        this.targetClass = targetClass;
        this.factory = factory;
    }

    @Override
    public void writeObject(ObjectOutput output, K key) throws IOException {
        EXTERNALIZER.writeObject(output, key.getValue());
    }

    @Override
    public K readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        return this.factory.createKey(EXTERNALIZER.readObject(input), input);
    }

    @Override
    public Class<K> getTargetClass() {
        return this.targetClass;
    }
}

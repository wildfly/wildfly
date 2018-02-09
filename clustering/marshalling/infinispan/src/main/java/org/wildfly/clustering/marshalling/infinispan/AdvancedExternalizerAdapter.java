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

package org.wildfly.clustering.marshalling.infinispan;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.Set;

import org.infinispan.commons.marshall.AdvancedExternalizer;
import org.wildfly.clustering.marshalling.Externalizer;

/**
 * Adapts an {@link Externalizer} to an Infinispan {@link AdvancedExternalizer}.
 * @author Paul Ferraro
 */
public class AdvancedExternalizerAdapter<T> implements AdvancedExternalizer<T> {
    private static final long serialVersionUID = 6805126239518013697L;

    private final int id;
    private final Externalizer<T> externalizer;

    public AdvancedExternalizerAdapter(int id, Externalizer<T> externalizer) {
        this.id = id;
        this.externalizer = externalizer;
    }

    @Override
    public void writeObject(ObjectOutput output, T object) throws IOException {
        this.externalizer.writeObject(output, object);
    }

    @Override
    public T readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        return this.externalizer.readObject(input);
    }

    @Override
    public Set<Class<? extends T>> getTypeClasses() {
        return Collections.singleton(this.externalizer.getTargetClass());
    }

    @Override
    public Integer getId() {
        return this.id;
    }
}

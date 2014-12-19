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
package org.wildfly.clustering.ejb.infinispan.group;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;

import org.wildfly.clustering.infinispan.spi.io.AbstractSimpleExternalizer;
import org.wildfly.clustering.marshalling.MarshalledValue;
import org.wildfly.clustering.marshalling.MarshallingContext;

/**
 * @author Paul Ferraro
 */
public class InfinispanBeanGroupEntryExternalizer<I, T> extends AbstractSimpleExternalizer<InfinispanBeanGroupEntry<I, T>> {
    private static final long serialVersionUID = 783357750795915336L;

    public InfinispanBeanGroupEntryExternalizer() {
        this(InfinispanBeanGroupEntry.class);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private InfinispanBeanGroupEntryExternalizer(Class targetClass) {
        super(targetClass);
    }

    @Override
    public void writeObject(ObjectOutput output, InfinispanBeanGroupEntry<I, T> entry) throws IOException {
        output.writeObject(entry.getBeans());
    }

    @Override
    public InfinispanBeanGroupEntry<I, T> readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        @SuppressWarnings("unchecked")
        MarshalledValue<Map<I, T>, MarshallingContext> value = (MarshalledValue<Map<I, T>, MarshallingContext>) input.readObject();
        return new InfinispanBeanGroupEntry<>(value);
    }
}

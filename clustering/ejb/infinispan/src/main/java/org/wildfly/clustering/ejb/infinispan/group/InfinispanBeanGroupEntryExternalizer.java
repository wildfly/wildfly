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

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshalledValue;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshalledValueExternalizer;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(Externalizer.class)
public class InfinispanBeanGroupEntryExternalizer<I, T> implements Externalizer<InfinispanBeanGroupEntry<I, T>> {

    private final Externalizer<SimpleMarshalledValue<Map<I, T>>> externalizer = new SimpleMarshalledValueExternalizer<>();

    @Override
    public void writeObject(ObjectOutput output, InfinispanBeanGroupEntry<I, T> entry) throws IOException {
        SimpleMarshalledValue<Map<I, T>> value = (SimpleMarshalledValue<Map<I, T>>) entry.getBeans();
        this.externalizer.writeObject(output, value);
    }

    @Override
    public InfinispanBeanGroupEntry<I, T> readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        return new InfinispanBeanGroupEntry<>(this.externalizer.readObject(input));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<InfinispanBeanGroupEntry<I, T>> getTargetClass() {
        return (Class<InfinispanBeanGroupEntry<I, T>>) (Class<?>) InfinispanBeanGroupEntry.class;
    }
}

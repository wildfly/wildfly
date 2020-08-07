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
import org.wildfly.clustering.marshalling.spi.DefaultExternalizer;
import org.wildfly.clustering.marshalling.spi.MarshalledValue;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(Externalizer.class)
public class InfinispanBeanGroupEntryExternalizer<I, T, C> implements Externalizer<InfinispanBeanGroupEntry<I, T, C>> {

    @Override
    public void writeObject(ObjectOutput output, InfinispanBeanGroupEntry<I, T, C> entry) throws IOException {
        DefaultExternalizer.MARSHALLED_VALUE.cast(MarshalledValue.class).writeObject(output, entry.getBeans());
    }

    @Override
    public InfinispanBeanGroupEntry<I, T, C> readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        MarshalledValue<Map<I, T>, C> value = DefaultExternalizer.MARSHALLED_VALUE.cast(MarshalledValue.class).readObject(input);
        return new InfinispanBeanGroupEntry<>(value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<InfinispanBeanGroupEntry<I, T, C>> getTargetClass() {
        return (Class<InfinispanBeanGroupEntry<I, T, C>>) (Class<?>) InfinispanBeanGroupEntry.class;
    }
}

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

package org.wildfly.clustering.infinispan.spi.metadata;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.infinispan.container.versioning.EntryVersion;
import org.infinispan.container.versioning.NumericVersion;
import org.infinispan.container.versioning.SimpleClusteredVersion;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.Externalizer;

/**
 * @author Paul Ferraro
 */
public interface EntryVersionExternalizer<V extends EntryVersion> extends Externalizer<V> {

    @MetaInfServices(Externalizer.class)
    public static class NumericVersionExternalizer implements EntryVersionExternalizer<NumericVersion> {

        @Override
        public void writeObject(ObjectOutput output, NumericVersion version) throws IOException {
            output.writeLong(version.getVersion());
        }

        @Override
        public NumericVersion readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            return new NumericVersion(input.readLong());
        }

        @Override
        public Class<NumericVersion> getTargetClass() {
            return NumericVersion.class;
        }
    }

    @MetaInfServices(Externalizer.class)
    public class SimpleClusteredVersionExternalizer implements EntryVersionExternalizer<SimpleClusteredVersion> {

        @Override
        public void writeObject(ObjectOutput output, SimpleClusteredVersion version) throws IOException {
            output.writeInt(version.getTopologyId());
            output.writeLong(version.getVersion());
        }

        @Override
        public SimpleClusteredVersion readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            return new SimpleClusteredVersion(input.readInt(), input.readLong());
        }

        @Override
        public Class<SimpleClusteredVersion> getTargetClass() {
            return SimpleClusteredVersion.class;
        }
    }
}

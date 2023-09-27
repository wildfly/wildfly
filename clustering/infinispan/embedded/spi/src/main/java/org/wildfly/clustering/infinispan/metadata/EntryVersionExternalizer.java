/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.metadata;

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

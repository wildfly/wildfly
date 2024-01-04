/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.metadata;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.infinispan.container.versioning.EntryVersion;
import org.infinispan.metadata.EmbeddedMetadata;
import org.infinispan.metadata.Metadata;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.Externalizer;

/**
 * @author Paul Ferraro
 */
public class MetadataExternalizer<M extends Metadata> implements Externalizer<M> {

    private final Class<M> metadataClass;
    private final boolean hasLifespan;
    private final boolean hasMaxIdle;

    MetadataExternalizer(Class<M> metadataClass, boolean hasLifespan, boolean hasMaxIdle) {
        this.metadataClass = metadataClass;
        this.hasLifespan = hasLifespan;
        this.hasMaxIdle = hasMaxIdle;
    }

    @Override
    public void writeObject(ObjectOutput output, M metadata) throws IOException {
        output.writeObject(metadata.version());
        if (this.hasLifespan) {
            output.writeLong(metadata.lifespan());
        }
        if (this.hasMaxIdle) {
            output.writeLong(metadata.maxIdle());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public M readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        EmbeddedMetadata.Builder builder = new EmbeddedMetadata.Builder();
        builder.version((EntryVersion) input.readObject());
        if (this.hasLifespan) {
            builder.lifespan(input.readLong());
        }
        if (this.hasMaxIdle) {
            builder.maxIdle(input.readLong());
        }
        return (M) builder.build();
    }

    @Override
    public Class<M> getTargetClass() {
        return this.metadataClass;
    }

    @MetaInfServices(Externalizer.class)
    public static class EmbeddedMetadataExternalizer extends MetadataExternalizer<EmbeddedMetadata> {
        public EmbeddedMetadataExternalizer() {
            super(EmbeddedMetadata.class, false, false);
        }
    }

    @MetaInfServices(Externalizer.class)
    public static class EmbeddedExpirableMetadataExternalizer extends MetadataExternalizer<EmbeddedMetadata.EmbeddedExpirableMetadata> {
        public EmbeddedExpirableMetadataExternalizer() {
            super(EmbeddedMetadata.EmbeddedExpirableMetadata.class, true, true);
        }
    }

    @MetaInfServices(Externalizer.class)
    public static class EmbeddedLifespanExpirableMetadataExternalizer extends MetadataExternalizer<EmbeddedMetadata.EmbeddedLifespanExpirableMetadata> {
        public EmbeddedLifespanExpirableMetadataExternalizer() {
            super(EmbeddedMetadata.EmbeddedLifespanExpirableMetadata.class, true, false);
        }
    }

    @MetaInfServices(Externalizer.class)
    public static class EmbeddedMaxIdleExpirableMetadataExternalizer extends MetadataExternalizer<EmbeddedMetadata.EmbeddedMaxIdleExpirableMetadata> {
        public EmbeddedMaxIdleExpirableMetadataExternalizer() {
            super(EmbeddedMetadata.EmbeddedMaxIdleExpirableMetadata.class, false, true);
        }
    }
}

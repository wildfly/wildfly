/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;

import org.infinispan.protostream.descriptors.WireType;

/**
 * @author Paul Ferraro
 */
public class SimpleClassLoaderMarshaller implements ClassLoaderMarshaller {

    private final ClassLoader loader;

    public SimpleClassLoaderMarshaller(ClassLoader loader) {
        this.loader = loader;
    }

    @Override
    public ClassLoader createInitialValue() {
        return this.loader;
    }

    @Override
    public int getFields() {
        return 0;
    }

    @Override
    public ClassLoader readFrom(ProtoStreamReader reader, int index, WireType type, ClassLoader loader) throws IOException {
        return loader;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, ClassLoader value) throws IOException {
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;

/**
 * @author Paul Ferraro
 */
public class SimpleClassLoaderMarshaller implements ClassLoaderMarshaller {

    private final ClassLoader loader;

    public SimpleClassLoaderMarshaller(ClassLoader loader) {
        this.loader = loader;
    }

    @Override
    public ClassLoader getBuilder() {
        return this.loader;
    }

    @Override
    public int getFields() {
        return 0;
    }

    @Override
    public ClassLoader readField(ProtoStreamReader reader, int index, ClassLoader loader) throws IOException {
        return loader;
    }

    @Override
    public void writeFields(ProtoStreamWriter writer, int startIndex, ClassLoader value) throws IOException {
    }
}

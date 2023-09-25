/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.weld.annotated.slim;

import java.io.IOException;

import org.infinispan.protostream.descriptors.WireType;
import org.jboss.weld.Container;
import org.jboss.weld.annotated.slim.AnnotatedTypeIdentifier;
import org.jboss.weld.annotated.slim.SlimAnnotatedType;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.resources.ClassTransformer;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Generic marshaller for a {@link SlimAnnotatedType}.
 * @author Paul Ferraro
 */
public class AnnotatedTypeMarshaller<X, T extends SlimAnnotatedType<X>> implements ProtoStreamMarshaller<T> {

    private static final int IDENTIFIER_INDEX = 1;

    private final Class<T> targetClass;

    public AnnotatedTypeMarshaller(Class<T> targetClass) {
        this.targetClass = targetClass;
    }

    @Override
    public Class<? extends T> getJavaClass() {
        return this.targetClass;
    }

    @Override
    public T readFrom(ProtoStreamReader reader) throws IOException {
        AnnotatedTypeIdentifier identifier = null;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case IDENTIFIER_INDEX:
                    identifier = reader.readObject(AnnotatedTypeIdentifier.class);
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        BeanManagerImpl manager = Container.instance(identifier).getBeanManager(identifier.getBdaId());
        return this.getAnnotatedType(identifier, manager);
    }

    @SuppressWarnings("unchecked")
    protected T getAnnotatedType(AnnotatedTypeIdentifier identifier, BeanManagerImpl manager) {
        return (T) ClassTransformer.instance(manager).getSlimAnnotatedTypeById(identifier);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, T type) throws IOException {
        AnnotatedTypeIdentifier identifier = type.getIdentifier();
        if (identifier != null) {
            writer.writeObject(IDENTIFIER_INDEX, identifier);
        }
    }
}

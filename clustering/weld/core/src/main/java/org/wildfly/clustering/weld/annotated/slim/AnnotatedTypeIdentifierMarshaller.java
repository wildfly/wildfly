/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.annotated.slim;

import java.io.IOException;

import org.infinispan.protostream.descriptors.WireType;
import org.jboss.weld.Container;
import org.jboss.weld.annotated.slim.AnnotatedTypeIdentifier;
import org.jboss.weld.manager.BeanManagerImpl;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Marshaller for an {@link AnnotatedTypeIdentifier}.
 * @author Paul Ferraro
 */
public class AnnotatedTypeIdentifierMarshaller implements ProtoStreamMarshaller<AnnotatedTypeIdentifier> {

    private static final int BEAN_MANAGER_INDEX = 1;
    private static final int CLASS_NAME_INDEX = 2;
    private static final int MODIFIED_CLASS_NAME_INDEX = 3;
    private static final int SUFFIX_INDEX = 4;

    @Override
    public Class<? extends AnnotatedTypeIdentifier> getJavaClass() {
        return AnnotatedTypeIdentifier.class;
    }

    @Override
    public AnnotatedTypeIdentifier readFrom(ProtoStreamReader reader) throws IOException {
        BeanManagerImpl manager = null;
        String className = null;
        boolean modified = false;
        String suffix = null;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case BEAN_MANAGER_INDEX:
                    manager = reader.readAny(BeanManagerImpl.class);
                    break;
                case MODIFIED_CLASS_NAME_INDEX:
                    modified = true;
                case CLASS_NAME_INDEX:
                    className = reader.readAny(String.class);
                    break;
                case SUFFIX_INDEX:
                    suffix = reader.readAny(String.class);
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return AnnotatedTypeIdentifier.of(manager.getContextId(), manager.getId(), className, suffix, modified);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, AnnotatedTypeIdentifier identifier) throws IOException {
        BeanManagerImpl manager = Container.instance(identifier.getContextId()).getBeanManager(identifier.getBdaId());
        if (manager != null) {
            writer.writeAny(BEAN_MANAGER_INDEX, manager);
        }
        writer.writeAny(identifier.isModified() ? MODIFIED_CLASS_NAME_INDEX : CLASS_NAME_INDEX, identifier.getClassName());
        String suffix = identifier.getSuffix();
        if (suffix != null) {
            writer.writeAny(SUFFIX_INDEX, suffix);
        }
    }
}

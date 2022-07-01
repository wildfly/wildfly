/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.weld.contexts;

import java.io.IOException;
import java.lang.reflect.Field;
import java.security.PrivilegedAction;
import java.util.LinkedList;
import java.util.List;

import org.infinispan.protostream.descriptors.WireType;
import org.jboss.weld.context.api.ContextualInstance;
import org.jboss.weld.contexts.CreationalContextImpl;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author Paul Ferraro
 */
public class CreationalContextImplMarshaller<T> implements ProtoStreamMarshaller<CreationalContextImpl<T>> {

    private static final int PARENT_INDEX = 1;
    private static final int DEPENDENT_INDEX = 2;

    static final Field PARENT_FIELD = WildFlySecurityManager.doUnchecked(new PrivilegedAction<Field>() {
        @Override
        public Field run() {
            for (Field field : CreationalContextImpl.class.getDeclaredFields()) {
                if (field.getType() == CreationalContextImpl.class) {
                    field.setAccessible(true);
                    return field;
                }
            }
            throw new IllegalStateException();
        }
    });

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends CreationalContextImpl<T>> getJavaClass() {
        return (Class<CreationalContextImpl<T>>) (Class<?>) CreationalContextImpl.class;
    }

    @Override
    public CreationalContextImpl<T> readFrom(ProtoStreamReader reader) throws IOException {
        CreationalContextImpl<T> result = new CreationalContextImpl<>(null);
        reader.getContext().addReference(result);
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case PARENT_INDEX:
                    CreationalContextImpl<?> parent = reader.readAny(CreationalContextImpl.class);
                    WildFlySecurityManager.doUnchecked(new PrivilegedAction<Void>() {
                        @Override
                        public Void run() {
                            try {
                                PARENT_FIELD.set(result, parent);
                                return null;
                            } catch (IllegalAccessException e) {
                                throw new IllegalStateException(e);
                            }
                        }
                    });
                    for (ContextualInstance<?> dependent : parent.getDependentInstances()) {
                        result.addDependentInstance(dependent);
                    }
                    break;
                case DEPENDENT_INDEX:
                    ContextualInstance<?> dependent = reader.readAny(ContextualInstance.class);
                    result.getCreationalContext(null).addDependentInstance(dependent);
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void writeTo(ProtoStreamWriter writer, CreationalContextImpl<T> context) throws IOException {
        writer.getContext().addReference(context);
        CreationalContextImpl<?> parent = context.getParentCreationalContext();
        if (parent != null) {
            writer.writeAny(PARENT_INDEX, parent);
        }
        // https://issues.jboss.org/browse/WELD-1076
        // Mimics CreationalContextImpl.writeReplace(...)
        List<Object> unmarshallableDependents = new LinkedList<>();
        for (ContextualInstance<?> dependent : context.getDependentInstances()) {
            Object dependentInstance = dependent.getInstance();
            if (writer.getSerializationContext().canMarshall(dependentInstance)) {
                writer.writeAny(DEPENDENT_INDEX, dependent);
            } else {
                unmarshallableDependents.add(dependentInstance);
            }
        }
        // Destroy unmarshallable dependents outside of loop - otherwise it will throw a ConcurrentModificationException
        for (Object dependentInstance : unmarshallableDependents) {
            context.destroyDependentInstance((T) dependentInstance);
        }
    }
}

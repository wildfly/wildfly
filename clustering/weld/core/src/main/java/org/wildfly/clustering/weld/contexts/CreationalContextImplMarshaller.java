/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.contexts;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;

import org.infinispan.protostream.descriptors.WireType;
import org.jboss.weld.context.api.ContextualInstance;
import org.jboss.weld.contexts.CreationalContextImpl;
import org.wildfly.clustering.function.BiFunction;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * @author Paul Ferraro
 */
public class CreationalContextImplMarshaller<T> implements ProtoStreamMarshaller<CreationalContextImpl<T>> {

    private static final int PARENT_INDEX = 1;
    private static final int DEPENDENT_INDEX = 2;

    private static final BiFunction<CreationalContextImpl<?>, CreationalContextImpl<?>, Void> PARENT_HANDLE = BiFunction.invoke(findHandle(CreationalContextImpl.class, CreationalContextImpl.class));

    static MethodHandle findHandle(Class<?> sourceClass, Class<?> fieldClass) {
        for (Field field : sourceClass.getDeclaredFields()) {
            if (field.getType() == fieldClass) {
                try {
                    // Necessary, since This field is final
                    field.setAccessible(true);
                    return MethodHandles.privateLookupIn(sourceClass, MethodHandles.lookup()).unreflectSetter(field);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        throw new IllegalArgumentException(fieldClass.getName());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends CreationalContextImpl<T>> getJavaClass() {
        return (Class<CreationalContextImpl<T>>) (Class<?>) CreationalContextImpl.class;
    }

    @Override
    public CreationalContextImpl<T> readFrom(ProtoStreamReader reader) throws IOException {
        CreationalContextImpl<T> result = new CreationalContextImpl<>(null);
        reader.getContext().record(result);
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case PARENT_INDEX:
                    CreationalContextImpl<?> parent = reader.readAny(CreationalContextImpl.class);
                    PARENT_HANDLE.apply(result, parent);
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
        writer.getContext().record(context);
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

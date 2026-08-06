/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.faces.component;

import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;

import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.StateHolder;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.function.BiFunction;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.reflect.BinaryFieldMarshaller;

@MetaInfServices(SerializationContextInitializer.class)
public class ComponentSerializationContextInitializer extends AbstractSerializationContextInitializer {

    public ComponentSerializationContextInitializer() {
        super(FacesComponent.class.getPackage());
    }

    @Override
    public void registerMarshallers(SerializationContext context) {
        // Package protected enums!!!
        registerMarshaller(context, UIComponent.class, "PropertyKeys");
        registerMarshaller(context, UIComponent.class, "PropertyKeysPrivate");
        // Package protected class with inaccessible fields!!!
        Map.Entry<Class<Object>, BiFunction<String, Serializable, Object>> entry = findClass("jakarta.faces.component.StateHolderSaver", FacesContext.class, Object.class);
        context.registerMarshaller(new BinaryFieldMarshaller<>(entry.getKey(), String.class, Serializable.class, entry.getValue()));
    }

    private static <E extends Enum<E>> void registerMarshaller(SerializationContext context, Class<?> parentClass, String enumName) {
        Class<E> enumClass = findEnumClass(parentClass, enumName);
        context.registerMarshaller(ProtoStreamMarshaller.of(enumClass));
    }

    @SuppressWarnings("unchecked")
    private static <E extends Enum<E>> Class<E> findEnumClass(Class<?> parentClass, String enumName) {
        try {
            return (Class<E>) parentClass.getClassLoader().loadClass(parentClass.getName() + "$" + enumName).asSubclass(Enum.class);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private static <V1, V2> Map.Entry<Class<Object>, BiFunction<V1, V2, Object>> findClass(String className, Class<?>... constructorParameterTypes) {
        try {
            @SuppressWarnings("unchecked")
            Class<Object> targetClass = (Class<Object>) StateHolder.class.getClassLoader().loadClass(className);
            MethodHandle constructorHandle = MethodHandles.privateLookupIn(targetClass, MethodHandles.lookup()).findConstructor(targetClass, MethodType.methodType(void.class, constructorParameterTypes));
            return Map.entry(targetClass, BiFunction.invoke(constructorHandle));
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}

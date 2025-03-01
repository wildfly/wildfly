/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.faces.component;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.function.Supplier;

import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.StateHolder;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.reflect.FieldMarshaller;
import org.wildfly.security.manager.WildFlySecurityManager;

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
        Map.Entry<Class<? extends Object>, Supplier<Object>> entry = findClass("jakarta.faces.component.StateHolderSaver", FacesContext.class, Object.class);
        context.registerMarshaller(new FieldMarshaller<>(entry.getKey(), entry.getValue(), String.class, Serializable.class));
    }

    private static <E extends Enum<E>> void registerMarshaller(SerializationContext context, Class<?> parentClass, String enumName) {
        Class<E> enumClass = findEnumClass(parentClass, enumName);
        context.registerMarshaller(ProtoStreamMarshaller.of(enumClass));
    }

    @SuppressWarnings("unchecked")
    private static <E extends Enum<E>> Class<E> findEnumClass(Class<?> parentClass, String enumName) {
        try {
            return (Class<E>) WildFlySecurityManager.getClassLoaderPrivileged(parentClass).loadClass(parentClass.getName() + "$" + enumName).asSubclass(Enum.class);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Map.Entry<Class<? extends Object>, Supplier<Object>> findClass(String className, Class<?>... constructorParameterTypes) {
        try {
            Class<? extends Object> targetClass = WildFlySecurityManager.getClassLoaderPrivileged(StateHolder.class).loadClass(className);
            PrivilegedAction<Object> action = () -> {
                try {
                    Constructor<? extends Object> constructor = targetClass.getDeclaredConstructor(constructorParameterTypes);
                    constructor.setAccessible(true);
                    return constructor.newInstance(null, null);
                } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
                    throw new IllegalStateException(e);
                }
            };
            return Map.entry(targetClass, () -> WildFlySecurityManager.doUnchecked(action));
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
}

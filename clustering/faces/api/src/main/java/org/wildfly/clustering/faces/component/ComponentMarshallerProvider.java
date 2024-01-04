/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.faces.component;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.PrivilegedAction;

import jakarta.faces.component.StateHolder;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;

import org.wildfly.clustering.marshalling.protostream.EnumMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.reflect.FieldMarshaller;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author Paul Ferraro
 */
public enum ComponentMarshallerProvider implements ProtoStreamMarshallerProvider {
    PROPERTY_KEYS(UIComponent.class, "PropertyKeys"),
    PROPERTY_KEYS_PRIVATE(UIComponent.class, "PropertyKeysPrivate"),
    STATE_HOLDER_SAVER("jakarta.faces.component.StateHolderSaver"),
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    ComponentMarshallerProvider(Class<?> parentClass, String enumName) {
        // Package protected enums!!!
        try {
            this.marshaller = new EnumMarshaller<>(WildFlySecurityManager.getClassLoaderPrivileged(parentClass).loadClass(parentClass.getName() + "$" + enumName).asSubclass(Enum.class));
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    ComponentMarshallerProvider(String className) {
        // Package protected class with inaccessible fields!!!
        try {
            Class<? extends Object> targetClass = WildFlySecurityManager.getClassLoaderPrivileged(StateHolder.class).loadClass(className);
            PrivilegedAction<Object> action = () -> {
                try {
                    Constructor<? extends Object> constructor = targetClass.getDeclaredConstructor(FacesContext.class, Object.class);
                    constructor.setAccessible(true);
                    return constructor.newInstance(null, null);
                } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
                    throw new IllegalStateException(e);
                }
            };
            this.marshaller = new FieldMarshaller<>(targetClass, () -> WildFlySecurityManager.doUnchecked(action), String.class, Serializable.class);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    ComponentMarshallerProvider(ProtoStreamMarshaller<?> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}

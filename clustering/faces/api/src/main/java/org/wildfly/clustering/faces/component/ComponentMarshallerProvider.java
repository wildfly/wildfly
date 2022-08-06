/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

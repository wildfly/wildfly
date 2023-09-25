/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.marshalling.spi.io;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.InvocationTargetException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Externalizer for Externalizable objects.
 * @author Paul Ferraro
 */
public class ExternalizableExternalizer<T extends Externalizable> implements Externalizer<T> {

    private final Class<T> targetClass;

    public ExternalizableExternalizer(Class<T> targetClass) {
        this.targetClass = targetClass;
    }

    @Override
    public void writeObject(ObjectOutput output, T object) throws IOException {
        object.writeExternal(output);
    }

    @Override
    public T readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        PrivilegedExceptionAction<T> action = new PrivilegedExceptionAction<>() {
            @Override
            public T run() throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
                return ExternalizableExternalizer.this.getTargetClass().getConstructor().newInstance();
            }
        };
        try {
            T object = WildFlySecurityManager.doChecked(action);
            object.readExternal(input);
            return object;
        } catch (PrivilegedActionException e) {
            throw new IOException(e.getCause());
        }
    }

    @Override
    public Class<T> getTargetClass() {
        return this.targetClass;
    }
}

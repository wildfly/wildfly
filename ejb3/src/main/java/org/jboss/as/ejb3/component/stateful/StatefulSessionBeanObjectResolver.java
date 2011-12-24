/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.component.stateful;

import java.io.IOException;
import java.io.InvalidClassException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.jboss.marshalling.ObjectResolver;
import org.jboss.marshalling.reflect.SerializableClass;
import org.jboss.marshalling.reflect.SerializableClassRegistry;
import org.jboss.marshalling.reflect.SerializableField;

/**
 * @author Paul Ferraro
 *
 */
public class StatefulSessionBeanObjectResolver implements ObjectResolver {

    private static final SerializableClassRegistry registry = new SerializableClassRegistryAction().getInstance();

    /**
     * {@inheritDoc}
     * @see org.jboss.marshalling.ObjectResolver#readResolve(java.lang.Object)
     */
    @Override
    public Object readResolve(Object replacement) {
        return (replacement != null) && (replacement instanceof SerializableAdapter) ? ((SerializableAdapter) replacement).object : replacement;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.marshalling.ObjectResolver#writeReplace(java.lang.Object)
     */
    @Override
    public Object writeReplace(Object original) {
        return (original != null) && !(original instanceof Serializable) ? new SerializableAdapter(original) : original;
    }

    public static class SerializableAdapter implements Serializable {
        private static final long serialVersionUID = -6899336404581473380L;
        private transient Object object;

        SerializableAdapter(Object object) {
            this.object = object;
        }

        private void readObject(java.io.ObjectInputStream input) throws IOException, ClassNotFoundException {
            Class<?> targetClass = (Class<?>) input.readObject();
            try {
                this.object = targetClass.newInstance();
                while (targetClass != null) {
                    SerializableClass clazz = registry.lookup(targetClass);
                    for (SerializableField field: clazz.getFields()) {
                        Field accessibleField = field.getField();
                        switch (field.getKind()) {
                            case BOOLEAN: {
                                accessibleField.setBoolean(this.object, input.readBoolean());
                                break;
                            }
                            case BYTE: {
                                accessibleField.setByte(this.object, input.readByte());
                                break;
                            }
                            case CHAR: {
                                accessibleField.setChar(this.object, input.readChar());
                                break;
                            }
                            case DOUBLE: {
                                accessibleField.setDouble(this.object, input.readDouble());
                                break;
                            }
                            case FLOAT: {
                                accessibleField.setFloat(this.object, input.readFloat());
                                break;
                            }
                            case INT: {
                                accessibleField.setInt(this.object, input.readInt());
                                break;
                            }
                            case LONG: {
                                accessibleField.setLong(this.object, input.readLong());
                                break;
                            }
                            case SHORT: {
                                accessibleField.setShort(this.object, input.readShort());
                                break;
                            }
                            case OBJECT: {
                                accessibleField.set(this.object, input.readObject());
                                break;
                            }
                        }
                    }
                    targetClass = targetClass.getSuperclass();
                }
            } catch (InstantiationException e) {
                // This shouldn't happen - since the serialization would have failed
                throw new IllegalStateException(e);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        private void writeObject(java.io.ObjectOutputStream output) throws IOException {
            Class<?> targetClass = this.object.getClass();
            // Make sure that Class.newInstance() will succeed during readObject(...)
            if (new HasAccessibleNoArgConstructorAction(targetClass).check()) {
                throw new InvalidClassException(targetClass.getName());
            }
            output.writeObject(targetClass);
            while (targetClass != null) {
                SerializableClass clazz = registry.lookup(targetClass);
                for (SerializableField field: clazz.getFields()) {
                    Field accessibleField = field.getField();
                    try {
                        switch (field.getKind()) {
                            case BOOLEAN: {
                                output.writeBoolean(accessibleField.getBoolean(this.object));
                                break;
                            }
                            case BYTE: {
                                output.writeByte(accessibleField.getByte(this.object));
                                break;
                            }
                            case CHAR: {
                                output.writeChar(accessibleField.getChar(this.object));
                                break;
                            }
                            case DOUBLE: {
                                output.writeDouble(accessibleField.getDouble(this.object));
                                break;
                            }
                            case FLOAT: {
                                output.writeFloat(accessibleField.getFloat(this.object));
                                break;
                            }
                            case INT: {
                                output.writeInt(accessibleField.getInt(this.object));
                                break;
                            }
                            case LONG: {
                                output.writeLong(accessibleField.getLong(this.object));
                                break;
                            }
                            case SHORT: {
                                output.writeShort(accessibleField.getShort(this.object));
                                break;
                            }
                            case OBJECT: {
                                output.writeObject(accessibleField.get(this.object));
                                break;
                            }
                        }
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                }
                targetClass = targetClass.getSuperclass();
            }
        }
    }

    static class SerializableClassRegistryAction implements PrivilegedAction<SerializableClassRegistry> {
        @Override
        public SerializableClassRegistry run() {
            return SerializableClassRegistry.getInstance();
        }

        SerializableClassRegistry getInstance() {
            return AccessController.doPrivileged(this);
        }
    };

    static class HasAccessibleNoArgConstructorAction implements PrivilegedAction<Boolean> {
        private final Class<?> targetClass;

        HasAccessibleNoArgConstructorAction(Class<?> targetClass) {
            this.targetClass = targetClass;
        }

        @Override
        public Boolean run() {
            try {
                return this.targetClass.getDeclaredConstructor().isAccessible();
            } catch (NoSuchMethodException e) {
                // Test for compiler-generated default constructor
                return this.targetClass.getDeclaredConstructors().length == 0;
            }
        }

        Boolean check() {
            return AccessController.doPrivileged(this);
        }
    };
}

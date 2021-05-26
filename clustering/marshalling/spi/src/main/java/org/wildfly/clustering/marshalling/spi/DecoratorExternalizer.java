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

package org.wildfly.clustering.marshalling.spi;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Field;
import java.security.PrivilegedAction;
import java.util.function.UnaryOperator;

import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.security.ParametricPrivilegedAction;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Marshaller for a decorator that does not provide public access to its decorated object.
 * @author Paul Ferraro
 */
public class DecoratorExternalizer<T> implements Externalizer<T>, ParametricPrivilegedAction<T, T> {

    private final Class<T> decoratorClass;
    private final UnaryOperator<T> decorator;
    private final Field field;

    /**
     * Constructs a decorator externalizer.
     * @param decoratedClass the generalized type of the decorated object
     * @param decorator the decoration function
     * @param sample a sample object used to determine the type of the decorated object
     */
    @SuppressWarnings("unchecked")
    public DecoratorExternalizer(Class<T> decoratedClass, UnaryOperator<T> decorator, T sample) {
        this.decorator = decorator;
        Class<?> decoratorClass = decorator.apply(sample).getClass();
        this.decoratorClass = (Class<T>) decoratorClass;
        this.field = WildFlySecurityManager.doUnchecked(new PrivilegedAction<Field>() {
            @Override
            public Field run() {
                Field field = findDecoratedField(decoratorClass, decoratedClass);
                field.setAccessible(true);
                return field;
            }
        });
    }

    static Field findDecoratedField(Class<?> decoratorClass, Class<?> decoratedClass) {
        for (Field field : decoratorClass.getDeclaredFields()) {
            if (field.getType().isAssignableFrom(decoratedClass)) {
                return field;
            }
        }
        Class<?> superClass = decoratorClass.getSuperclass();
        if (superClass == null) {
            throw new IllegalStateException();
        }
        return findDecoratedField(superClass, decoratedClass);
    }

    @Override
    public Class<T> getTargetClass() {
        return this.decoratorClass;
    }

    @Override
    public void writeObject(ObjectOutput output, T value) throws IOException {
        T decorated = WildFlySecurityManager.doUnchecked(value, this);
        output.writeObject(decorated);
    }

    @Override
    public T readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        @SuppressWarnings("unchecked")
        T decorated = (T) input.readObject();
        return this.decorator.apply(decorated);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T run(T value) {
        try {
            return (T) this.field.get(value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}

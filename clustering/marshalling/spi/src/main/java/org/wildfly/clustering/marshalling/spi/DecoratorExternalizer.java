/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.PrivilegedAction;
import java.util.LinkedList;
import java.util.List;
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
        List<Field> assignableFields = new LinkedList<>();
        for (Field field : decoratorClass.getDeclaredFields()) {
            Class<?> type = field.getType();
            if (!Modifier.isStatic(field.getModifiers()) && (type != Object.class) && type.isAssignableFrom(decoratedClass)) {
                assignableFields.add(field);
            }
        }
        // We should not have matched more than 1 field
        if (assignableFields.size() > 1) {
            throw new IllegalStateException(assignableFields.toString());
        }
        if (!assignableFields.isEmpty()) {
            return assignableFields.get(0);
        }
        Class<?> superClass = decoratorClass.getSuperclass();
        if (superClass == null) {
            throw new IllegalStateException(decoratorClass.getName());
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

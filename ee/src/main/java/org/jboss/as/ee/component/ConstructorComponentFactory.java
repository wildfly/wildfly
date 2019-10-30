package org.jboss.as.ee.component;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.jboss.as.naming.ManagedReference;
import org.jboss.invocation.InterceptorContext;

/**
 * @author Stuart Douglas
 */
public class ConstructorComponentFactory implements ComponentFactory {

    private final Constructor<?> constructor;

    public ConstructorComponentFactory(final Constructor<?> constructor) {
        this.constructor = constructor;
    }

    @Override
    public ManagedReference create(final InterceptorContext context) {
        try {
            Object instance = constructor.newInstance();
            return new ConstructorManagedReference(instance);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static final class ConstructorManagedReference implements ManagedReference, Serializable {

        private final Object value;

        private ConstructorManagedReference(final Object value) {
            this.value = value;
        }

        @Override
        public void release() {

        }

        @Override
        public Object getInstance() {
            return value;
        }
    }
}

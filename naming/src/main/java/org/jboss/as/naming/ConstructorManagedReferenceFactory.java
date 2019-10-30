package org.jboss.as.naming;

import java.lang.reflect.Constructor;

/**
 * Managed reference that creates an instance from the constructor.
 *
 * @author Stuart Douglas
 */
public class ConstructorManagedReferenceFactory implements ManagedReferenceFactory {

    private final Constructor<?> constructor;

    public ConstructorManagedReferenceFactory(Constructor<?> constructor) {
        this.constructor = constructor;
    }

    @Override
    public ManagedReference getReference() {
        try {
            return new ImmediateManagedReference(constructor.newInstance());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

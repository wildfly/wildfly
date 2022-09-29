package org.jboss.as.ee.component;

import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;

/**
 *
 *
 * @author Stuart Douglas
 */
public interface EEClassIntrospector {

    ManagedReferenceFactory createFactory(final Class<?> clazz);

    /**
     * Returns the managed reference of an new instance.
     * @param instance an object instance
     * @return a managed reference
     */
    ManagedReference createInstance(Object instance);

    /**
     * Returns the managed reference of an existing instance.
     * @param instance an object instance
     * @return a managed reference
     */
    ManagedReference getInstance(Object instance);
}

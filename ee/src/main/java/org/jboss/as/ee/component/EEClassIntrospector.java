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

    ManagedReference createInstance(Object instance);
}

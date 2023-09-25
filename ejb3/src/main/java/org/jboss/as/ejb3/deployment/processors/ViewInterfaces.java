/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.deployment.processors;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import java.io.Externalizable;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
class ViewInterfaces {
    /**
     * Returns all interfaces implemented by a bean that are eligible to be view interfaces
     *
     * @param beanClass The bean class
     * @return A collection of all potential view interfaces
     */
    static Set<Class<?>> getPotentialViewInterfaces(Class<?> beanClass) {
        Class<?>[] interfaces = beanClass.getInterfaces();
        if (interfaces.length == 0) {
            return Collections.emptySet();
        }
        final Set<Class<?>> potentialBusinessInterfaces = new HashSet<>();
        for (Class<?> klass : interfaces) {
            // Enterprise Beans 3.1 FR 4.9.7 bullet 5.3
            if (klass.equals(Serializable.class) ||
                    klass.equals(Externalizable.class) ||
                    klass.getName().startsWith("jakarta.ejb.") ||
                    klass.getName().startsWith("groovy.lang.")) {
                continue;
            }
            potentialBusinessInterfaces.add(klass);
        }
        return potentialBusinessInterfaces;
    }

    /**
     * Returns all interfaces implemented by a bean that are eligible to be view interfaces
     *
     * @param beanClass The bean class
     * @return A collection of all potential view interfaces
     */
    static Set<DotName> getPotentialViewInterfaces(ClassInfo beanClass) {
        List<DotName> interfaces = beanClass.interfaceNames();
        if (interfaces.isEmpty()) {
            return Collections.emptySet();
        }
        final Set<DotName> names = new HashSet<>();
        for (DotName dotName : interfaces) {
            String name = dotName.toString();
            // Enterprise Beans 3.1 FR 4.9.7 bullet 5.3
            // & FR 5.4.2
            if (name.equals(Serializable.class.getName()) ||
                    name.equals(Externalizable.class.getName()) ||
                    name.startsWith("jakarta.ejb.") ||
                    name.startsWith("groovy.lang.")) {
                continue;
            }
            names.add(dotName);
        }
        return names;
    }
}

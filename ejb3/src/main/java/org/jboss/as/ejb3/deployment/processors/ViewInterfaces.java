/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.deployment.processors;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import java.io.Externalizable;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
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
        if (interfaces == null) {
            return Collections.emptySet();
        }
        final Set<Class<?>> potentialBusinessInterfaces = new HashSet<Class<?>>();
        for (Class<?> klass : interfaces) {
            // EJB 3.1 FR 4.9.7 bullet 5.3
            if (klass.equals(Serializable.class) ||
                    klass.equals(Externalizable.class) ||
                    klass.getName().startsWith("javax.ejb.") ||
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
        DotName[] interfaces = beanClass.interfaces();
        if (interfaces == null) {
            return Collections.emptySet();
        }
        final Set<DotName> names = new HashSet<DotName>();
        for (DotName dotName : interfaces) {
            String name = dotName.toString();
            // EJB 3.1 FR 4.9.7 bullet 5.3
            // & FR 5.4.2
            if (name.equals(Serializable.class.getName()) ||
                    name.equals(Externalizable.class.getName()) ||
                    name.startsWith("javax.ejb.") ||
                    name.startsWith("groovy.lang.")) {
                continue;
            }
            names.add(dotName);
        }
        return names;
    }
}

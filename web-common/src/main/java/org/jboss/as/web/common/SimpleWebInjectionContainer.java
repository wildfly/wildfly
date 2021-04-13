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

package org.jboss.as.web.common;

import java.lang.reflect.InvocationTargetException;

import org.jboss.as.ee.component.ComponentRegistry;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;

/**
 * A {@link WebInjectionContainer} implementation for use with distributable web applications that does not cache {@link ManagedReference} instances.
 * @author Paul Ferraro
 */
public class SimpleWebInjectionContainer extends AbstractWebInjectionContainer {

    public SimpleWebInjectionContainer(ClassLoader loader, ComponentRegistry componentRegistry) {
        super(loader, componentRegistry);
    }

    @Override
    public void destroyInstance(Object instance) {
        ManagedReference reference = this.getComponentRegistry().getInstance(instance);
        if (reference != null) {
            reference.release();
        }
    }

    @Override
    public Object newInstance(Class<?> clazz) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        final ManagedReferenceFactory factory = this.getComponentRegistry().createInstanceFactory(clazz);
        ManagedReference reference = factory.getReference();
        if (reference != null) {
            return reference.getInstance();
        }
        return clazz.newInstance();
    }

    @Override
    public void newInstance(Object instance) {
        this.getComponentRegistry().createInstance(instance);
    }
}

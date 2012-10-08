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
package org.jboss.as.ee.beanvalidation;

import javax.validation.ValidatorFactory;

import org.jboss.as.naming.ContextListAndJndiViewManagedReferenceFactory;
import org.jboss.as.naming.ContextListManagedReferenceFactory;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ValueManagedReference;
import org.jboss.msc.value.ImmediateValue;

/**
* @author Stuart Douglas
* @author Eduardo Martins
*/
final class ValidatorJndiInjectable implements ContextListAndJndiViewManagedReferenceFactory {
    private final ValidatorFactory factory;

    public ValidatorJndiInjectable(ValidatorFactory factory) {
        this.factory = factory;
    }

    @Override
    public ManagedReference getReference() {
        return new ValueManagedReference(new ImmediateValue<Object>(factory.getValidator()));
    }

    private Object getInstanceSafely() {
        final ClassLoader cl = SecurityActions.getContextClassLoader();
        try {
            SecurityActions.setContextClassLoader(factory.getClass().getClassLoader());
            return factory.getValidator();
        } finally {
            SecurityActions.setContextClassLoader(cl);
        }
    }

    @Override
    public String getInstanceClassName() {
        final Object instance = getInstanceSafely();
        if (instance == null) {
            return ContextListManagedReferenceFactory.DEFAULT_INSTANCE_CLASS_NAME;
        }
        return instance.getClass().getName();
    }

    @Override
    public String getJndiViewInstanceValue() {
        final Object instance = getInstanceSafely();
        if (instance == null) {
            return "null";
        }
        final ClassLoader cl = SecurityActions.getContextClassLoader();
        try {
            SecurityActions.setContextClassLoader(instance.getClass().getClassLoader());
            return instance.toString();
        } finally {
            SecurityActions.setContextClassLoader(cl);
        }
    }
}

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.naming.subsystem;

import org.jboss.as.naming.ContextListAndJndiViewManagedReferenceFactory;
import org.jboss.as.naming.ContextListManagedReferenceFactory;
import org.jboss.as.naming.ImmediateManagedReference;
import org.jboss.as.naming.ManagedReference;

import javax.naming.InitialContext;
import javax.naming.LinkRef;

/**
 * The {@link org.jboss.as.naming.ManagedReferenceFactory} for config's lookup bindings, which expose the source's lookup name.
 * @author Eduardo Martins
 */
public class LookupBindingManagedReferenceFactory implements ContextListAndJndiViewManagedReferenceFactory {

    private final String lookupName;

    public LookupBindingManagedReferenceFactory(String lookupName) {
        this.lookupName = lookupName;
    }

    @Override
    public ManagedReference getReference() {
        try {
            final Object value = new InitialContext().lookup(lookupName);
            return new ImmediateManagedReference(value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getInstanceClassName() {
        final Object value = getReference().getInstance();
        return value != null ? value.getClass().getName() : ContextListManagedReferenceFactory.DEFAULT_INSTANCE_CLASS_NAME;
    }

    @Override
    public String getJndiViewInstanceValue() {
        return String.valueOf(getReference().getInstance());
    }

    /**
     * Retrieves a {@link javax.naming.LinkRef} pointing to the binding's source JNDI name.
     * @return
     */
    public LinkRef getLink() {
        return new LinkRef(lookupName);
    }
}

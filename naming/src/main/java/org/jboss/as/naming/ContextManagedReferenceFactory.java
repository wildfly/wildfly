/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.naming;

import javax.naming.Name;
import javax.naming.NamingException;

import org.jboss.as.naming.util.NameParser;
import org.jboss.msc.value.InjectedValue;

/**
 * Managed reference factory used for binding a context.
 *
 * @author Stuart Douglas
 * @author Eduardo Martins
 */
public class ContextManagedReferenceFactory implements ContextListAndJndiViewManagedReferenceFactory {

    private final String name;
    private final InjectedValue<NamingStore> namingStoreInjectedValue = new InjectedValue<NamingStore>();

    public ContextManagedReferenceFactory(final String name) {
        this.name = name;
    }

    @Override
    public ManagedReference getReference() {
        final NamingStore namingStore = namingStoreInjectedValue.getValue();
        try {
            final Name name = NameParser.INSTANCE.parse(this.name);
            final NamingContext context =  new NamingContext(name, namingStore, null);
            return new ManagedReference() {
                @Override
                public void release() {

                }

                @Override
                public Object getInstance() {
                    return context;
                }
            };
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    public InjectedValue<NamingStore> getNamingStoreInjectedValue() {
        return namingStoreInjectedValue;
    }

    @Override
    public String getInstanceClassName() {
        return NamingContext.class.getName();
    }

    @Override
    public String getJndiViewInstanceValue() {
        return name;
    }
}

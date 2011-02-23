/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.naming.context;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;
import java.util.Hashtable;

/**
 * Object factory used to retrieve a naming context with a given name.  This relies on a {@code org.jboss.as.naming.contexts.NamespaceContextSelector}
 * to select the right naming context to return.
 *
 * @author John E. Bailey
 */
public class NamespaceObjectFactory implements ObjectFactory {

    /**
     * Create a complete reference to a @{NamespaceObjectFactory) for a given context identifier.
     *
     * @param contextIdentifier The context identifier
     * @return The reference
     */
    public static Reference createReference(final String contextIdentifier) {
        return ModularReference.create(Context.class, new StringRefAddr("nns", contextIdentifier), NamespaceObjectFactory.class);
    }

    /**
     * Get the correct the context for the provided reference object using the name specified in the reference.
     *
     * @param obj The reference object
     * @param name The name
     * @param nameCtx The current naming context
     * @param environment The environment
     * @return The selected context for this name
     * @throws Exception
     */
    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
        final Reference reference = (Reference) obj;
        final StringRefAddr nameAdr = (StringRefAddr)reference.get("nns");
        if(nameAdr == null) {
            throw new NamingException("Invalid context reference.  Not a 'nns' reference.");
        }
        final String contextName = (String)nameAdr.getContent();

        final NamespaceContextSelector selector = NamespaceContextSelector.getCurrentSelector();
        if(selector == null) {
            throw new NamingException("Failed to get context with name " + contextName);
        }
        return selector.getContext(contextName);
    }
}

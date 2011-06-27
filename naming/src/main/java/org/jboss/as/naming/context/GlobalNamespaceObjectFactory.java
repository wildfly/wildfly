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
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;
import java.util.Hashtable;

/**
 * Object factory used to retrieve a global naming context with a given name.
 *
 * @author John E. Bailey
 * @author Jason T. Greene
 */
public class GlobalNamespaceObjectFactory implements ObjectFactory {


    private static class ContextRefAddr extends RefAddr {
        private static final long serialVersionUID = 4992673639670119399L;
        private final Context context;

        ContextRefAddr(Context ctx) {
            super("context");
            this.context = ctx;
        }

        public Object getContent() {
            return context;
        }
    }

    /**
     * Create a complete reference to a @{NamespaceObjectFactory) for a given context identifier.
     *
     * @param contextIdentifier The context identifier
     * @return The reference
     */
    public static Reference createReference(final String contextIdentifier, final Context context) {
        ModularReference ref = ModularReference.create(Context.class, new StringRefAddr("nns", contextIdentifier), GlobalNamespaceObjectFactory.class);
        ref.add(new ContextRefAddr(context));
        return ref;
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
        final Object ctx = reference.get("context").getContent();

        if(!(ctx instanceof Context)) {
            throw new NamingException("Failed to get global context");
        }

        return ctx;

    }
}

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

package org.jboss.as.naming;

import org.jboss.msc.service.ServiceName;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.Reference;
import java.util.Hashtable;

/**
 * @author John Bailey
 */
public class ManagedReferenceObjectFactory extends ServiceReferenceObjectFactory {

    public static Reference createReference(final ServiceName serviceName) {
        return ServiceReferenceObjectFactory.createReference(serviceName, ManagedReferenceObjectFactory.class);
    }

    public Object getObjectInstance(final Object serviceValue, final Object obj, final Name name, final Context nameCtx, final Hashtable<?, ?> environment) throws Exception {
        final ManagedReferenceFactory managedReferenceFactory = ManagedReferenceFactory.class.cast(serviceValue);
        return managedReferenceFactory.getReference().getInstance();
    }
}

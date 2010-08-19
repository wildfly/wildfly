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

package org.jboss.as.deployment.managedbean;

import org.jboss.msc.service.ServiceName;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import java.util.Hashtable;

/**
 * Naming ObjectFactory responsible for retrieving managed bean instances from the managed bean registry when a managed
 * bean is looked up from a naming context.
 *  
 * @author John E. Bailey
 */
public class ManagedBeanObjectFactory implements ObjectFactory {
    /**
     * Get an managed bean instance by using the service name in the reference object to retrieve the manged bean
     * instance for the registry.
     *
     * @param obj The object reference
     * @param name The context name
     * @param nameCtx The naming context
     * @param environment The environment table
     * @return A new managed bean instance
     * @throws Exception
     */
    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
        final Reference reference = Reference.class.cast(obj);
        RefAddr refAddr = reference.get(0);
        final ServiceName managedBeanName = (ServiceName)refAddr.getContent();
        final ManagedBeanService<?> managedBeanService = ManagedBeanRegistry.get(managedBeanName);
        if(managedBeanService == null) {
            throw new NamingException("Managed bean does not exist with name: " + managedBeanName);
        }
        return managedBeanService.getValue();
    }

    /**
     * Create a reference used to bind an instance into a naming context.
     *
     * @param managedBeanClass The managed bean's class
     * @param serviceName The service name for the manage bean service
     * @return A reference instance
     */
    public static final Reference createReference(final Class<?> managedBeanClass, final ServiceName serviceName) {
        final RefAddr refAddr = new ManagedBeanObjectFactory.ServiceNameRefAdr(serviceName);
        final Reference reference = new Reference(managedBeanClass.getName(), refAddr, ManagedBeanObjectFactory.class.getName(), null);
        return reference;
    }

    public static final class ServiceNameRefAdr extends RefAddr {
        private static final long serialVersionUID = -8030736501810800377L;
        
        private final ServiceName serviceName;

        public ServiceNameRefAdr(final ServiceName serviceName) {
            super("ServiceName");
            this.serviceName = serviceName;
        }

        @Override
        public Object getContent() {
            return serviceName;
        }
    }
}

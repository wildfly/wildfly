/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jmx;

import java.io.ObjectInputStream;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanRegistrationException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.OperationsException;
import javax.management.ReflectionException;
import javax.management.loading.ClassLoaderRepository;

import org.jboss.as.jmx.model.Constants;
import org.jboss.as.server.jmx.MBeanServerPlugin;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class BaseMBeanServerPlugin implements MBeanServerPlugin {
    public ObjectInstance createMBean(String className, ObjectName name, Object[] params, String[] signature)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanException, NotCompliantMBeanException {
        throw new NotCompliantMBeanException("You can't create mbeans under the reserved domain '" + Constants.DOMAIN + "'");
    }

    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName, Object[] params,
            String[] signature) throws ReflectionException, InstanceAlreadyExistsException, MBeanException,
            NotCompliantMBeanException, InstanceNotFoundException {
        throw new NotCompliantMBeanException("You can't create mbeans under the reserved domain '" + Constants.DOMAIN + "'");
    }

    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName) throws ReflectionException,
            InstanceAlreadyExistsException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException {
        throw new NotCompliantMBeanException("You can't create mbeans under the reserved domain '" + Constants.DOMAIN + "'");
    }

    public ObjectInstance createMBean(String className, ObjectName name) throws ReflectionException,
            InstanceAlreadyExistsException, MBeanException, NotCompliantMBeanException {
        throw new NotCompliantMBeanException("You can't create mbeans under the reserved domain '" + Constants.DOMAIN + "'");
    }

    @Deprecated
    public ObjectInputStream deserialize(ObjectName name, byte[] data) throws OperationsException {
        throw new OperationsException("Don't know how to deserialize");
    }

    @Deprecated
    public ObjectInputStream deserialize(String className, byte[] data) throws OperationsException, ReflectionException {
        throw new OperationsException("Don't know how to deserialize");
    }

    @Deprecated
    public ObjectInputStream deserialize(String className, ObjectName loaderName, byte[] data) throws OperationsException,
            ReflectionException {
        throw new OperationsException("Don't know how to deserialize");
    }

    public ClassLoaderRepository getClassLoaderRepository() {
        throw new UnsupportedOperationException("getClassLoaderRepository is not supported");
    }

    public String getDefaultDomain() {
        throw new UnsupportedOperationException("getDefaultDomain is not supported");
    }

    public Object instantiate(String className, Object[] params, String[] signature) throws ReflectionException, MBeanException {
        throw new UnsupportedOperationException("instantiate is not supported");
    }

    public Object instantiate(String className, ObjectName loaderName, Object[] params, String[] signature)
            throws ReflectionException, MBeanException, InstanceNotFoundException {
        throw new UnsupportedOperationException("instantiate is not supported");
    }

    public Object instantiate(String className, ObjectName loaderName) throws ReflectionException, MBeanException,
            InstanceNotFoundException {
        throw new UnsupportedOperationException("instantiate is not supported");
    }

    public Object instantiate(String className) throws ReflectionException, MBeanException {
        throw new UnsupportedOperationException("instantiate is not supported");
    }

    public ObjectInstance registerMBean(Object object, ObjectName name) throws InstanceAlreadyExistsException,
            MBeanRegistrationException, NotCompliantMBeanException {
        throw new MBeanRegistrationException(new RuntimeException("You can't register mbeans under the reserved domain '"
                + Constants.DOMAIN + "'"));
    }

    public void unregisterMBean(ObjectName name) throws InstanceNotFoundException, MBeanRegistrationException {
        throw new MBeanRegistrationException(new RuntimeException("You can't unregister mbeans under the reserved domain '"
                + Constants.DOMAIN + "'"));
    }

}

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
package org.jboss.as.jmx.tcl;

import java.io.ObjectInputStream;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.OperationsException;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.loading.ClassLoaderRepository;

/**
 * An MBeanServer wrapper that sets the thread context classloader before
 * calling the delegate MBeanServer method
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class TcclMBeanServer implements MBeanServer {

    private final MBeanServer delegate;

    public TcclMBeanServer(MBeanServer delegate) {
        this.delegate = delegate;
    }

    public void addNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback)
            throws InstanceNotFoundException {
        ClassLoader old = pushClassLoader(name);
        try {
            delegate.addNotificationListener(name, listener, filter, handback);
        } finally {
            resetClassLoader(old);
        }

    }

    public void addNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback)
            throws InstanceNotFoundException {
        ClassLoader old = pushClassLoader(name);
        try {
            delegate.addNotificationListener(name, listener, filter, handback);
        } finally {
            resetClassLoader(old);
        }
    }

    public ObjectInstance createMBean(String className, ObjectName name, Object[] params, String[] signature) throws ReflectionException,
            InstanceAlreadyExistsException, MBeanException, NotCompliantMBeanException {
        return delegate.createMBean(className, name, params, signature);
    }

    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName, Object[] params, String[] signature)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanException, NotCompliantMBeanException,
            InstanceNotFoundException {
        ClassLoader old = pushClassLoaderByName(loaderName);
        try {
            return delegate.createMBean(className, name, loaderName, params, signature);
        } finally {
            resetClassLoader(old);
        }
    }

    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName) throws ReflectionException,
            InstanceAlreadyExistsException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException {

        ClassLoader old = pushClassLoaderByName(loaderName);
        try {
            return delegate.createMBean(className, name, loaderName);
        } finally {
            resetClassLoader(old);
        }

    }

    public ObjectInstance createMBean(String className, ObjectName name) throws ReflectionException, InstanceAlreadyExistsException,
             MBeanException, NotCompliantMBeanException {
        return delegate.createMBean(className, name);
    }

    public ObjectInputStream deserialize(ObjectName name, byte[] data) throws OperationsException {
        return delegate.deserialize(name, data);
    }

    public ObjectInputStream deserialize(String className, byte[] data) throws OperationsException, ReflectionException {
        return delegate.deserialize(className, data);
    }

    public ObjectInputStream deserialize(String className, ObjectName loaderName, byte[] data) throws OperationsException,
            ReflectionException {
        return delegate.deserialize(className, loaderName, data);
    }

    public Object getAttribute(ObjectName name, String attribute) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException,
            ReflectionException {
        ClassLoader old = pushClassLoader(name);
        try {
            return delegate.getAttribute(name, attribute);
        } finally {
            resetClassLoader(old);
        }
    }

    public AttributeList getAttributes(ObjectName name, String[] attributes) throws InstanceNotFoundException, ReflectionException {
        ClassLoader old = pushClassLoader(name);
        try {
            return delegate.getAttributes(name, attributes);
        } finally {
            resetClassLoader(old);
        }
    }

    public ClassLoader getClassLoader(ObjectName loaderName) throws InstanceNotFoundException {
        return delegate.getClassLoader(loaderName);
    }

    public ClassLoader getClassLoaderFor(ObjectName mbeanName) throws InstanceNotFoundException {
        return delegate.getClassLoaderFor(mbeanName);
    }

    public ClassLoaderRepository getClassLoaderRepository() {
        return delegate.getClassLoaderRepository();
    }

    public String getDefaultDomain() {
        return delegate.getDefaultDomain();
    }

    public String[] getDomains() {
        return delegate.getDomains();
    }

    public Integer getMBeanCount() {
        return delegate.getMBeanCount();
    }

    public MBeanInfo getMBeanInfo(ObjectName name) throws InstanceNotFoundException, IntrospectionException, ReflectionException {
        return delegate.getMBeanInfo(name);
    }

    public ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException {
        return delegate.getObjectInstance(name);
    }

    public Object instantiate(String className, Object[] params, String[] signature) throws ReflectionException, MBeanException {
        return delegate.instantiate(className, params, signature);
    }

    public Object instantiate(String className, ObjectName loaderName, Object[] params, String[] signature) throws ReflectionException,
            MBeanException, InstanceNotFoundException {
        ClassLoader old = pushClassLoaderByName(loaderName);
        try {
            return delegate.instantiate(className, loaderName, params, signature);
        } finally {
            resetClassLoader(old);
        }
    }

    public Object instantiate(String className, ObjectName loaderName) throws ReflectionException, MBeanException, InstanceNotFoundException {
        ClassLoader old = pushClassLoaderByName(loaderName);
        try {
            return delegate.instantiate(className, loaderName);
        } finally {
            resetClassLoader(old);
        }
    }

    public Object instantiate(String className) throws ReflectionException, MBeanException {
        return delegate.instantiate(className);
    }

    public Object invoke(ObjectName name, String operationName, Object[] params, String[] signature) throws InstanceNotFoundException,
            MBeanException, ReflectionException {

        ClassLoader old = pushClassLoader(name);
        try {
            return delegate.invoke(name, operationName, params, signature);
        } finally {
            resetClassLoader(old);
        }
    }

    public boolean isInstanceOf(ObjectName name, String className) throws InstanceNotFoundException {
        return delegate.isInstanceOf(name, className);
    }

    public boolean isRegistered(ObjectName name) {
        return delegate.isRegistered(name);
    }

    public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) {
        return delegate.queryMBeans(name, query);
    }

    public Set<ObjectName> queryNames(ObjectName name, QueryExp query) {
        return delegate.queryNames(name, query);
    }

    public ObjectInstance registerMBean(Object object, ObjectName name) throws InstanceAlreadyExistsException, MBeanRegistrationException,
            NotCompliantMBeanException {
        return delegate.registerMBean(object, name);
    }

    public void removeNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, ListenerNotFoundException {
        ClassLoader old = pushClassLoader(name);
        try {
            delegate.removeNotificationListener(name, listener, filter, handback);
        } finally {
            resetClassLoader(old);
        }
    }

    public void removeNotificationListener(ObjectName name, NotificationListener listener) throws InstanceNotFoundException,
            ListenerNotFoundException {
        ClassLoader old = pushClassLoader(name);
        try {
            delegate.removeNotificationListener(name, listener);
        } finally {
            resetClassLoader(old);
        }
    }

    public void removeNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, ListenerNotFoundException {
        ClassLoader old = pushClassLoader(name);
        try {
            delegate.removeNotificationListener(name, listener, filter, handback);
        } finally {
            resetClassLoader(old);
        }
    }

    public void removeNotificationListener(ObjectName name, ObjectName listener) throws InstanceNotFoundException, ListenerNotFoundException {
        ClassLoader old = pushClassLoader(name);
        try {
            delegate.removeNotificationListener(name, listener);
        } finally {
            resetClassLoader(old);
        }
    }

    public void setAttribute(ObjectName name, Attribute attribute) throws InstanceNotFoundException, AttributeNotFoundException,
            InvalidAttributeValueException, MBeanException, ReflectionException {
        ClassLoader old = pushClassLoader(name);
        try {
            delegate.setAttribute(name, attribute);
        } finally {
            resetClassLoader(old);
        }
    }

    public AttributeList setAttributes(ObjectName name, AttributeList attributes) throws InstanceNotFoundException, ReflectionException {
        ClassLoader old = pushClassLoader(name);
        try {
            return delegate.setAttributes(name, attributes);
        } finally {
            resetClassLoader(old);
        }
    }

    public void unregisterMBean(ObjectName name) throws InstanceNotFoundException, MBeanRegistrationException {
        delegate.unregisterMBean(name);
    }

    private ClassLoader pushClassLoader(ObjectName name) throws InstanceNotFoundException {
        ClassLoader mbeanCl = delegate.getClassLoaderFor(name);
        return SecurityActions.setThreadContextClassLoader(mbeanCl);
    }

    private ClassLoader pushClassLoaderByName(ObjectName loaderName) throws InstanceNotFoundException {
        ClassLoader mbeanCl = delegate.getClassLoader(loaderName);
        return SecurityActions.setThreadContextClassLoader(mbeanCl);
    }

    private void resetClassLoader(ClassLoader cl) {
        SecurityActions.resetThreadContextClassLoader(cl);
    }
}

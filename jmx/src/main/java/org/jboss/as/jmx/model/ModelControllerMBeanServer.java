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
package org.jboss.as.jmx.model;

import static org.jboss.as.jmx.JmxMessages.MESSAGES;

import java.io.ObjectInputStream;
import java.util.HashSet;
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

import org.jboss.as.controller.ModelController;

/**
 * An MBeanServer wrapper that exposes the ModelController via JMX.
 * <p/>
 * <b>Note:</b> This only gets invoked when connecting via JConsole
 * if you connect via a remote process URL. If you connect to a 'Local Process' the platform MBean
 * Server is used directly.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ModelControllerMBeanServer implements MBeanServer {

    private final MBeanServer delegate;
    private final ModelControllerMBeanHelper helper;

    public ModelControllerMBeanServer(MBeanServer delegate, ModelController controller) {
        this.delegate = delegate;
        helper = new ModelControllerMBeanHelper(controller);
    }

    public ObjectInstance createMBean(String className, ObjectName name, Object[] params, String[] signature) throws ReflectionException,
            InstanceAlreadyExistsException, MBeanException, NotCompliantMBeanException {
        if (ObjectNameAddressUtil.isReservedDomain(name)) {
            throw new NotCompliantMBeanException(MESSAGES.reservedMBeanDomain(Constants.DOMAIN));
        }
        return delegate.createMBean(className, name, params, signature);
    }

    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName, Object[] params, String[] signature)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanException, NotCompliantMBeanException,
            InstanceNotFoundException {
        if (ObjectNameAddressUtil.isReservedDomain(name)) {
            throw new NotCompliantMBeanException(MESSAGES.reservedMBeanDomain(Constants.DOMAIN));
        }
        return delegate.createMBean(className, name, loaderName, params, signature);
    }

    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName) throws ReflectionException,
            InstanceAlreadyExistsException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException {
        if (ObjectNameAddressUtil.isReservedDomain(name)) {
            throw new NotCompliantMBeanException(MESSAGES.reservedMBeanDomain(Constants.DOMAIN));
        }
        return delegate.createMBean(className, name, loaderName);
    }

    public ObjectInstance createMBean(String className, ObjectName name) throws ReflectionException, InstanceAlreadyExistsException,
             MBeanException, NotCompliantMBeanException {
        if (ObjectNameAddressUtil.isReservedDomain(name)) {
            throw new NotCompliantMBeanException(MESSAGES.reservedMBeanDomain(Constants.DOMAIN));
        }
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
        if (delegate.isRegistered(name)) {
            return delegate.getAttribute(name, attribute);
        }
        return helper.getAttribute(name, attribute);
    }

    public AttributeList getAttributes(ObjectName name, String[] attributes) throws InstanceNotFoundException, ReflectionException {
        if (delegate.isRegistered(name)) {
            return delegate.getAttributes(name, attributes);
        }
        return helper.getAttributes(name, attributes);
    }

    public ClassLoader getClassLoader(ObjectName loaderName) throws InstanceNotFoundException {
        if (delegate.isRegistered(loaderName)) {
            return delegate.getClassLoader(loaderName);
        }
        if (helper.resolvePathAddress(loaderName) != null) {
            return SecurityActions.getClassLoader(this.getClass());
        }
        throw ModelControllerMBeanHelper.createInstanceNotFoundException(loaderName);
    }

    public ClassLoader getClassLoaderFor(ObjectName mbeanName) throws InstanceNotFoundException {
        if (delegate.isRegistered(mbeanName)) {
            return delegate.getClassLoaderFor(mbeanName);
        }
        if (helper.resolvePathAddress(mbeanName) != null) {
            return SecurityActions.getClassLoader(this.getClass());
        }
        throw ModelControllerMBeanHelper.createInstanceNotFoundException(mbeanName);
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
        return delegate.getMBeanCount() + helper.getMBeanCount();
    }

    public MBeanInfo getMBeanInfo(ObjectName name) throws InstanceNotFoundException, IntrospectionException, ReflectionException {
        if (delegate.isRegistered(name)) {
            return delegate.getMBeanInfo(name);
        }
        return helper.getMBeanInfo(name);
    }

    public ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException {
        if (delegate.isRegistered(name)) {
            return delegate.getObjectInstance(name);
        }
        return helper.getObjectInstance(name);
    }

    public Object instantiate(String className, Object[] params, String[] signature) throws ReflectionException, MBeanException {
        return delegate.instantiate(className, params, signature);
    }

    public Object instantiate(String className, ObjectName loaderName, Object[] params, String[] signature) throws ReflectionException,
            MBeanException, InstanceNotFoundException {
        return delegate.instantiate(className, loaderName, params, signature);
    }

    public Object instantiate(String className, ObjectName loaderName) throws ReflectionException, MBeanException, InstanceNotFoundException {
        return delegate.instantiate(className, loaderName);
    }

    public Object instantiate(String className) throws ReflectionException, MBeanException {
        return delegate.instantiate(className);
    }

    public Object invoke(ObjectName name, String operationName, Object[] params, String[] signature) throws InstanceNotFoundException,
            MBeanException, ReflectionException {
        if (delegate.isRegistered(name)) {
            return delegate.invoke(name, operationName, params, signature);
        }
        return helper.invoke(name, operationName, params, signature);
    }

    public boolean isInstanceOf(ObjectName name, String className) throws InstanceNotFoundException {
        if (delegate.isRegistered(name)) {
            return delegate.isInstanceOf(name, className);
        }
        return false;
    }

    public boolean isRegistered(ObjectName name) {
        if (delegate.isRegistered(name)) {
            return true;
        }
        return helper.resolvePathAddress(name) != null;
    }

    public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) {
        HashSet<ObjectInstance> mbeans = new HashSet<ObjectInstance>();
        mbeans.addAll(delegate.queryMBeans(name, query));
        mbeans.addAll(helper.queryMBeans(name, query));
        return mbeans;
    }

    public Set<ObjectName> queryNames(ObjectName name, QueryExp query) {
        HashSet<ObjectName> mbeans = new HashSet<ObjectName>();
        mbeans.addAll(delegate.queryNames(name, query));
        mbeans.addAll(helper.queryNames(name, query));
        return mbeans;
    }

    public ObjectInstance registerMBean(Object object, ObjectName name) throws InstanceAlreadyExistsException, MBeanRegistrationException,
            NotCompliantMBeanException {
        if (ObjectNameAddressUtil.isReservedDomain(name)) {
            throw new MBeanRegistrationException(new RuntimeException(MESSAGES.reservedMBeanDomain(Constants.DOMAIN)));
        }
        return delegate.registerMBean(object, name);
    }

    public void setAttribute(ObjectName name, Attribute attribute) throws InstanceNotFoundException, AttributeNotFoundException,
            InvalidAttributeValueException, MBeanException, ReflectionException {
        if (delegate.isRegistered(name)) {
            delegate.setAttribute(name, attribute);
            return;
        }
        helper.setAttribute(name, attribute);
    }

    public AttributeList setAttributes(ObjectName name, AttributeList attributes) throws InstanceNotFoundException, ReflectionException {
        if (delegate.isRegistered(name)) {
            return delegate.setAttributes(name, attributes);
        }
        return helper.setAttributes(name, attributes);
    }

    public void unregisterMBean(ObjectName name) throws InstanceNotFoundException, MBeanRegistrationException {
        if (ObjectNameAddressUtil.isReservedDomain(name)) {
            throw new MBeanRegistrationException(new RuntimeException(MESSAGES.reservedMBeanDomain(Constants.DOMAIN)));
        }
        delegate.unregisterMBean(name);
    }

    public void addNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback)
            throws InstanceNotFoundException {
        //TODO handle notifications in model?
        delegate.addNotificationListener(name, listener, filter, handback);
    }

    public void addNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback)
            throws InstanceNotFoundException {
        //TODO handle notifications in model?
        delegate.addNotificationListener(name, listener, filter, handback);
    }

    public void removeNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, ListenerNotFoundException {
        //TODO handle notifications in model?
        delegate.removeNotificationListener(name, listener, filter, handback);
    }

    public void removeNotificationListener(ObjectName name, NotificationListener listener) throws InstanceNotFoundException,
            ListenerNotFoundException {
        //TODO handle notifications in model?
        delegate.removeNotificationListener(name, listener);
    }

    public void removeNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, ListenerNotFoundException {
        //TODO handle notifications in model?
        delegate.removeNotificationListener(name, listener, filter, handback);
    }

    public void removeNotificationListener(ObjectName name, ObjectName listener) throws InstanceNotFoundException, ListenerNotFoundException {
        //TODO handle notifications in model?
        delegate.removeNotificationListener(name, listener);
    }
}

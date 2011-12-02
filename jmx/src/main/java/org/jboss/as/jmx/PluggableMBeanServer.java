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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

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
 * An MBeanServer supporting {@link MBeanServerPlugin}s. At it's core is the original platform mbean server wrapped in TCCL behaviour.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class PluggableMBeanServer implements MBeanServer {

    private final MBeanServer rootMBeanServer;

    private final Set<MBeanServerPlugin> delegates = new CopyOnWriteArraySet<MBeanServerPlugin>();

    PluggableMBeanServer(MBeanServer rootMBeanServer) {
        this.rootMBeanServer = new TcclMBeanServer(rootMBeanServer);
    }

    public void addPlugin(MBeanServerPlugin delegate) {
        delegates.add(delegate);
    }

    public void removePlugin(MBeanServerPlugin delegate) {
        delegates.remove(delegate);
    }

    private MBeanServer findDelegate(ObjectName name) {
        if (name == null) {
            throw new IllegalArgumentException("Object name can't be null");
        }
        if (delegates.size() > 0) {
            for (MBeanServerPlugin delegate : delegates) {
                if (delegate.accepts(name)) {
                    return delegate;
                }
            }
        }
        return rootMBeanServer;
    }

    @Override
    public void addNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback)
            throws InstanceNotFoundException {
        findDelegate(name).addNotificationListener(name, listener, filter, handback);
    }

    @Override
    public void addNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback)
            throws InstanceNotFoundException {
        findDelegate(name).addNotificationListener(name, listener, filter, handback);
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, Object[] params, String[] signature)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanException,
            NotCompliantMBeanException {
        return findDelegate(name).createMBean(className, name, params, signature);
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName, Object[] params,
            String[] signature) throws ReflectionException, InstanceAlreadyExistsException,
            MBeanException, NotCompliantMBeanException, InstanceNotFoundException {
        return findDelegate(name).createMBean(className, name, loaderName, params, signature);
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName) throws ReflectionException,
            InstanceAlreadyExistsException, MBeanException, NotCompliantMBeanException,
            InstanceNotFoundException {
        return findDelegate(name).createMBean(className, name, loaderName);
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name) throws ReflectionException,
            InstanceAlreadyExistsException, MBeanException, NotCompliantMBeanException {
        return findDelegate(name).createMBean(className, name);
    }

    @Override
    public ObjectInputStream deserialize(ObjectName name, byte[] data) throws OperationsException {
        return findDelegate(name).deserialize(name, data);
    }

    @Override
    public ObjectInputStream deserialize(String className, byte[] data) throws OperationsException, ReflectionException {
        return rootMBeanServer.deserialize(className, data);
    }

    @Override
    public ObjectInputStream deserialize(String className, ObjectName loaderName, byte[] data) throws OperationsException, ReflectionException {
        return rootMBeanServer.deserialize(className, loaderName, data);
    }

    @Override
    public Object getAttribute(ObjectName name, String attribute) throws MBeanException, AttributeNotFoundException,
            InstanceNotFoundException, ReflectionException {
        return findDelegate(name).getAttribute(name, attribute);
    }

    @Override
    public AttributeList getAttributes(ObjectName name, String[] attributes) throws InstanceNotFoundException,
            ReflectionException {
        return findDelegate(name).getAttributes(name, attributes);
    }

    @Override
    public ClassLoader getClassLoader(ObjectName loaderName) throws InstanceNotFoundException {
        return findDelegate(loaderName).getClassLoader(loaderName);
    }

    @Override
    public ClassLoader getClassLoaderFor(ObjectName mbeanName) throws InstanceNotFoundException {
        return findDelegate(mbeanName).getClassLoaderFor(mbeanName);
    }

    @Override
    public ClassLoaderRepository getClassLoaderRepository() {
        return rootMBeanServer.getClassLoaderRepository();
    }

    @Override
    public String getDefaultDomain() {
        return rootMBeanServer.getDefaultDomain();
    }

    @Override
    public String[] getDomains() {
        ArrayList<String> result = new ArrayList<String>();
        if (delegates.size() > 0) {
            for (MBeanServerPlugin delegate : delegates) {
                String[] domains = delegate.getDomains();
                if (domains.length > 0) {
                    result.addAll(Arrays.asList(domains));
                }
            }
        }
        result.addAll(Arrays.asList(rootMBeanServer.getDomains()));
        return result.toArray(new String[result.size()]);
    }

    @Override
    public Integer getMBeanCount() {
        int i = 0;
        if (delegates.size() > 0) {
            for (MBeanServerPlugin delegate : delegates) {
                i += delegate.getMBeanCount();
            }
        }
        i += rootMBeanServer.getMBeanCount();
        return i;
    }

    @Override
    public MBeanInfo getMBeanInfo(ObjectName name) throws InstanceNotFoundException, IntrospectionException, ReflectionException {
        return findDelegate(name).getMBeanInfo(name);
    }

    @Override
    public ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException {
        return findDelegate(name).getObjectInstance(name);
    }

    @Override
    public Object instantiate(String className, Object[] params, String[] signature) throws ReflectionException, MBeanException {
        return rootMBeanServer.instantiate(className, params, signature);
    }

    @Override
    public Object instantiate(String className, ObjectName loaderName, Object[] params, String[] signature)
            throws ReflectionException, MBeanException, InstanceNotFoundException {
        return rootMBeanServer.instantiate(className, loaderName, params, signature);
    }

    @Override
    public Object instantiate(String className, ObjectName loaderName) throws ReflectionException, MBeanException,
            InstanceNotFoundException {
        return rootMBeanServer.instantiate(className, loaderName);
    }

    @Override
    public Object instantiate(String className) throws ReflectionException, MBeanException {
        return rootMBeanServer.instantiate(className);
    }

    @Override
    public Object invoke(ObjectName name, String operationName, Object[] params, String[] signature)
            throws InstanceNotFoundException, MBeanException, ReflectionException {
        try {
            return findDelegate(name).invoke(name, operationName, params, signature);
        } catch (InstanceNotFoundException e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public boolean isInstanceOf(ObjectName name, String className) throws InstanceNotFoundException {
        return findDelegate(name).isInstanceOf(name, className);
    }

    @Override
    public boolean isRegistered(ObjectName name) {
        return findDelegate(name).isRegistered(name);
    }

    @Override
    public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) {
        Set<ObjectInstance> result = new HashSet<ObjectInstance>();
        if (delegates.size() > 0) {
            for (MBeanServerPlugin delegate : delegates) {
                result.addAll(delegate.queryMBeans(name, query));
            }
        }
        result.addAll(rootMBeanServer.queryMBeans(name, query));
        return result;
    }

    @Override
    public Set<ObjectName> queryNames(ObjectName name, QueryExp query) {
        Set<ObjectName> result = new HashSet<ObjectName>();
        if (delegates.size() > 0) {
            for (MBeanServerPlugin delegate : delegates) {
                result.addAll(delegate.queryNames(name, query));
            }
        }
        result.addAll(rootMBeanServer.queryNames(name, query));
        return result;
    }

    @Override
    public ObjectInstance registerMBean(Object object, ObjectName name) throws InstanceAlreadyExistsException,
            MBeanRegistrationException, NotCompliantMBeanException {
        return findDelegate(name).registerMBean(object, name);
    }

    @Override
    public void removeNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter,
            Object handback) throws InstanceNotFoundException, ListenerNotFoundException {
        findDelegate(name).removeNotificationListener(name, listener, filter, handback);
    }

    @Override
    public void removeNotificationListener(ObjectName name, NotificationListener listener) throws InstanceNotFoundException,
            ListenerNotFoundException {
        findDelegate(name).removeNotificationListener(name, listener);
    }

    @Override
    public void removeNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, ListenerNotFoundException {
        findDelegate(name).removeNotificationListener(name, listener, filter, handback);
    }

    @Override
    public void removeNotificationListener(ObjectName name, ObjectName listener) throws InstanceNotFoundException,
            ListenerNotFoundException {
        findDelegate(name).removeNotificationListener(name, listener);
    }

    @Override
    public void setAttribute(ObjectName name, Attribute attribute) throws InstanceNotFoundException, AttributeNotFoundException,
            InvalidAttributeValueException, MBeanException, ReflectionException {
        findDelegate(name).setAttribute(name, attribute);
    }

    @Override
    public AttributeList setAttributes(ObjectName name, AttributeList attributes) throws InstanceNotFoundException,
            ReflectionException {
        return findDelegate(name).setAttributes(name, attributes);
    }

    @Override
    public void unregisterMBean(ObjectName name) throws InstanceNotFoundException, MBeanRegistrationException {
        findDelegate(name).unregisterMBean(name);
    }

    private class TcclMBeanServer implements MBeanServer {

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

}

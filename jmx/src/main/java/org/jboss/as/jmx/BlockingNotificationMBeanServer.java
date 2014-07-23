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

package org.jboss.as.jmx;

import static org.jboss.as.jmx.JmxMessages.MESSAGES;

import java.io.ObjectInputStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

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
import javax.management.MBeanServerNotification;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.OperationsException;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.RuntimeOperationsException;
import javax.management.loading.ClassLoaderRepository;

/**
 * MBeanServer wrapper that does <strong>not</strong> forward calls
 * to add/remove a notification listener for domains exposing WildFly model controllers
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
public class BlockingNotificationMBeanServer implements MBeanServer {

    private MBeanServer mbs;
    private final String resolvedDomain;
    private final String expressionsDomain;

    /**
     * Keep an association between the incoming &lt;name,listener,filter,handback&gt; tuple and a BlockingNotificationFilter
     * that prevents leaking out WildFly notifications.
     * The PlatformMBeanServer is checking the object identity when it is removing a notification listener. This association makes
     * sure we will pass the same {@code BlockingNotificationFilter} to the removeNotificationListener() method that the one passed in the
     * addNotificationListener().
     */
    private final Map<Association, BlockingNotificationFilter> associations = new ConcurrentHashMap<Association, BlockingNotificationFilter>();

    /**
     *
     * @param mbs the JMX MBeanServer (can not be {@code null}
     * @param resolvedDomain JMX domain name for the 'resolved' model controller (can be {@code null} if the model controller is not exposed)
     * @param expressionsDomain JMX domain name for the 'expression' model controller (can be {@code null} if the model controller is not exposed)
     */
    public BlockingNotificationMBeanServer(MBeanServer mbs, String resolvedDomain, String expressionsDomain) {
        this.mbs = mbs;
        this.resolvedDomain = resolvedDomain;
        this.expressionsDomain = expressionsDomain;
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name) throws ReflectionException, InstanceAlreadyExistsException, MBeanException, NotCompliantMBeanException {
        return mbs.createMBean(className, name);
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName) throws ReflectionException, InstanceAlreadyExistsException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException {
        return mbs.createMBean(className, name, loaderName);
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, Object[] params, String[] signature) throws ReflectionException, InstanceAlreadyExistsException, MBeanException, NotCompliantMBeanException {
        return mbs.createMBean(className, name, params, signature);
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName, Object[] params, String[] signature) throws ReflectionException, InstanceAlreadyExistsException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException {
        return mbs.createMBean(className, name, loaderName, params, signature);
    }

    @Override
    public ObjectInstance registerMBean(Object object, ObjectName name) throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
        return mbs.registerMBean(object, name);
    }

    @Override
    public void unregisterMBean(ObjectName name) throws InstanceNotFoundException, MBeanRegistrationException {
        mbs.unregisterMBean(name);
    }

    @Override
    public ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException {
        return mbs.getObjectInstance(name);
    }

    @Override
    public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) {
        return mbs.queryMBeans(name, query);
    }

    @Override
    public Set<ObjectName> queryNames(ObjectName name, QueryExp query) {
        return mbs.queryNames(name, query);
    }

    @Override
    public boolean isRegistered(ObjectName name) {
        return mbs.isRegistered(name);
    }

    @Override
    public Integer getMBeanCount() {
        return mbs.getMBeanCount();
    }

    @Override
    public Object getAttribute(ObjectName name, String attribute) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException {
        return mbs.getAttribute(name, attribute);
    }

    @Override
    public AttributeList getAttributes(ObjectName name, String[] attributes) throws InstanceNotFoundException, ReflectionException {
        return mbs.getAttributes(name, attributes);
    }

    @Override
    public void setAttribute(ObjectName name, Attribute attribute) throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        mbs.setAttribute(name, attribute);
    }

    @Override
    public AttributeList setAttributes(ObjectName name, AttributeList attributes) throws InstanceNotFoundException, ReflectionException {
        return mbs.setAttributes(name, attributes);
    }

    @Override
    public Object invoke(ObjectName name, String operationName, Object[] params, String[] signature) throws InstanceNotFoundException, MBeanException, ReflectionException {
        return mbs.invoke(name, operationName, params, signature);
    }

    @Override
    public String getDefaultDomain() {
        return mbs.getDefaultDomain();
    }

    @Override
    public String[] getDomains() {
        return mbs.getDomains();
    }

    @Override
    public void addNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException {
        boolean inExposedModelControllerDomains = isInExposedModelControllerDomains(name);

        if (inExposedModelControllerDomains) {
            throw new RuntimeOperationsException(MESSAGES.addNotificationListenerNotAllowed(name));
        }

        BlockingNotificationFilter blockingFilter = new BlockingNotificationFilter(filter);
        associations.put(new Association(name, listener, filter, handback), blockingFilter);
        mbs.addNotificationListener(name, listener, blockingFilter, handback);
    }

    @Override
    public void addNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException {
        boolean inExposedModelControllerDomains = isInExposedModelControllerDomains(name);

        if (inExposedModelControllerDomains) {
            throw new RuntimeOperationsException(MESSAGES.addNotificationListenerNotAllowed(name));
        }

        BlockingNotificationFilter blockingFilter = new BlockingNotificationFilter(filter);
        associations.put(new Association(name, listener, filter, handback), blockingFilter);
        mbs.addNotificationListener(name, listener, new BlockingNotificationFilter(filter), handback);
    }

    @Override
    public void removeNotificationListener(ObjectName name, ObjectName listener) throws InstanceNotFoundException, ListenerNotFoundException {
        if (isInExposedModelControllerDomains(name)) {
            throw new RuntimeOperationsException(MESSAGES.removeNotificationListenerNotAllowed(name));
        }
        mbs.removeNotificationListener(name, listener);
    }

    @Override
    public void removeNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, ListenerNotFoundException {
        if (isInExposedModelControllerDomains(name)) {
            throw new RuntimeOperationsException(MESSAGES.removeNotificationListenerNotAllowed(name));
        }
        BlockingNotificationFilter blockingFilter = associations.get(new Association(name, listener, filter, handback));
        if (blockingFilter != null) {
            mbs.removeNotificationListener(name, listener, blockingFilter, handback);
        } else {
            mbs.removeNotificationListener(name, listener, filter, handback);
        }
    }

    @Override
    public void removeNotificationListener(ObjectName name, NotificationListener listener) throws InstanceNotFoundException, ListenerNotFoundException {
        if (isInExposedModelControllerDomains(name)) {
            throw new RuntimeOperationsException(MESSAGES.removeNotificationListenerNotAllowed(name));
        }
        mbs.removeNotificationListener(name, listener);
    }

    @Override
    public void removeNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, ListenerNotFoundException {
        if (isInExposedModelControllerDomains(name)) {
            throw new RuntimeOperationsException(MESSAGES.removeNotificationListenerNotAllowed(name));
        }
        BlockingNotificationFilter blockingFilter = associations.get(new Association(name, listener, filter, handback));
        if (blockingFilter != null) {
            mbs.removeNotificationListener(name, listener, blockingFilter, handback);
        } else {
            mbs.removeNotificationListener(name, listener, filter, handback);
        }
    }

    @Override
    public MBeanInfo getMBeanInfo(ObjectName name) throws InstanceNotFoundException, IntrospectionException, ReflectionException {
        return mbs.getMBeanInfo(name);
    }

    @Override
    public boolean isInstanceOf(ObjectName name, String className) throws InstanceNotFoundException {
        return mbs.isInstanceOf(name, className);
    }

    @Override
    public Object instantiate(String className) throws ReflectionException, MBeanException {
        return mbs.instantiate(className);
    }

    @Override
    public Object instantiate(String className, ObjectName loaderName) throws ReflectionException, MBeanException, InstanceNotFoundException {
        return mbs.instantiate(className, loaderName);
    }

    @Override
    public Object instantiate(String className, Object[] params, String[] signature) throws ReflectionException, MBeanException {
        return mbs.instantiate(className, params, signature);
    }

    @Override
    public Object instantiate(String className, ObjectName loaderName, Object[] params, String[] signature) throws ReflectionException, MBeanException, InstanceNotFoundException {
        return mbs.instantiate(className, loaderName, params, signature);
    }

    @Override
    public ObjectInputStream deserialize(ObjectName name, byte[] data) throws OperationsException {
        return mbs.deserialize(name, data);
    }

    @Override
    public ObjectInputStream deserialize(String className, byte[] data) throws OperationsException, ReflectionException {
        return mbs.deserialize(className, data);
    }

    @Override
    public ObjectInputStream deserialize(String className, ObjectName loaderName, byte[] data) throws OperationsException, ReflectionException {
        return mbs.deserialize(className, loaderName, data);
    }

    @Override
    public ClassLoader getClassLoaderFor(ObjectName mbeanName) throws InstanceNotFoundException {
        return mbs.getClassLoaderFor(mbeanName);
    }

    @Override
    public ClassLoader getClassLoader(ObjectName loaderName) throws InstanceNotFoundException {
        return mbs.getClassLoaderFor(loaderName);
    }

    @Override
    public ClassLoaderRepository getClassLoaderRepository() {
        return mbs.getClassLoaderRepository();
    }


    private boolean isInExposedModelControllerDomains(ObjectName name) {
        String domain = name.getDomain();

        if (!name.isDomainPattern()) {
            if (domain.equals(resolvedDomain) || domain.equals(expressionsDomain)) {
                return true;
            }
        }
        Pattern p = Pattern.compile(name.getDomain().replace("*", ".*"));
        if (p.matcher(resolvedDomain).matches() || p.matcher(expressionsDomain).matches()) {
            return true;
        }

        return false;
    }

    /**
     * Block MBeanServerNotification corresponding to WildFly MBeans.
     */
    private class BlockingNotificationFilter implements NotificationFilter {
        private final NotificationFilter filter;

        private BlockingNotificationFilter(NotificationFilter filter) {
            this.filter = filter;
        }

        @Override
        public boolean isNotificationEnabled(Notification notification) {
            if (notification instanceof MBeanServerNotification) {
                MBeanServerNotification notif = (MBeanServerNotification) notification;
                if (notif.getMBeanName().getDomain().equals(resolvedDomain) ||
                        notif.getMBeanName().getDomain().equals(expressionsDomain)) {
                    return false;
                }
            }
            if (filter == null) {
                return true;
            } else {
                return filter.isNotificationEnabled(notification);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            BlockingNotificationFilter that = (BlockingNotificationFilter) o;

            if (filter != null ? !filter.equals(that.filter) : that.filter != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return filter != null ? filter.hashCode() : 0;
        }
    }

    private static final class Association {
        private final ObjectName name;
        private final Object listener;
        private final NotificationFilter filter;
        private final Object handback;

        /**
         * @param name must not be {@code null}
         * @param listener must not be {@code null}
         * @param filter can be {@code null}
         * @param handback can be {@code null}
         */
        private Association(ObjectName name, Object listener, NotificationFilter filter, Object handback) {
            this.name = name;
            this.filter = filter;
            this.listener = listener;
            this.handback = handback;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Association that = (Association) o;

            if (filter != null ? !filter.equals(that.filter) : that.filter != null) return false;
            if (handback != null ? !handback.equals(that.handback) : that.handback != null) return false;
            if (!listener.equals(that.listener)) return false;
            if (!name.equals(that.name)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + listener.hashCode();
            result = 31 * result + (filter != null ? filter.hashCode() : 0);
            result = 31 * result + (handback != null ? handback.hashCode() : 0);
            return result;
        }
    }
}

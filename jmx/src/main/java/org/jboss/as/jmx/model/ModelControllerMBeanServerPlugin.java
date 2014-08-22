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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESOURCE_ADDED_NOTIFICATION;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import javax.management.Attribute;
import javax.management.AttributeChangeNotification;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerNotification;
import javax.management.NotificationBroadcaster;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.RuntimeOperationsException;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.notification.Notification;
import org.jboss.as.controller.notification.NotificationFilter;
import org.jboss.as.controller.notification.NotificationHandler;
import org.jboss.as.controller.notification.NotificationRegistry;
import org.jboss.as.controller.operations.global.GlobalNotifications;
import org.jboss.as.jmx.BaseMBeanServerPlugin;
import org.jboss.dmr.ModelNode;

/**
 * An MBeanServer wrapper that exposes the ModelController via JMX.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ModelControllerMBeanServerPlugin extends BaseMBeanServerPlugin {

    private final ConfiguredDomains configuredDomains;
    private final ModelControllerMBeanHelper legacyHelper;
    private final ModelControllerMBeanHelper exprHelper;
    private final NotificationRegistry notificationRegistry;
    private final AtomicLong notificationSequenceNumber = new AtomicLong(0);

    public ModelControllerMBeanServerPlugin(final ConfiguredDomains configuredDomains, ModelController controller, final MBeanServerDelegate delegate,
                                            boolean legacyWithProperPropertyFormat, boolean forStandalone) {
        assert configuredDomains != null;
        this.configuredDomains = configuredDomains;
        this.notificationRegistry = controller.getNotificationRegistry();

        legacyHelper = configuredDomains.getLegacyDomain() != null ?
                new ModelControllerMBeanHelper(TypeConverters.createLegacyTypeConverters(legacyWithProperPropertyFormat),
                        configuredDomains, configuredDomains.getLegacyDomain(), controller, forStandalone) : null;
        exprHelper = configuredDomains.getExprDomain() != null ?
                new ModelControllerMBeanHelper(TypeConverters.createExpressionTypeConverters(), configuredDomains,
                        configuredDomains.getExprDomain(), controller, forStandalone) : null;

        // JMX notifications for MBean registration/unregistration are emitted by the MBeanServerDelegate and not by the
        // MBeans itself. If we have a reference on the delegate, we add a listener for any WildFly resource address
        // that converts the resource-added and resource-removed notifications to MBeanServerNotification and send them
        // through the delegate.
        if (delegate != null) {
            for (String domain : configuredDomains.getDomains()) {
                ResourceRegistrationNotificationHandler handler = new ResourceRegistrationNotificationHandler(delegate, domain);
                notificationRegistry.registerNotificationHandler(NotificationRegistry.ANY_ADDRESS, handler, handler);
            }
        }
    }

    @Override
    public boolean accepts(ObjectName objectName) {
        String domain = objectName.getDomain();
        if (!objectName.isDomainPattern()) {
            return domain.equals(configuredDomains.getLegacyDomain()) || domain.equals(configuredDomains.getExprDomain());
        }

        Pattern p = Pattern.compile(objectName.getDomain().replace("*", ".*"));
        return p.matcher(configuredDomains.getLegacyDomain()).matches() || p.matcher(configuredDomains.getExprDomain()).matches();
    }

    @Override
    public boolean shouldAuditLog() {
        return false;
    }

    @Override
    public boolean shouldAuthorize() {
        return false;
    }


    public Object getAttribute(ObjectName name, String attribute) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException,
            ReflectionException {
        return getHelper(name).getAttribute(name, attribute);
    }

    public AttributeList getAttributes(ObjectName name, String[] attributes) throws InstanceNotFoundException, ReflectionException {
        return getHelper(name).getAttributes(name, attributes);
    }

    public ClassLoader getClassLoader(ObjectName loaderName) throws InstanceNotFoundException {
        if (getHelper(loaderName).resolvePathAddress(loaderName) != null) {
            return SecurityActions.getClassLoader(this.getClass());
        }
        throw MESSAGES.mbeanNotFound(loaderName);
    }

    public ClassLoader getClassLoaderFor(ObjectName mbeanName) throws InstanceNotFoundException {
        if (getHelper(mbeanName).toPathAddress(mbeanName) != null) {
            return SecurityActions.getClassLoader(this.getClass());
        }
        throw MESSAGES.mbeanNotFound(mbeanName);
    }

    public String[] getDomains() {
        return configuredDomains.getDomains();
    }

    public Integer getMBeanCount() {
        int count = 0;
        if (legacyHelper != null) {
            count += legacyHelper.getMBeanCount();
        }
        if (exprHelper != null) {
            count += exprHelper.getMBeanCount();
        }
        return count;
    }

    public MBeanInfo getMBeanInfo(ObjectName name) throws InstanceNotFoundException, IntrospectionException, ReflectionException {
        return getHelper(name).getMBeanInfo(name);
    }

    public ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException {
        return getHelper(name).getObjectInstance(name);
    }

    public Object invoke(ObjectName name, String operationName, Object[] params, String[] signature) throws InstanceNotFoundException,
            MBeanException, ReflectionException {
        return getHelper(name).invoke(name, operationName, params, signature);
    }

    public boolean isInstanceOf(ObjectName name, String className) throws InstanceNotFoundException {
        // return true for NotificationBroadcaster so that client connected to the MBeanServer know
        // that it can broadcast notifications.
        if (NotificationBroadcaster.class.getName().equals(className)) {
            return true;
        }
        return false;
    }

    public boolean isRegistered(ObjectName name) {
        return getHelper(name).resolvePathAddress(name) != null;
    }

    public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) {
        if (name != null && !name.isDomainPattern()) {
            return getHelper(name).queryMBeans(name, query);
        } else {
            Set<ObjectInstance> instances = new HashSet<ObjectInstance>();
            if (legacyHelper != null) {
                instances.addAll(legacyHelper.queryMBeans(name, query));
            }
            if (exprHelper != null) {
                instances.addAll(exprHelper.queryMBeans(name, query));
            }
            return instances;
        }
    }

    public Set<ObjectName> queryNames(ObjectName name, QueryExp query) {
        if (name != null && !name.isDomainPattern()) {
            return getHelper(name).queryNames(name, query);
        } else {
            Set<ObjectName> instances = new HashSet<ObjectName>();
            if (legacyHelper != null) {
                instances.addAll(legacyHelper.queryNames(name, query));
            }
            if (exprHelper != null) {
                instances.addAll(exprHelper.queryNames(name, query));
            }
            return instances;
        }
    }

    public void setAttribute(ObjectName name, Attribute attribute) throws InstanceNotFoundException, AttributeNotFoundException,
            InvalidAttributeValueException, MBeanException, ReflectionException {
        getHelper(name).setAttribute(name, attribute);
    }

    public AttributeList setAttributes(ObjectName name, AttributeList attributes) throws InstanceNotFoundException, ReflectionException {
        return getHelper(name).setAttributes(name, attributes);
    }

    public void addNotificationListener(final ObjectName name, final NotificationListener listener, final javax.management.NotificationFilter filter, final Object handback)
            throws InstanceNotFoundException {
        PathAddress pathAddress = getHelper(name).toPathAddress(name);
        JMXNotificationHandler handler = new JMXNotificationHandler(getHelper(name).getDomain(), name, listener, filter, handback);
        notificationRegistry.registerNotificationHandler(pathAddress, handler, handler);
    }

    public void addNotificationListener(ObjectName name, ObjectName listener, javax.management.NotificationFilter filter, Object handback)
            throws InstanceNotFoundException {
        throw new RuntimeOperationsException(MESSAGES.addNotificationListerWithObjectNameNotSupported(listener));
    }

    public void removeNotificationListener(ObjectName name, NotificationListener listener, javax.management.NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, ListenerNotFoundException {
        PathAddress pathAddress = getHelper(name).toPathAddress(name);
        JMXNotificationHandler handler = new JMXNotificationHandler(getHelper(name).getDomain(), name, listener, filter, handback);
        notificationRegistry.unregisterNotificationHandler(pathAddress, handler, handler);
    }

    public void removeNotificationListener(ObjectName name, NotificationListener listener) throws InstanceNotFoundException,
            ListenerNotFoundException {
        removeNotificationListener(name, listener, null, null);
    }

    public void removeNotificationListener(ObjectName name, ObjectName listener, javax.management.NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, ListenerNotFoundException {
        throw new RuntimeOperationsException(MESSAGES.removeNotificationListerWithObjectNameNotSupported(listener));
    }

    public void removeNotificationListener(ObjectName name, ObjectName listener) throws InstanceNotFoundException, ListenerNotFoundException {
        throw new RuntimeOperationsException(MESSAGES.removeNotificationListerWithObjectNameNotSupported(listener));
    }

    private ModelControllerMBeanHelper getHelper(ObjectName name) {
        String domain = name.getDomain();
        if (domain.equals(configuredDomains.getLegacyDomain()) || name.isDomainPattern()) {
            return legacyHelper;
        }
        if (domain.equals(configuredDomains.getExprDomain())) {
            return exprHelper;
        }
        //This should not happen
        throw MESSAGES.unknownDomain(domain);
    }

    /**
     * Handle WildFly notifications, convert them to JMX notifications and forward them to the JMX listener.
     */
    private class JMXNotificationHandler implements NotificationHandler, NotificationFilter {

        private final String domain;
        private final ObjectName name;
        private final NotificationListener listener;
        private final javax.management.NotificationFilter filter;
        private final Object handback;

        private JMXNotificationHandler(String domain, ObjectName name, NotificationListener listener, javax.management.NotificationFilter filter, Object handback) {
            this.domain = domain;
            this.name = name;
            this.listener = listener;
            this.filter = filter;
            this.handback = handback;
        }

        @Override
        public boolean isNotificationEnabled(Notification notification) {
            if (isResourceAddedOrRemovedNotification(notification)) {
                // filter outs resource-added and resource-removed notifications that are handled by the
                // MBeanServerDelegate
                return false;
            }

            if (filter == null) {
                return true;
            } else {
                javax.management.Notification jmxNotification = convert(notification);
                return filter.isNotificationEnabled(jmxNotification);
            }
        }

        @Override
        public void handleNotification(Notification notification) {
            javax.management.Notification jmxNotification = convert(notification);
            if (jmxNotification != null) {
            listener.handleNotification(jmxNotification, handback);
            }
        }

        private javax.management.Notification convert(Notification notification) {
            long sequenceNumber = notificationSequenceNumber.incrementAndGet();
            ObjectName source = ObjectNameAddressUtil.createObjectName(domain, notification.getSource());
            String message = notification.getMessage();
            long timestamp = notification.getTimestamp();
            String notificationType = notification.getType();
            javax.management.Notification jmxNotification;
            if (notificationType.equals(ModelDescriptionConstants.ATTRIBUTE_VALUE_WRITTEN_NOTIFICATION)) {
                ModelNode data = notification.getData();
                String attributeName = data.get(ModelDescriptionConstants.NAME).asString();
                String jmxAttributeName = NameConverter.convertToCamelCase(attributeName);
                try {
                    ModelNode modelDescription = getHelper(source).getMBeanRegistration(source).getModelDescription(PathAddress.EMPTY_ADDRESS).getModelDescription(null);
                    ModelNode attributeDescription = modelDescription.get(ATTRIBUTES, attributeName);
                    TypeConverters converters = getHelper(source).getConverters();
                    Object oldValue = converters.fromModelNode(attributeDescription, data.get(GlobalNotifications.OLD_VALUE));
                    Object newValue = converters.fromModelNode(attributeDescription, data.get(GlobalNotifications.NEW_VALUE));
                    String attributeType = converters.convertToMBeanType(attributeDescription).getTypeName();
                    jmxNotification = new AttributeChangeNotification(source, sequenceNumber, timestamp, message, jmxAttributeName, attributeType, oldValue, newValue);

                } catch (InstanceNotFoundException e) {
                    // fallback to a generic notification
                    jmxNotification = new javax.management.Notification(notification.getType(), source, sequenceNumber, timestamp, message);

                }
            } else if (notificationType.equals(ModelDescriptionConstants.RESOURCE_ADDED_NOTIFICATION) ||
                notificationType.equals(ModelDescriptionConstants.RESOURCE_REMOVED_NOTIFICATION)) {
                // do not convert resource-added and resource-removed notifications: for JMX, they are not emitted by the MBean itself
                // but by the MBeanServerDelegate (see ModelControllerMBeanServerPlugin constructor)
                jmxNotification = null;
            } else {
                jmxNotification = new javax.management.Notification(notificationType, source, sequenceNumber, timestamp, message);
                jmxNotification.setUserData(notification.getData());
            }
            return jmxNotification;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            JMXNotificationHandler that = (JMXNotificationHandler) o;

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

    /**
     * Handle resource-added and resource-removed notifications that are converted to MBeanServerNotifcations
     * emitted by the MBeanServerDelegate
     */
    private static class ResourceRegistrationNotificationHandler implements NotificationHandler, NotificationFilter {

        private final MBeanServerDelegate delegate;
        private final String domain;
        private AtomicLong sequence = new AtomicLong(0);

        private ResourceRegistrationNotificationHandler(MBeanServerDelegate delegate, String domain) {
            this.delegate = delegate;
            this.domain = domain;
        }

        @Override
        public void handleNotification(Notification notification) {
            String jmxType = notification.getType().equals(RESOURCE_ADDED_NOTIFICATION) ? MBeanServerNotification.REGISTRATION_NOTIFICATION : MBeanServerNotification.UNREGISTRATION_NOTIFICATION;
            ObjectName mbeanName = ObjectNameAddressUtil.createObjectName(domain, notification.getSource());
            javax.management.Notification jmxNotification = new MBeanServerNotification(jmxType, MBeanServerDelegate.DELEGATE_NAME, sequence.incrementAndGet(), mbeanName);
            delegate.sendNotification(jmxNotification);
        }

        @Override
        public boolean isNotificationEnabled(Notification notification) {
            return isResourceAddedOrRemovedNotification(notification);
        }
    }

    private static boolean isResourceAddedOrRemovedNotification(Notification notification) {
        return notification.getType().equals(RESOURCE_ADDED_NOTIFICATION) ||
                notification.getType().equals(ModelDescriptionConstants.RESOURCE_REMOVED_NOTIFICATION);
    }
}

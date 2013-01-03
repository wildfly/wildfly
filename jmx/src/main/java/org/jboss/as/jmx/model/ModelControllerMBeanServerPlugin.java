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

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;

import org.jboss.as.controller.ModelController;
import org.jboss.as.jmx.BaseMBeanServerPlugin;

/**
 * An MBeanServer wrapper that exposes the ModelController via JMX.
 * <p/>
 * <b>Note:</b> This only gets invoked when connecting via JConsole
 * if you connect via a remote process URL. If you connect to a 'Local Process' the platform MBean
 * Server is used directly.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ModelControllerMBeanServerPlugin extends BaseMBeanServerPlugin {

    private final ConfiguredDomains configuredDomains;
    private final ModelControllerMBeanHelper legacyHelper;
    private final ModelControllerMBeanHelper exprHelper;

    public ModelControllerMBeanServerPlugin(ConfiguredDomains configuredDomains, ModelController controller, boolean legacyWithProperPropertyFormat) {
        assert configuredDomains != null;
        this.configuredDomains = configuredDomains;

        legacyHelper = configuredDomains.getLegacyDomain() != null ?
                new ModelControllerMBeanHelper(TypeConverters.createLegacyTypeConverters(legacyWithProperPropertyFormat), configuredDomains, configuredDomains.getLegacyDomain(), controller) : null;
        exprHelper = configuredDomains.getExprDomain() != null ?
                new ModelControllerMBeanHelper(TypeConverters.createExpressionTypeConverters(), configuredDomains, configuredDomains.getExprDomain(), controller) : null;
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
        throw ModelControllerMBeanHelper.createInstanceNotFoundException(loaderName);
    }

    public ClassLoader getClassLoaderFor(ObjectName mbeanName) throws InstanceNotFoundException {
        if (getHelper(mbeanName).resolvePathAddress(mbeanName) != null) {
            return SecurityActions.getClassLoader(this.getClass());
        }
        throw ModelControllerMBeanHelper.createInstanceNotFoundException(mbeanName);
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

    public void addNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback)
            throws InstanceNotFoundException {
        //TODO handle notifications in model?
    }

    public void addNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback)
            throws InstanceNotFoundException {
        //TODO handle notifications in model?
    }

    public void removeNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, ListenerNotFoundException {
        //TODO handle notifications in model?
    }

    public void removeNotificationListener(ObjectName name, NotificationListener listener) throws InstanceNotFoundException,
            ListenerNotFoundException {
        //TODO handle notifications in model?
    }

    public void removeNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, ListenerNotFoundException {
        //TODO handle notifications in model?
    }

    public void removeNotificationListener(ObjectName name, ObjectName listener) throws InstanceNotFoundException, ListenerNotFoundException {
        //TODO handle notifications in model?
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
}

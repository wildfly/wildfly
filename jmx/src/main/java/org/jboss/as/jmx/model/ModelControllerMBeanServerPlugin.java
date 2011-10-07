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

import java.util.Set;

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

    private final ModelControllerMBeanHelper helper;

    public ModelControllerMBeanServerPlugin(ModelController controller) {
        helper = new ModelControllerMBeanHelper(controller);
    }

    @Override
    public boolean accepts(ObjectName objectName) {
        return ObjectNameAddressUtil.isReservedDomain(objectName);
    }

    public Object getAttribute(ObjectName name, String attribute) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException,
            ReflectionException {
        return helper.getAttribute(name, attribute);
    }

    public AttributeList getAttributes(ObjectName name, String[] attributes) throws InstanceNotFoundException, ReflectionException {
        return helper.getAttributes(name, attributes);
    }

    public ClassLoader getClassLoader(ObjectName loaderName) throws InstanceNotFoundException {
        if (helper.resolvePathAddress(loaderName) != null) {
            return SecurityActions.getClassLoader(this.getClass());
        }
        throw ModelControllerMBeanHelper.createInstanceNotFoundException(loaderName);
    }

    public ClassLoader getClassLoaderFor(ObjectName mbeanName) throws InstanceNotFoundException {
        if (helper.resolvePathAddress(mbeanName) != null) {
            return SecurityActions.getClassLoader(this.getClass());
        }
        throw ModelControllerMBeanHelper.createInstanceNotFoundException(mbeanName);
    }

    public String[] getDomains() {
        return new String[] {Constants.DOMAIN};
    }

    public Integer getMBeanCount() {
        return helper.getMBeanCount();
    }

    public MBeanInfo getMBeanInfo(ObjectName name) throws InstanceNotFoundException, IntrospectionException, ReflectionException {
        return helper.getMBeanInfo(name);
    }

    public ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException {
        return helper.getObjectInstance(name);
    }

    public Object invoke(ObjectName name, String operationName, Object[] params, String[] signature) throws InstanceNotFoundException,
            MBeanException, ReflectionException {
        return helper.invoke(name, operationName, params, signature);
    }

    public boolean isInstanceOf(ObjectName name, String className) throws InstanceNotFoundException {
        return false;
    }

    public boolean isRegistered(ObjectName name) {
        return helper.resolvePathAddress(name) != null;
    }

    public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) {
        return helper.queryMBeans(name, query);
    }

    public Set<ObjectName> queryNames(ObjectName name, QueryExp query) {
        return helper.queryNames(name, query);
    }

    public void setAttribute(ObjectName name, Attribute attribute) throws InstanceNotFoundException, AttributeNotFoundException,
            InvalidAttributeValueException, MBeanException, ReflectionException {
        helper.setAttribute(name, attribute);
    }

    public AttributeList setAttributes(ObjectName name, AttributeList attributes) throws InstanceNotFoundException, ReflectionException {
        return helper.setAttributes(name, attributes);
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
}

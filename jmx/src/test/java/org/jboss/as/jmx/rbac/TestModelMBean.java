/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.jmx.rbac;

import javax.management.Attribute;
import javax.management.AttributeChangeNotification;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ReflectionException;
import javax.management.RuntimeOperationsException;
import javax.management.modelmbean.InvalidTargetObjectTypeException;
import javax.management.modelmbean.ModelMBean;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanConstructorInfo;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.ModelMBeanInfoSupport;
import javax.management.modelmbean.ModelMBeanNotificationInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class TestModelMBean implements ModelMBean {

    public TestModelMBean() {

    }

    @Override
    public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
        throw new AttributeNotFoundException();
    }

    @Override
    public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException,
            MBeanException, ReflectionException {
        throw new AttributeNotFoundException();
    }

    @Override
    public AttributeList getAttributes(String[] attributes) {
        return new AttributeList();
    }

    @Override
    public AttributeList setAttributes(AttributeList attributes) {
        return null;
    }

    @Override
    public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException {
        return null;
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        try {
            ModelMBeanAttributeInfo[] attributes = new ModelMBeanAttributeInfo[0];
            ModelMBeanConstructorInfo[] constructors = new ModelMBeanConstructorInfo[] {
                    new ModelMBeanConstructorInfo("-", this.getClass().getConstructor())
            };
            ModelMBeanOperationInfo[] operations = new ModelMBeanOperationInfo[] {
                    new ModelMBeanOperationInfo("info", "-", new MBeanParameterInfo[0], Void.class.getName(), ModelMBeanOperationInfo.INFO),
                    new ModelMBeanOperationInfo("action", "-", new MBeanParameterInfo[0], Void.class.getName(), ModelMBeanOperationInfo.ACTION),
                    new ModelMBeanOperationInfo("actionInfo", "-", new MBeanParameterInfo[0], Void.class.getName(), ModelMBeanOperationInfo.ACTION_INFO),
                    new ModelMBeanOperationInfo("unknown", "-", new MBeanParameterInfo[0], Void.class.getName(), ModelMBeanOperationInfo.UNKNOWN)
            };
            ModelMBeanNotificationInfo[] notifications = new ModelMBeanNotificationInfo[0];
            return new ModelMBeanInfoSupport(this.getClass().getName(), "-", attributes, constructors, operations, notifications);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void load() throws MBeanException, RuntimeOperationsException, InstanceNotFoundException {
    }

    @Override
    public void store() throws MBeanException, RuntimeOperationsException, InstanceNotFoundException {
    }

    @Override
    public void sendNotification(Notification ntfyObj) throws MBeanException, RuntimeOperationsException {
    }

    @Override
    public void sendNotification(String ntfyText) throws MBeanException, RuntimeOperationsException {
    }

    @Override
    public void sendAttributeChangeNotification(AttributeChangeNotification notification) throws MBeanException,
            RuntimeOperationsException {
    }

    @Override
    public void sendAttributeChangeNotification(Attribute oldValue, Attribute newValue) throws MBeanException,
            RuntimeOperationsException {
    }

    @Override
    public void addAttributeChangeNotificationListener(NotificationListener listener, String attributeName, Object handback)
            throws MBeanException, RuntimeOperationsException, IllegalArgumentException {
    }

    @Override
    public void removeAttributeChangeNotificationListener(NotificationListener listener, String attributeName)
            throws MBeanException, RuntimeOperationsException, ListenerNotFoundException {
    }

    @Override
    public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback)
            throws IllegalArgumentException {
    }

    @Override
    public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
    }

    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {
        return null;
    }

    @Override
    public void setModelMBeanInfo(ModelMBeanInfo inModelMBeanInfo) throws MBeanException, RuntimeOperationsException {
    }

    @Override
    public void setManagedResource(Object mr, String mr_type) throws MBeanException, RuntimeOperationsException,
            InstanceNotFoundException, InvalidTargetObjectTypeException {
    }
}

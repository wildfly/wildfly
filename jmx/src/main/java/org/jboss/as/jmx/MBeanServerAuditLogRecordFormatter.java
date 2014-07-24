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
package org.jboss.as.jmx;

import static org.jboss.as.jmx.MBeanServerSignature.ADD_NOTIFICATION_LISTENER;
import static org.jboss.as.jmx.MBeanServerSignature.ADD_NOTIFICATION_LISTENER_SIG_1;
import static org.jboss.as.jmx.MBeanServerSignature.ADD_NOTIFICATION_LISTENER_SIG_2;
import static org.jboss.as.jmx.MBeanServerSignature.CREATE_MBEAN;
import static org.jboss.as.jmx.MBeanServerSignature.CREATE_MBEAN_SIG_1;
import static org.jboss.as.jmx.MBeanServerSignature.CREATE_MBEAN_SIG_2;
import static org.jboss.as.jmx.MBeanServerSignature.CREATE_MBEAN_SIG_3;
import static org.jboss.as.jmx.MBeanServerSignature.CREATE_MBEAN_SIG_4;
import static org.jboss.as.jmx.MBeanServerSignature.DESERIALIZE;
import static org.jboss.as.jmx.MBeanServerSignature.DESERIALIZE_SIG1;
import static org.jboss.as.jmx.MBeanServerSignature.DESERIALIZE_SIG2;
import static org.jboss.as.jmx.MBeanServerSignature.DESERIALIZE_SIG3;
import static org.jboss.as.jmx.MBeanServerSignature.GET_ATTRIBUTE;
import static org.jboss.as.jmx.MBeanServerSignature.GET_ATTRIBUTES;
import static org.jboss.as.jmx.MBeanServerSignature.GET_ATTRIBUTES_SIG;
import static org.jboss.as.jmx.MBeanServerSignature.GET_ATTRIBUTE_SIG;
import static org.jboss.as.jmx.MBeanServerSignature.GET_CLASSLOADER;
import static org.jboss.as.jmx.MBeanServerSignature.GET_CLASSLOADER_FOR;
import static org.jboss.as.jmx.MBeanServerSignature.GET_CLASSLOADER_FOR_SIG;
import static org.jboss.as.jmx.MBeanServerSignature.GET_CLASSLOADER_REPOSITORY;
import static org.jboss.as.jmx.MBeanServerSignature.GET_CLASSLOADER_REPOSITORY_SIG;
import static org.jboss.as.jmx.MBeanServerSignature.GET_CLASSLOADER_SIG;
import static org.jboss.as.jmx.MBeanServerSignature.GET_DEFAULT_DOMAIN;
import static org.jboss.as.jmx.MBeanServerSignature.GET_DEFAULT_DOMAIN_SIG;
import static org.jboss.as.jmx.MBeanServerSignature.GET_DOMAINS;
import static org.jboss.as.jmx.MBeanServerSignature.GET_DOMAINS_SIG;
import static org.jboss.as.jmx.MBeanServerSignature.GET_MBEAN_COUNT;
import static org.jboss.as.jmx.MBeanServerSignature.GET_MBEAN_COUNT_SIG;
import static org.jboss.as.jmx.MBeanServerSignature.GET_MBEAN_INFO;
import static org.jboss.as.jmx.MBeanServerSignature.GET_MBEAN_INFO_SIG;
import static org.jboss.as.jmx.MBeanServerSignature.GET_OBJECT_INSTANCE;
import static org.jboss.as.jmx.MBeanServerSignature.GET_OBJECT_INSTANCE_SIG;
import static org.jboss.as.jmx.MBeanServerSignature.INSTANTIATE;
import static org.jboss.as.jmx.MBeanServerSignature.INSTANTIATE_SIG1;
import static org.jboss.as.jmx.MBeanServerSignature.INSTANTIATE_SIG2;
import static org.jboss.as.jmx.MBeanServerSignature.INSTANTIATE_SIG3;
import static org.jboss.as.jmx.MBeanServerSignature.INSTANTIATE_SIG4;
import static org.jboss.as.jmx.MBeanServerSignature.INVOKE;
import static org.jboss.as.jmx.MBeanServerSignature.INVOKE_SIG;
import static org.jboss.as.jmx.MBeanServerSignature.IS_INSTANCE_OF;
import static org.jboss.as.jmx.MBeanServerSignature.IS_INSTANCE_OF_SIG;
import static org.jboss.as.jmx.MBeanServerSignature.IS_REGISTERED;
import static org.jboss.as.jmx.MBeanServerSignature.IS_REGISTERED_SIG;
import static org.jboss.as.jmx.MBeanServerSignature.QUERY_MBEANS;
import static org.jboss.as.jmx.MBeanServerSignature.QUERY_MBEANS_SIG;
import static org.jboss.as.jmx.MBeanServerSignature.QUERY_NAMES;
import static org.jboss.as.jmx.MBeanServerSignature.QUERY_NAMES_SIG;
import static org.jboss.as.jmx.MBeanServerSignature.REGISTER_MBEAN;
import static org.jboss.as.jmx.MBeanServerSignature.REGISTER_MBEAN_SIG;
import static org.jboss.as.jmx.MBeanServerSignature.REMOVE_NOTIFICATION_LISTENER;
import static org.jboss.as.jmx.MBeanServerSignature.REMOVE_NOTIFICATION_LISTENER_SIG_1;
import static org.jboss.as.jmx.MBeanServerSignature.REMOVE_NOTIFICATION_LISTENER_SIG_2;
import static org.jboss.as.jmx.MBeanServerSignature.REMOVE_NOTIFICATION_LISTENER_SIG_3;
import static org.jboss.as.jmx.MBeanServerSignature.REMOVE_NOTIFICATION_LISTENER_SIG_4;
import static org.jboss.as.jmx.MBeanServerSignature.SET_ATTRIBUTE;
import static org.jboss.as.jmx.MBeanServerSignature.SET_ATTRIBUTES;
import static org.jboss.as.jmx.MBeanServerSignature.SET_ATTRIBUTES_SIG;
import static org.jboss.as.jmx.MBeanServerSignature.SET_ATTRIBUTE_SIG;
import static org.jboss.as.jmx.MBeanServerSignature.UNREGISTER_MBEAN;
import static org.jboss.as.jmx.MBeanServerSignature.UNREGISTER_MBEAN_SIG;

import java.io.ObjectInputStream;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.loading.ClassLoaderRepository;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class MBeanServerAuditLogRecordFormatter implements MBeanServer {

    private final PluggableMBeanServerImpl pluggableMBeanServerImpl;
    private final Throwable error;
    private final boolean readOnly;

    public MBeanServerAuditLogRecordFormatter(PluggableMBeanServerImpl pluggableMBeanServerImpl, Throwable error, boolean readOnly) {
        this.pluggableMBeanServerImpl = pluggableMBeanServerImpl;
        this.error = error;
        this.readOnly = readOnly;
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name) throws ReflectionException,
            InstanceAlreadyExistsException, MBeanException, NotCompliantMBeanException {
        log(readOnly, CREATE_MBEAN, CREATE_MBEAN_SIG_1, className, name);
        return null;
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName) {
        log(readOnly, CREATE_MBEAN, CREATE_MBEAN_SIG_2, className, name, loaderName);
        return null;
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, Object[] params, String[] signature) {
        log(readOnly, CREATE_MBEAN, CREATE_MBEAN_SIG_3, className, name, params, signature);
        return null;
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName, Object[] params,
            String[] signature) {
        log(readOnly, CREATE_MBEAN, CREATE_MBEAN_SIG_4, className, name, loaderName, params, signature);
        return null;
    }

    @Override
    public ObjectInstance registerMBean(Object object, ObjectName name) {
        log(readOnly, REGISTER_MBEAN, REGISTER_MBEAN_SIG, object, name);
        return null;
    }

    @Override
    public void unregisterMBean(ObjectName name) {
        log(readOnly, UNREGISTER_MBEAN, UNREGISTER_MBEAN_SIG, name);
    }

    @Override
    public ObjectInstance getObjectInstance(ObjectName name) {
        log(readOnly, GET_OBJECT_INSTANCE, GET_OBJECT_INSTANCE_SIG, name);
        return null;
    }

    @Override
    public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) {
        log(readOnly, QUERY_MBEANS, QUERY_MBEANS_SIG, name, query);
        return null;
    }

    @Override
    public Set<ObjectName> queryNames(ObjectName name, QueryExp query) {
        log(readOnly, QUERY_NAMES, QUERY_NAMES_SIG, name, query);
        return null;
    }

    @Override
    public boolean isRegistered(ObjectName name) {
        log(readOnly, IS_REGISTERED, IS_REGISTERED_SIG, name);
        return false;
    }

    @Override
    public Integer getMBeanCount() {
        log(readOnly, GET_MBEAN_COUNT, GET_MBEAN_COUNT_SIG);
        return null;
    }

    @Override
    public Object getAttribute(ObjectName name, String attribute) {
        log(readOnly, GET_ATTRIBUTE, GET_ATTRIBUTE_SIG, name, attribute);
        return null;
    }

    @Override
    public AttributeList getAttributes(ObjectName name, String[] attributes) {
        log(readOnly, GET_ATTRIBUTES, GET_ATTRIBUTES_SIG, name, attributes);
        return null;
    }

    @Override
    public void setAttribute(ObjectName name, Attribute attribute) {
        log(readOnly, SET_ATTRIBUTE, SET_ATTRIBUTE_SIG, name, attribute);
    }

    @Override
    public AttributeList setAttributes(ObjectName name, AttributeList attributes) {
        log(readOnly, SET_ATTRIBUTES, SET_ATTRIBUTES_SIG, name, attributes);
        return null;
    }

    @Override
    public Object invoke(ObjectName name, String operationName, Object[] params, String[] signature) {
        log(readOnly, INVOKE, INVOKE_SIG, name, operationName, params, signature);
        return null;
    }

    @Override
    public String getDefaultDomain() {
        log(readOnly, GET_DEFAULT_DOMAIN, GET_DEFAULT_DOMAIN_SIG);
        return null;
    }

    @Override
    public String[] getDomains() {
        log(readOnly, GET_DOMAINS, GET_DOMAINS_SIG);
        return null;
    }

    @Override
    public void addNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) {
        log(readOnly, ADD_NOTIFICATION_LISTENER, ADD_NOTIFICATION_LISTENER_SIG_1, name, listener, filter, handback);
    }

    @Override
    public void addNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) {
        log(readOnly, ADD_NOTIFICATION_LISTENER, ADD_NOTIFICATION_LISTENER_SIG_2, name, listener, filter, handback);
    }

    @Override
    public void removeNotificationListener(ObjectName name, ObjectName listener) {
        log(readOnly, REMOVE_NOTIFICATION_LISTENER, REMOVE_NOTIFICATION_LISTENER_SIG_1, name, listener);
    }

    @Override
    public void removeNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) {
        log(readOnly, REMOVE_NOTIFICATION_LISTENER, REMOVE_NOTIFICATION_LISTENER_SIG_2, name, listener, filter, handback);
    }

    @Override
    public void removeNotificationListener(ObjectName name, NotificationListener listener) {
        log(readOnly, REMOVE_NOTIFICATION_LISTENER, REMOVE_NOTIFICATION_LISTENER_SIG_3, name, listener);
    }

    @Override
    public void removeNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter,
            Object handback) {
        log(readOnly, REMOVE_NOTIFICATION_LISTENER, REMOVE_NOTIFICATION_LISTENER_SIG_4, name, listener, filter, handback);
    }

    @Override
    public MBeanInfo getMBeanInfo(ObjectName name) {
        log(readOnly, GET_MBEAN_INFO, GET_MBEAN_INFO_SIG, name);
        return null;
    }

    @Override
    public boolean isInstanceOf(ObjectName name, String className) {
        log(readOnly, IS_INSTANCE_OF, IS_INSTANCE_OF_SIG, name, className);
        return false;
    }

    @Override
    public Object instantiate(String className) {
        log(readOnly, INSTANTIATE, INSTANTIATE_SIG1, className);
        return null;
    }

    @Override
    public Object instantiate(String className, ObjectName loaderName) {
        log(true, INSTANTIATE, INSTANTIATE_SIG2, className, loaderName);
        return null;
    }

    @Override
    public Object instantiate(String className, Object[] params, String[] signature) {
        log(readOnly, INSTANTIATE, INSTANTIATE_SIG3, className, params, signature);
        return null;
    }

    @Override
    public Object instantiate(String className, ObjectName loaderName, Object[] params, String[] signature) {
        log(readOnly, INSTANTIATE, INSTANTIATE_SIG4, className, loaderName, params, signature);
        return null;
    }

    @Override
    public ObjectInputStream deserialize(ObjectName name, byte[] data) {
        log(readOnly, DESERIALIZE, DESERIALIZE_SIG1, name, data);
        return null;
    }

    @Override
    public ObjectInputStream deserialize(String className, byte[] data) {
        log(readOnly, DESERIALIZE, DESERIALIZE_SIG2, className, data);
        return null;
    }

    @Override
    public ObjectInputStream deserialize(String className, ObjectName loaderName, byte[] data) {
        log(readOnly, DESERIALIZE, DESERIALIZE_SIG3, className, loaderName, data);
        return null;
    }

    @Override
    public ClassLoader getClassLoaderFor(ObjectName mbeanName) {
        log(readOnly, GET_CLASSLOADER_FOR, GET_CLASSLOADER_FOR_SIG, mbeanName);
        return null;
    }

    @Override
    public ClassLoader getClassLoader(ObjectName loaderName) {
        log(readOnly, GET_CLASSLOADER, GET_CLASSLOADER_SIG, loaderName);
        return null;
    }

    @Override
    public ClassLoaderRepository getClassLoaderRepository() {
        log(readOnly, GET_CLASSLOADER_REPOSITORY, GET_CLASSLOADER_REPOSITORY_SIG);
        return null;
    }

    private void log(boolean readOnly, String methodName, String[] methodSignature, Object...methodParams) {
        pluggableMBeanServerImpl.log(readOnly, error, methodName, methodSignature, methodParams);
    }
}

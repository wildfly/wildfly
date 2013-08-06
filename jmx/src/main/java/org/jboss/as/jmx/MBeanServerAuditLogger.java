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

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.loading.ClassLoaderRepository;
import javax.security.auth.Subject;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.Set;

import org.jboss.as.controller.security.AccessMechanismPrincipal;
import org.jboss.as.controller.security.InetAddressPrincipal;
import org.jboss.as.core.security.AccessMechanism;
import org.jboss.as.core.security.RealmUser;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class MBeanServerAuditLogger implements MBeanServer {

    private static final String[] NO_ARGS_SIG = new String[] {ObjectName.class.getName()};
    private static final String[] OBJECT_NAME_ONLY_SIG = new String[] {ObjectName.class.getName()};

    private static final String CREATE_MBEAN = "createMBean";
    private static final String[] CREATE_MBEAN_SIG_1 = new String[] {String.class.getName(), ObjectName.class.getName()};
    private static final String[] CREATE_MBEAN_SIG_2 = new String[] {String.class.getName(), ObjectName.class.getName(), ObjectName.class.getName()};
    private static final String[] CREATE_MBEAN_SIG_3 = new String[] {String.class.getName(), Object[].class.getName(), String[].class.getName()};
    private static final String[] CREATE_MBEAN_SIG_4 = new String[] {String.class.getName(), ObjectName.class.getName(), ObjectName.class.getName(), Object[].class.getName(), String[].class.getName()};

    private static final String REGISTER_MBEAN = "registerMBean";
    private static final String[] REGISTER_MBEAN_SIG = new String[] {Object.class.getName(), ObjectName.class.getName()};


    private static final String UNREGISTER_MBEAN = "unregisterMBean";
    private static final String[] UNREGISTER_MBEAN_SIG = OBJECT_NAME_ONLY_SIG;

    private static final String GET_OBJECT_INSTANCE = "getObjectInstance";
    private static final String[] GET_OBJECT_INSTANCE_SIG = OBJECT_NAME_ONLY_SIG;

    private static final String QUERY_MBEANS = "queryMBeans";
    private static final String[] QUERY_MBEANS_SIG = new String[] {ObjectName.class.getName(), QueryExp.class.getName()};

    private static final String QUERY_NAMES = "queryMBeans";
    private static final String[] QUERY_NAMES_SIG = QUERY_MBEANS_SIG;

    private static final String IS_REGISTERED = "isRegistered";
    private static final String[] IS_REGISTERED_SIG = OBJECT_NAME_ONLY_SIG;

    private static final String GET_MBEAN_COUNT = "getMBeanCount";
    private static final String[] GET_MBEAN_COUNT_SIG = NO_ARGS_SIG;

    private static final String GET_ATTRIBUTE = "getAttribute";
    private static final String[] GET_ATTRIBUTE_SIG = new String[] {ObjectName.class.getName(), String.class.getName()};

    private static final String GET_ATTRIBUTES = "getAttribute";
    private static final String[] GET_ATTRIBUTES_SIG = new String[] {ObjectName.class.getName(), String[].class.getName()};

    private static final String SET_ATTRIBUTE = "setAttribute";
    private static final String[] SET_ATTRIBUTE_SIG = new String[] {ObjectName.class.getName(), Attribute.class.getName()};

    private static final String SET_ATTRIBUTES = "setAttribute";
    private static final String[] SET_ATTRIBUTES_SIG = new String[] {ObjectName.class.getName(), AttributeList.class.getName()};

    private static final String INVOKE = "invoke";
    private static final String[] INVOKE_SIG = new String[] {ObjectName.class.getName(), String.class.getName(), Object[].class.getName(), String[].class.getName()};

    private static final String GET_DEFAULT_DOMAIN = "getDefaultDomain";
    private static final String[] GET_DEFAULT_DOMAIN_SIG = NO_ARGS_SIG;

    private static final String GET_DOMAINS = "getDomains";
    private static final String[] GET_DOMAINS_SIG = NO_ARGS_SIG;

    private static final String ADD_NOTIFICATION_LISTENER  = "addNotificationListener";
    private static final String[] ADD_NOTIFICATION_LISTENER_SIG_1 = new String[] {ObjectName.class.getName(), NotificationListener.class.getName(), NotificationFilter.class.getName(), Object.class.getName()};
    private static final String[] ADD_NOTIFICATION_LISTENER_SIG_2 = new String[] {ObjectName.class.getName(), ObjectName.class.getName(), NotificationFilter.class.getName(), Object.class.getName()};

    private static final String REMOVE_NOTIFICATION_LISTENER  = "addNotificationListener";
    private static final String[] REMOVE_NOTIFICATION_LISTENER_SIG_1 = new String[] {ObjectName.class.getName(), ObjectName.class.getName()};
    private static final String[] REMOVE_NOTIFICATION_LISTENER_SIG_2 = new String[] {ObjectName.class.getName(), ObjectName.class.getName(), NotificationFilter.class.getName(), Object.class.getName()};
    private static final String[] REMOVE_NOTIFICATION_LISTENER_SIG_3 = new String[] {ObjectName.class.getName(), NotificationListener.class.getName()};
    private static final String[] REMOVE_NOTIFICATION_LISTENER_SIG_4 = new String[] {ObjectName.class.getName(), NotificationListener.class.getName(), NotificationFilter.class.getName(), Object.class.getName()};

    private static final String GET_MBEAN_INFO = "getMBeanInfo";
    private static final String[] GET_MBEAN_INFO_SIG = OBJECT_NAME_ONLY_SIG;

    private static final String IS_INSTANCE_OF = "isInstanceOf";
    private static final String[] IS_INSTANCE_OF_SIG = new String[] {ObjectName.class.getName(), String.class.getName()};

    private static final String INSTANTIATE = "instantiate";
    private static final String[] INSTANTIATE_SIG1 = new String[] {String.class.getName()};
    private static final String[] INSTANTIATE_SIG2 = new String[] {String.class.getName(), ObjectName.class.getName()};
    private static final String[] INSTANTIATE_SIG3 = new String[] {String.class.getName(), Object[].class.getName(), String[].class.getName()};
    private static final String[] INSTANTIATE_SIG4 = new String[] {String.class.getName(), ObjectName.class.getName(), Object[].class.getName(), String[].class.getName()};

    private static final String DESERIALIZE = "deserialize";
    private static final String[] DESERIALIZE_SIG1 = new String[] {ObjectName.class.getName(), byte[].class.getName()};
    private static final String[] DESERIALIZE_SIG2 = new String[] {String.class.getName(), byte[].class.getName()};
    private static final String[] DESERIALIZE_SIG3 = new String[] {String.class.getName(), ObjectName.class.getName(), byte[].class.getName()};

    private static final String GET_CLASSLOADER_FOR = "getClassLoaderFor";
    private static final String[] GET_CLASSLOADER_FOR_SIG = OBJECT_NAME_ONLY_SIG;

    private static final String GET_CLASSLOADER = "getClassLoader";
    private static final String[] GET_CLASSLOADER_SIG = OBJECT_NAME_ONLY_SIG;

    private static final String GET_CLASSLOADER_REPOSITORY = "getClassLoaderRepository";
    private static final String[] GET_CLASSLOADER_REPOSITORY_SIG = NO_ARGS_SIG;


    private final PluggableMBeanServerImpl pluggableMBeanServerImpl;
    private final Throwable error;

    public MBeanServerAuditLogger(PluggableMBeanServerImpl pluggableMBeanServerImpl, Throwable error) {
        this.pluggableMBeanServerImpl = pluggableMBeanServerImpl;
        this.error = error;
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name) throws ReflectionException,
            InstanceAlreadyExistsException, MBeanException, NotCompliantMBeanException {
        final boolean readOnly = false;
        if (shouldLog(readOnly)) {
            log(readOnly, CREATE_MBEAN, CREATE_MBEAN_SIG_1, className, name);
        }
        return null;
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName) {
        final boolean readOnly = false;
        if (shouldLog(readOnly)) {
            log(readOnly, CREATE_MBEAN, CREATE_MBEAN_SIG_2, className, name, loaderName);
        }
        return null;
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, Object[] params, String[] signature) {
        final boolean readOnly = false;
        if (shouldLog(readOnly)) {
            log(readOnly, CREATE_MBEAN, CREATE_MBEAN_SIG_3, className, name, params, signature);
        }
        return null;
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName, Object[] params,
            String[] signature) {
        final boolean readOnly = false;
        if (shouldLog(readOnly)) {
            log(readOnly, CREATE_MBEAN, CREATE_MBEAN_SIG_4, className, name, loaderName, params, signature);
        }
        return null;
    }

    @Override
    public ObjectInstance registerMBean(Object object, ObjectName name) {
        final boolean readOnly = false;
        if (shouldLog(readOnly)) {
            log(readOnly, REGISTER_MBEAN, REGISTER_MBEAN_SIG, object, name);
        }
        return null;
    }

    @Override
    public void unregisterMBean(ObjectName name) {
        final boolean readOnly = false;
        if (shouldLog(readOnly)) {
            log(readOnly, UNREGISTER_MBEAN, UNREGISTER_MBEAN_SIG, name);
        }
    }

    @Override
    public ObjectInstance getObjectInstance(ObjectName name) {
        final boolean readOnly = true;
        if (shouldLog(readOnly)) {
            log(readOnly, GET_OBJECT_INSTANCE, GET_OBJECT_INSTANCE_SIG, name);
        }
        return null;
    }

    @Override
    public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) {
        final boolean readOnly = true;
        if (shouldLog(readOnly)) {
            log(readOnly, QUERY_MBEANS, QUERY_MBEANS_SIG, name, query);
        }
        return null;
    }

    @Override
    public Set<ObjectName> queryNames(ObjectName name, QueryExp query) {
        final boolean readOnly = true;
        if (shouldLog(readOnly)) {
            log(readOnly, QUERY_NAMES, QUERY_NAMES_SIG, name, query);
        }
        return null;
    }

    @Override
    public boolean isRegistered(ObjectName name) {
        final boolean readOnly = true;
        if (shouldLog(readOnly)) {
            log(readOnly, IS_REGISTERED, IS_REGISTERED_SIG, name);
        }
        return false;
    }

    @Override
    public Integer getMBeanCount() {
        final boolean readOnly = true;
        if (shouldLog(readOnly)) {
            log(readOnly, GET_MBEAN_COUNT, GET_MBEAN_COUNT_SIG);
        }
        return null;
    }

    @Override
    public Object getAttribute(ObjectName name, String attribute) {
        final boolean readOnly = true;
        if (shouldLog(readOnly)) {
            log(readOnly, GET_ATTRIBUTE, GET_ATTRIBUTE_SIG, name, attribute);
        }
        return null;
    }

    @Override
    public AttributeList getAttributes(ObjectName name, String[] attributes) {
        final boolean readOnly = true;
        if (shouldLog(readOnly)) {
            log(readOnly, GET_ATTRIBUTES, GET_ATTRIBUTES_SIG, name, attributes);
        }
        return null;
    }

    @Override
    public void setAttribute(ObjectName name, Attribute attribute) {
        final boolean readOnly = false;
        if (shouldLog(readOnly)) {
            log(readOnly, SET_ATTRIBUTE, SET_ATTRIBUTE_SIG, name, attribute);
        }
    }

    @Override
    public AttributeList setAttributes(ObjectName name, AttributeList attributes) {
        final boolean readOnly = false;
        if (shouldLog(readOnly)) {
            log(readOnly, SET_ATTRIBUTES, SET_ATTRIBUTES_SIG, name, attributes);
        }
        return null;
    }

    @Override
    public Object invoke(ObjectName name, String operationName, Object[] params, String[] signature) {
        final boolean readOnly = isOperationReadOnly(name, operationName, signature);
        if (shouldLog(readOnly)) {
            log(readOnly, INVOKE, INVOKE_SIG, name, operationName, params, signature);
        }
        return null;
    }

    @Override
    public String getDefaultDomain() {
        final boolean readOnly = true;
        if (shouldLog(readOnly)) {
            log(readOnly, GET_DEFAULT_DOMAIN, GET_DEFAULT_DOMAIN_SIG);
        }
        return null;
    }

    @Override
    public String[] getDomains() {
        final boolean readOnly = true;
        if (shouldLog(readOnly)) {
            log(readOnly, GET_DOMAINS, GET_DOMAINS_SIG);
        }
        return null;
    }

    @Override
    public void addNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) {
        final boolean readOnly = false;
        if (shouldLog(readOnly)) {
            log(readOnly, ADD_NOTIFICATION_LISTENER, ADD_NOTIFICATION_LISTENER_SIG_1, name, listener, filter, handback);
        }
    }

    @Override
    public void addNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) {
        final boolean readOnly = false;
        if (shouldLog(readOnly)) {
            log(readOnly, ADD_NOTIFICATION_LISTENER, ADD_NOTIFICATION_LISTENER_SIG_2, name, listener, filter, handback);
        }
    }

    @Override
    public void removeNotificationListener(ObjectName name, ObjectName listener) {
        final boolean readOnly = false;
        if (shouldLog(readOnly)) {
            log(readOnly, REMOVE_NOTIFICATION_LISTENER, REMOVE_NOTIFICATION_LISTENER_SIG_1, name, listener);
        }
    }

    @Override
    public void removeNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) {
        final boolean readOnly = false;
        if (shouldLog(readOnly)) {
            log(readOnly, REMOVE_NOTIFICATION_LISTENER, REMOVE_NOTIFICATION_LISTENER_SIG_2, name, listener, filter, handback);
        }
    }

    @Override
    public void removeNotificationListener(ObjectName name, NotificationListener listener) {
        final boolean readOnly = false;
        if (shouldLog(readOnly)) {
            log(readOnly, REMOVE_NOTIFICATION_LISTENER, REMOVE_NOTIFICATION_LISTENER_SIG_3, name, listener);
        }
    }

    @Override
    public void removeNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter,
            Object handback) {
        final boolean readOnly = false;
        if (shouldLog(readOnly)) {
            log(readOnly, REMOVE_NOTIFICATION_LISTENER, REMOVE_NOTIFICATION_LISTENER_SIG_4, name, listener, filter, handback);
        }
    }

    @Override
    public MBeanInfo getMBeanInfo(ObjectName name) {
        final boolean readOnly = true;
        if (shouldLog(readOnly)) {
            log(readOnly, GET_MBEAN_INFO, GET_MBEAN_INFO_SIG, name);
        }
        return null;
    }

    @Override
    public boolean isInstanceOf(ObjectName name, String className) {
        final boolean readOnly = true;
        if (shouldLog(readOnly)) {
            log(readOnly, IS_INSTANCE_OF, IS_INSTANCE_OF_SIG, name, className);
        }
        return false;
    }

    @Override
    public Object instantiate(String className) {
        final boolean readOnly = true;
        if (shouldLog(readOnly)) {
            log(readOnly, INSTANTIATE, INSTANTIATE_SIG1, className);
        }
        return null;
    }

    @Override
    public Object instantiate(String className, ObjectName loaderName) {
        final boolean readOnly = true;
        if (shouldLog(readOnly)) {
            log(true, INSTANTIATE, INSTANTIATE_SIG2, className, loaderName);
        }
        return null;
    }

    @Override
    public Object instantiate(String className, Object[] params, String[] signature) {
        final boolean readOnly = true;
        if (shouldLog(readOnly)) {
            log(readOnly, INSTANTIATE, INSTANTIATE_SIG3, className, params, signature);
        }
        return null;
    }

    @Override
    public Object instantiate(String className, ObjectName loaderName, Object[] params, String[] signature) {
        final boolean readOnly = true;
        if (shouldLog(readOnly)) {
            log(readOnly, INSTANTIATE, INSTANTIATE_SIG4, className, loaderName, params, signature);
        }
        return null;
    }

    @Override
    public ObjectInputStream deserialize(ObjectName name, byte[] data) {
        final boolean readOnly = true;
        if (shouldLog(readOnly)) {
            log(readOnly, DESERIALIZE, DESERIALIZE_SIG1, name, data);
        }
        return null;
    }

    @Override
    public ObjectInputStream deserialize(String className, byte[] data) {
        final boolean readOnly = true;
        if (shouldLog(readOnly)) {
            log(readOnly, DESERIALIZE, DESERIALIZE_SIG2, className, data);
        }
        return null;
    }

    @Override
    public ObjectInputStream deserialize(String className, ObjectName loaderName, byte[] data) {
        final boolean readOnly = true;
        if (shouldLog(readOnly)) {
            log(readOnly, DESERIALIZE, DESERIALIZE_SIG3, className, loaderName, data);
        }
        return null;
    }

    @Override
    public ClassLoader getClassLoaderFor(ObjectName mbeanName) {
        final boolean readOnly = true;
        if (shouldLog(readOnly)) {
            log(readOnly, GET_CLASSLOADER_FOR, GET_CLASSLOADER_FOR_SIG, mbeanName);
        }
        return null;
    }

    @Override
    public ClassLoader getClassLoader(ObjectName loaderName) {
        final boolean readOnly = true;
        if (shouldLog(readOnly)) {
            log(readOnly, GET_CLASSLOADER, GET_CLASSLOADER_SIG, loaderName);
        }
        return null;
    }

    @Override
    public ClassLoaderRepository getClassLoaderRepository() {
        final boolean readOnly = true;
        if (shouldLog(readOnly)) {
            log(readOnly, GET_CLASSLOADER_REPOSITORY, GET_CLASSLOADER_REPOSITORY_SIG);
        }
        return null;
    }

    private boolean shouldLog(boolean readOnly) {
        //TODO
        return true;
    }

    private boolean isOperationReadOnly(ObjectName name, String operationName, String[] signature) {
        MBeanInfo info;
        try {
            info = pluggableMBeanServerImpl.getMBeanInfo(name, false, true);
        } catch (Exception e) {
            //This should not happen, just in case say it is not RO
            return false;
        }
        if (info == null) {
            //Default to not RO
            return false;
        }
        for (MBeanOperationInfo op : info.getOperations()) {
            if (op.getName().equals(operationName)) {
                MBeanParameterInfo[] params = op.getSignature();
                if (params.length != signature.length) {
                    continue;
                }
                boolean same = true;
                for (int i = 0 ; i < params.length ; i++) {
                    if (!params[i].getType().equals(signature[i])) {
                        same = false;
                        break;
                    }
                }
                if (same) {
                    return op.getImpact() == MBeanOperationInfo.INFO;
                }
            }
        }
        //Default to not RO
        return false;
    }

    private void log(boolean readOnly, String methodName, String[] methodSignature, Object...methodParams) {
        String domainUUID = null; //TODO

        PrivilegedAction<Subject> getCurrentAction = new PrivilegedAction<Subject>() {
            @Override
            public Subject run() {
                return Subject.getSubject(AccessController.getContext());
            }
        };
        Subject subject = System.getSecurityManager() == null ?
                getCurrentAction.run() : AccessController.doPrivileged(getCurrentAction);

        pluggableMBeanServerImpl.getAuditLogger().logMethodAccess(
                readOnly,
                pluggableMBeanServerImpl.isBooting(),
                getCallerUserId(subject),
                domainUUID,
                getSubjectAccessMechanism(subject),
                getSubjectInetAddress(subject),
                methodName,
                methodSignature,
                methodParams,
                error);
    }

    private String getCallerUserId(Subject subject) {
        String userId = null;
        if (subject != null) {
            Set<RealmUser> realmUsers = subject.getPrincipals(RealmUser.class);
            RealmUser user = realmUsers.iterator().next();
            userId = user.getName();
        }
        return userId;
    }

    private InetAddress getSubjectInetAddress(Subject subject) {
        InetAddressPrincipal principal = getPrincipal(subject, InetAddressPrincipal.class);
        return principal != null ? principal.getInetAddress() : null;
    }

    private AccessMechanism getSubjectAccessMechanism(Subject subject) {
        AccessMechanismPrincipal principal = getPrincipal(subject, AccessMechanismPrincipal.class);
        return principal != null ? principal.getAccessMechanism() : null;
    }

    private <T extends Principal> T getPrincipal(Subject subject, Class<T> clazz) {
        if (subject == null) {
            return null;
        }
        Set<T> principals = subject.getPrincipals(clazz);
        assert principals.size() <= 1;
        if (principals.size() == 0) {
            return null;
        }
        return principals.iterator().next();
    }


}

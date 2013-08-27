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
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.QueryExp;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class MBeanServerSignature {
    static final String[] NO_ARGS_SIG = new String[] {ObjectName.class.getName()};
    static final String[] OBJECT_NAME_ONLY_SIG = new String[] {ObjectName.class.getName()};

    static final String CREATE_MBEAN = "createMBean";
    static final String[] CREATE_MBEAN_SIG_1 = new String[] {String.class.getName(), ObjectName.class.getName()};
    static final String[] CREATE_MBEAN_SIG_2 = new String[] {String.class.getName(), ObjectName.class.getName(), ObjectName.class.getName()};
    static final String[] CREATE_MBEAN_SIG_3 = new String[] {String.class.getName(), Object[].class.getName(), String[].class.getName()};
    static final String[] CREATE_MBEAN_SIG_4 = new String[] {String.class.getName(), ObjectName.class.getName(), ObjectName.class.getName(), Object[].class.getName(), String[].class.getName()};

    static final String REGISTER_MBEAN = "registerMBean";
    static final String[] REGISTER_MBEAN_SIG = new String[] {Object.class.getName(), ObjectName.class.getName()};


    static final String UNREGISTER_MBEAN = "unregisterMBean";
    static final String[] UNREGISTER_MBEAN_SIG = OBJECT_NAME_ONLY_SIG;

    static final String GET_OBJECT_INSTANCE = "getObjectInstance";
    static final String[] GET_OBJECT_INSTANCE_SIG = OBJECT_NAME_ONLY_SIG;

    static final String QUERY_MBEANS = "queryMBeans";
    static final String[] QUERY_MBEANS_SIG = new String[] {ObjectName.class.getName(), QueryExp.class.getName()};

    static final String QUERY_NAMES = "queryMBeans";
    static final String[] QUERY_NAMES_SIG = QUERY_MBEANS_SIG;

    static final String IS_REGISTERED = "isRegistered";
    static final String[] IS_REGISTERED_SIG = OBJECT_NAME_ONLY_SIG;

    static final String GET_MBEAN_COUNT = "getMBeanCount";
    static final String[] GET_MBEAN_COUNT_SIG = NO_ARGS_SIG;

    static final String GET_ATTRIBUTE = "getAttribute";
    static final String[] GET_ATTRIBUTE_SIG = new String[] {ObjectName.class.getName(), String.class.getName()};

    static final String GET_ATTRIBUTES = "getAttribute";
    static final String[] GET_ATTRIBUTES_SIG = new String[] {ObjectName.class.getName(), String[].class.getName()};

    static final String SET_ATTRIBUTE = "setAttribute";
    static final String[] SET_ATTRIBUTE_SIG = new String[] {ObjectName.class.getName(), Attribute.class.getName()};

    static final String SET_ATTRIBUTES = "setAttribute";
    static final String[] SET_ATTRIBUTES_SIG = new String[] {ObjectName.class.getName(), AttributeList.class.getName()};

    static final String INVOKE = "invoke";
    static final String[] INVOKE_SIG = new String[] {ObjectName.class.getName(), String.class.getName(), Object[].class.getName(), String[].class.getName()};

    static final String GET_DEFAULT_DOMAIN = "getDefaultDomain";
    static final String[] GET_DEFAULT_DOMAIN_SIG = NO_ARGS_SIG;

    static final String GET_DOMAINS = "getDomains";
    static final String[] GET_DOMAINS_SIG = NO_ARGS_SIG;

    static final String ADD_NOTIFICATION_LISTENER  = "addNotificationListener";
    static final String[] ADD_NOTIFICATION_LISTENER_SIG_1 = new String[] {ObjectName.class.getName(), NotificationListener.class.getName(), NotificationFilter.class.getName(), Object.class.getName()};
    static final String[] ADD_NOTIFICATION_LISTENER_SIG_2 = new String[] {ObjectName.class.getName(), ObjectName.class.getName(), NotificationFilter.class.getName(), Object.class.getName()};

    static final String REMOVE_NOTIFICATION_LISTENER  = "addNotificationListener";
    static final String[] REMOVE_NOTIFICATION_LISTENER_SIG_1 = new String[] {ObjectName.class.getName(), ObjectName.class.getName()};
    static final String[] REMOVE_NOTIFICATION_LISTENER_SIG_2 = new String[] {ObjectName.class.getName(), ObjectName.class.getName(), NotificationFilter.class.getName(), Object.class.getName()};
    static final String[] REMOVE_NOTIFICATION_LISTENER_SIG_3 = new String[] {ObjectName.class.getName(), NotificationListener.class.getName()};
    static final String[] REMOVE_NOTIFICATION_LISTENER_SIG_4 = new String[] {ObjectName.class.getName(), NotificationListener.class.getName(), NotificationFilter.class.getName(), Object.class.getName()};

    static final String GET_MBEAN_INFO = "getMBeanInfo";
    static final String[] GET_MBEAN_INFO_SIG = OBJECT_NAME_ONLY_SIG;

    static final String IS_INSTANCE_OF = "isInstanceOf";
    static final String[] IS_INSTANCE_OF_SIG = new String[] {ObjectName.class.getName(), String.class.getName()};

    static final String INSTANTIATE = "instantiate";
    static final String[] INSTANTIATE_SIG1 = new String[] {String.class.getName()};
    static final String[] INSTANTIATE_SIG2 = new String[] {String.class.getName(), ObjectName.class.getName()};
    static final String[] INSTANTIATE_SIG3 = new String[] {String.class.getName(), Object[].class.getName(), String[].class.getName()};
    static final String[] INSTANTIATE_SIG4 = new String[] {String.class.getName(), ObjectName.class.getName(), Object[].class.getName(), String[].class.getName()};

    static final String DESERIALIZE = "deserialize";
    static final String[] DESERIALIZE_SIG1 = new String[] {ObjectName.class.getName(), byte[].class.getName()};
    static final String[] DESERIALIZE_SIG2 = new String[] {String.class.getName(), byte[].class.getName()};
    static final String[] DESERIALIZE_SIG3 = new String[] {String.class.getName(), ObjectName.class.getName(), byte[].class.getName()};

    static final String GET_CLASSLOADER_FOR = "getClassLoaderFor";
    static final String[] GET_CLASSLOADER_FOR_SIG = OBJECT_NAME_ONLY_SIG;

    static final String GET_CLASSLOADER = "getClassLoader";
    static final String[] GET_CLASSLOADER_SIG = OBJECT_NAME_ONLY_SIG;

    static final String GET_CLASSLOADER_REPOSITORY = "getClassLoaderRepository";
    static final String[] GET_CLASSLOADER_REPOSITORY_SIG = NO_ARGS_SIG;
}

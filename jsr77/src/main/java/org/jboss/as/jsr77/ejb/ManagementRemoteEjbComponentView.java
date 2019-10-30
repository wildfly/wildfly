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
package org.jboss.as.jsr77.ejb;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.ejb.EJBObject;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.j2ee.Management;

import org.jboss.as.ee.utils.DescriptorUtils;
import org.jboss.as.jsr77.logging.JSR77Logger;
import org.jboss.invocation.InterceptorContext;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ManagementRemoteEjbComponentView extends BaseManagementEjbComponentView {

    private final MBeanServer server;

    private volatile Method queryNames;
    private volatile Method isRegistered;
    private volatile Method getMBeanCount;
    private volatile Method getMBeanInfo;
    private volatile Method getAttribute;
    private volatile Method getAttributes;
    private volatile Method setAttribute;
    private volatile Method setAttributes;
    private volatile Method invoke;
    private volatile Method getDefaultDomain;
    private volatile Method getListenerRegistry;

    private volatile Method remove;

    public ManagementRemoteEjbComponentView(MBeanServer server) {
        this.server = server;
    }


    @Override
    public Object invoke(InterceptorContext interceptorContext) throws Exception {
        final Method method = interceptorContext.getMethod();
        final Object[] params = interceptorContext.getParameters();
        if(WildFlySecurityManager.isChecking()) {
            try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws Exception {
                    return invokeInternal(method, params);
                }
            });
            } catch (PrivilegedActionException e) {
                Throwable cause = e.getCause();
                if (cause != null) {
                    if(cause instanceof Exception) {
                        throw (Exception)cause;
                    } else {
                        throw new RuntimeException(e);
                    }
                } else {
                    throw e;
                }
            }
        } else {
            return invokeInternal(method, params);
        }
    }

    private Object invokeInternal(Method method, Object[] params) throws InstanceNotFoundException, IntrospectionException, ReflectionException, MBeanException, AttributeNotFoundException, InvalidAttributeValueException {
        if (method == queryNames) {
            return server.queryNames(
                    getParameter(ObjectName.class, params, 0),
                    getParameter(QueryExp.class, params, 1));
        } else if (method == isRegistered) {
            return server.isRegistered(getParameter(ObjectName.class, params, 0));
        } else if (method == getMBeanCount) {
            return server.getMBeanCount();
        } else if (method == getMBeanInfo) {
            return server.getMBeanInfo(getParameter(ObjectName.class, params, 0));
        } else if (method == getAttribute) {
            return server.getAttribute(
                    getParameter(ObjectName.class, params, 0),
                    getParameter(String.class, params, 1));
        } else if (method == getAttributes) {
            return server.getAttributes(
                    getParameter(ObjectName.class, params, 0),
                    getParameter(String[].class, params, 1));
        } else if (method == setAttribute) {
            server.setAttribute(
                    getParameter(ObjectName.class, params, 0),
                    getParameter(Attribute.class, params, 1));
        } else if (method == setAttributes) {
            return server.setAttributes(
                    getParameter(ObjectName.class, params, 0),
                    getParameter(AttributeList.class, params, 1));

        } else if (method == invoke) {
            return server.invoke(
                    getParameter(ObjectName.class, params, 0),
                    getParameter(String.class, params, 1),
                    getParameter(Object[].class, params, 2),
                    getParameter(String[].class, params, 3));
        } else if (method == getDefaultDomain) {
            return server.getDefaultDomain();
        } else if (method == getListenerRegistry) {
            //TODO read spec ;-) and find out what this should do
            throw JSR77Logger.ROOT_LOGGER.notYetImplemented();
        } else if (method == remove) {
            return null;
        }
        throw JSR77Logger.ROOT_LOGGER.unknownMethod(method);
    }

    @Override
    Map<String, Map<String, Method>> initMethods() {

        try {

            queryNames = Management.class.getMethod("queryNames", ObjectName.class, QueryExp.class);
            isRegistered = Management.class.getMethod("isRegistered", ObjectName.class);
            getMBeanCount = Management.class.getMethod("getMBeanCount");
            getMBeanInfo = Management.class.getMethod("getMBeanInfo", ObjectName.class);
            getAttribute = Management.class.getMethod("getAttribute", ObjectName.class, String.class);
            getAttributes = Management.class.getMethod("getAttributes", ObjectName.class, String[].class);
            setAttribute = Management.class.getMethod("setAttribute", ObjectName.class, Attribute.class);
            setAttributes = Management.class.getMethod("setAttributes", ObjectName.class, AttributeList.class);
            invoke = Management.class.getMethod("invoke", ObjectName.class, String.class, Object[].class, String[].class);
            getDefaultDomain = Management.class.getMethod("getDefaultDomain");
            getListenerRegistry = Management.class.getMethod("getListenerRegistry");

            remove = EJBObject.class.getMethod("remove");
            //TODO rest of the EjbObject methods
        } catch (Exception e) {
            throw new RuntimeException();
        }
        Map<String, Map<String, Method>> map = new HashMap<String, Map<String,Method>>();
        addMethod(map, queryNames);
        addMethod(map, isRegistered);
        addMethod(map, getMBeanCount);
        addMethod(map, getMBeanInfo);
        addMethod(map, getAttribute);
        addMethod(map, getAttributes);
        addMethod(map, setAttribute);
        addMethod(map, setAttributes);
        addMethod(map, invoke);
        addMethod(map, getDefaultDomain);
        addMethod(map, getListenerRegistry);

        addMethod(map, remove);
        //TODO rest of the EjbObject methods

        return map;
    }

    private void addMethod(Map<String, Map<String, Method>> map, Method m) {
        map.put(m.getName(), Collections.singletonMap(DescriptorUtils.methodDescriptor(m), m));
    }

    private <T> T getParameter(Class<T> clazz, Object[] params, int index) {
        if (index >= params.length) {
            throw JSR77Logger.ROOT_LOGGER.wrongParamLength(index, params.length);
        }
        Object o = params[index];
        if (o == null) {
            return null;
        }
        try {
            return clazz.cast(o);
        } catch (Exception e) {
            throw JSR77Logger.ROOT_LOGGER.wrongParamType(index, clazz.getName(), o.getClass().getName());
        }
    }


    @Override
    public <T> T getPrivateData(Class<T> clazz) {
        return null;
    }
}

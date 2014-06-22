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
package org.jboss.as.jsr77.managedobject;

import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Set;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.ObjectName;
import javax.management.QueryExp;

import org.jboss.as.controller.interfaces.InetAddressUtil;
import org.jboss.as.jsr77.logging.JSR77Logger;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class JVMHandler extends BaseHandler {

    static final JVMHandler INSTANCE = new JVMHandler();

    private static final String ATTR_JVM_NAME = "javaVersion";
    private static final String ATTR_JAVA_VENDOR = "javaVendor";
    private static final String ATTR_NODE = "node";

    static final String J2EE_TYPE = "JVM";
    static final String DEFAULT_JVM_TYPE = "default";
    private final String objectName;

    private JVMHandler() {
        this.objectName = ObjectNameBuilder.createServerChild(J2EE_TYPE, DEFAULT_JVM_TYPE)
            .toString();
    }

    String getObjectName() {
        return objectName;
    }

    @Override
    Set<ObjectName> queryObjectNames(ModelReader reader, ObjectName name, QueryExp query) {
        return Collections.singleton(ObjectNameBuilder.createObjectName(getObjectName()));
    }

    protected Object getAttribute(ModelReader reader, ObjectName name, String attribute) throws AttributeNotFoundException {
        if (attribute.equals(ATTR_JVM_NAME)) {
            return WildFlySecurityManager.getPropertyPrivileged("java.version", null);
        } else if (attribute.equals(ATTR_JAVA_VENDOR)) {
            return WildFlySecurityManager.getPropertyPrivileged("java.vendor", null);
        } else if (attribute.equals(ATTR_NODE)) {
            try {
                return InetAddressUtil.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }
        return super.getAttribute(reader, name, attribute);
    }

    @Override
    Set<MBeanAttributeInfo> getAttributeInfos() {
        Set<MBeanAttributeInfo> attributes = super.getAttributeInfos();

        attributes.add(createRoMBeanAttributeInfo(ATTR_JVM_NAME, String[].class.getName(), JSR77Logger.ROOT_LOGGER.attrInfoJvmName()));
        attributes.add(createRoMBeanAttributeInfo(ATTR_JAVA_VENDOR, String.class.getName(), JSR77Logger.ROOT_LOGGER.attrInfoJavaVendor()));
        attributes.add(createRoMBeanAttributeInfo(ATTR_NODE, String.class.getName(), JSR77Logger.ROOT_LOGGER.attrInfoNode()));

        return attributes;
    }

}

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

import java.util.Collections;
import java.util.Set;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.ObjectName;
import javax.management.QueryExp;

import org.jboss.as.jsr77.logging.JSR77Logger;
import org.jboss.as.version.Version;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class J2EEServerHandler extends BaseHandler {

    static final J2EEServerHandler INSTANCE = new J2EEServerHandler();

    private static final String ATTR_DEPLOYED_OBJECTS = "deployedObjects";
    private static final String ATTR_RESOURCES = "resources";
    private static final String ATTR_JAVA_VMS = "javaVMs";
    private static final String ATTR_SERVER_VENDOR = "serverVendor";
    private static final String ATTR_SERVER_VERSION = "serverVersion";

    static final String J2EE_TYPE = "J2EEServer";
    static final String DEFAULT_SERVER_TYPE = "default";

    private final String objectName;

    private J2EEServerHandler() {
        ObjectNameBuilder builder = ObjectNameBuilder.createPlain(J2EE_TYPE, DEFAULT_SERVER_TYPE);
        this.objectName = builder.toString();
    }

    @Override
    Set<ObjectName> queryObjectNames(ModelReader reader, ObjectName name, QueryExp query) {
        return Collections.singleton(ObjectNameBuilder.createObjectName(this.objectName));
    }

    String getObjectName() {
        return objectName;
    }

    protected Object getAttribute(ModelReader reader, ObjectName name, String attribute) throws AttributeNotFoundException {
        if (attribute.equals(ATTR_DEPLOYED_OBJECTS)) {
            //TODO Implement if parts of the TCK require this
            return new String[0];
        } else if (attribute.equals(ATTR_RESOURCES)) {
            //TODO Implement if parts of the TCK require this
            return new String[0];
        } else if (attribute.equals(ATTR_JAVA_VMS)) {
            return new String[] {JVMHandler.INSTANCE.getObjectName()};
        } else if (attribute.equals(ATTR_SERVER_VENDOR)) {
            return "JBoss";
        } else if (attribute.equals(ATTR_SERVER_VERSION)) {
            return Version.AS_VERSION;
        }
        return super.getAttribute(reader, name, attribute);
    }

    @Override
    Set<MBeanAttributeInfo> getAttributeInfos() {
        Set<MBeanAttributeInfo> attributes = super.getAttributeInfos();

        attributes.add(createRoMBeanAttributeInfo(ATTR_DEPLOYED_OBJECTS, String[].class.getName(), JSR77Logger.ROOT_LOGGER.attrInfoDeployedObjects()));
        attributes.add(createRoMBeanAttributeInfo(ATTR_RESOURCES, String[].class.getName(), JSR77Logger.ROOT_LOGGER.attrInfoResources()));
        attributes.add(createRoMBeanAttributeInfo(ATTR_JAVA_VMS, String[].class.getName(), JSR77Logger.ROOT_LOGGER.attrInfoJavaVms()));
        attributes.add(createRoMBeanAttributeInfo(ATTR_SERVER_VENDOR, String.class.getName(), JSR77Logger.ROOT_LOGGER.attrInfoServerVendor()));
        attributes.add(createRoMBeanAttributeInfo(ATTR_SERVER_VERSION, String.class.getName(), JSR77Logger.ROOT_LOGGER.attrInfoServerVersion()));

        return attributes;
    }

}

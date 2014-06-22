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

import java.util.HashSet;
import java.util.Set;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ObjectName;
import org.jboss.as.jsr77.logging.JSR77Logger;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
abstract class BaseHandler extends Handler{

    private static final String ATTR_NAME = "objectName";
    private static final String ATTR_STATE_MANAGEABLE = "stateManageable";
    private static final String ATTR_STATISTICS_PROVIDER = "statisticsProvider";
    private static final String ATTR_EVENT_PROVIDER = "eventProvider";

    protected BaseHandler() {
    }

    Object getAttribute(ModelReader reader, ObjectName name, String attribute) throws AttributeNotFoundException {
        if (attribute.equals(ATTR_NAME)) {
            return name.toString();
        } else if (attribute.equals(ATTR_STATE_MANAGEABLE) || attribute.equals(ATTR_STATISTICS_PROVIDER) || attribute.equals(ATTR_EVENT_PROVIDER)) {
            return false;
        }
        throw JSR77Logger.ROOT_LOGGER.noAttributeCalled(attribute);
    }

    @Override
    MBeanInfo getMBeanInfo(ModelReader reader, ObjectName name) throws InstanceNotFoundException {
        Set<ObjectName> names = queryObjectNames(reader, name, null);
        if (names.size() != 1) {
            throw JSR77Logger.ROOT_LOGGER.noMBeanCalled(name);
        }
        if (!name.apply(names.iterator().next())){
            throw JSR77Logger.ROOT_LOGGER.noMBeanCalled(name);
        }
        Set<MBeanAttributeInfo> attrs = getAttributeInfos();
        MBeanAttributeInfo[] attributes = attrs.toArray(new MBeanAttributeInfo[attrs.size()]);

        return new MBeanInfo(this.getClass().getName(),
                "Management Object",
                attributes,
                new MBeanConstructorInfo[0],
                new MBeanOperationInfo[0],
                new MBeanNotificationInfo[0]);
    }


    Set<MBeanAttributeInfo> getAttributeInfos() {
        Set<MBeanAttributeInfo> attributes = new HashSet<MBeanAttributeInfo>();
        attributes.add(createRoMBeanAttributeInfo(ATTR_NAME, String.class.getName(), JSR77Logger.ROOT_LOGGER.attrInfoAttrName()));
        attributes.add(createRoMBeanAttributeInfo(ATTR_STATE_MANAGEABLE, Boolean.TYPE.getName(), JSR77Logger.ROOT_LOGGER.attrInfoStateManageable()));
        attributes.add(createRoMBeanAttributeInfo(ATTR_STATISTICS_PROVIDER, Boolean.TYPE.getName(), JSR77Logger.ROOT_LOGGER.attrInfoStatisticsProvider()));
        attributes.add(createRoMBeanAttributeInfo(ATTR_EVENT_PROVIDER, Boolean.TYPE.getName(), JSR77Logger.ROOT_LOGGER.attrInfoEventProvider()));
        return attributes;
    }

    MBeanAttributeInfo createRoMBeanAttributeInfo(String name, String type, String description) {
        return new MBeanAttributeInfo(name, type, description, true, false, false);
    }

}
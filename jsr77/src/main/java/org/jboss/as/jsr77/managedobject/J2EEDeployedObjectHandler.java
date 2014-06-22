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

import java.util.Set;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.ObjectName;
import org.jboss.as.jsr77.logging.JSR77Logger;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class J2EEDeployedObjectHandler extends BaseHandler {

    private final String ATTR_DEPLOYMENT_DESCRIPTOR = "deploymentDescriptor";
    private final String ATTR_SERVER = "server";

    @Override
    Object getAttribute(ModelReader reader, ObjectName name, String attribute) throws AttributeNotFoundException {
        if (attribute.equals(ATTR_DEPLOYMENT_DESCRIPTOR)) {
            //TODO implement if TCK requires the DD
            return "";
        } else if (attribute.equals(ATTR_SERVER)) {
            return J2EEServerHandler.INSTANCE.getObjectName().toString();
        }
        return super.getAttribute(reader, name, attribute);
    }

    @Override
    Set<MBeanAttributeInfo> getAttributeInfos() {
        Set<MBeanAttributeInfo> attributes = super.getAttributeInfos();

        attributes.add(createRoMBeanAttributeInfo(ATTR_DEPLOYMENT_DESCRIPTOR, String.class.getName(), JSR77Logger.ROOT_LOGGER.attrInfoDeploymentDescriptor()));
        attributes.add(createRoMBeanAttributeInfo(ATTR_SERVER, String.class.getName(), JSR77Logger.ROOT_LOGGER.attrInfoServer()));

        return attributes;
    }

}

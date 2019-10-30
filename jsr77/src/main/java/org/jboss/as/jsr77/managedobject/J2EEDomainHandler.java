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
import org.jboss.as.jsr77.subsystem.Constants;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class J2EEDomainHandler extends BaseHandler {

    static final J2EEDomainHandler INSTANCE = new J2EEDomainHandler();

    private static final String ATTR_SERVERS = "servers";

    static final String J2EE_TYPE = "J2EEDomain";

    private final String objectName;

    private J2EEDomainHandler() {
        ObjectNameBuilder builder = ObjectNameBuilder.createPlain(J2EE_TYPE, Constants.JMX_DOMAIN);
        this.objectName = builder.toString();
    }

    @Override
    Set<ObjectName> queryObjectNames(ModelReader reader, ObjectName name, QueryExp query) {
        return Collections.singleton(ObjectNameBuilder.createObjectName(objectName));
    }

    protected Object getAttribute(ModelReader reader, ObjectName name, String attribute) throws AttributeNotFoundException {
        if (attribute.equals(ATTR_SERVERS)) {
            return new String[] {J2EEServerHandler.INSTANCE.getObjectName()};
        }
        return super.getAttribute(reader, name, attribute);
    }

    @Override
    Set<MBeanAttributeInfo> getAttributeInfos() {
        Set<MBeanAttributeInfo> attributes = super.getAttributeInfos();

        attributes.add(createRoMBeanAttributeInfo(ATTR_SERVERS, String.class.getName(), JSR77Logger.ROOT_LOGGER.attrInfoServers()));

        return attributes;
    }

}

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

import javax.management.ObjectName;

import org.jboss.as.jsr77.logging.JSR77Logger;
import org.jboss.as.jsr77.subsystem.Constants;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ObjectNameBuilder {

    final StringBuilder sb;

    private ObjectNameBuilder(String j2eeType, String name) {
        sb = new StringBuilder(Constants.JMX_DOMAIN + ":" + Handler.J2EE_TYPE + "=" + j2eeType + ",name=" + name);
    }

    static ObjectNameBuilder createPlain(String j2eeType, String name) {
        return new ObjectNameBuilder(j2eeType, name);
    }

    static ObjectNameBuilder createServerChild(String j2eeType, String name) {
        return new ObjectNameBuilder(j2eeType, name).append(J2EEServerHandler.J2EE_TYPE, J2EEServerHandler.DEFAULT_SERVER_TYPE);
    }

    ObjectNameBuilder append(String key, String value) {
        sb.append("," + key + "=" + value);
        return this;
    }

    public String toString() {
        return sb.toString();
    }

    ObjectName toObjectName() {
        try {
            return new ObjectName(toString());
        } catch (Exception e) {
            throw JSR77Logger.ROOT_LOGGER.invalidObjectName(e, toString());
        }
    }

    static ObjectName createObjectName(String name) {
        try {
            return new ObjectName(name);
        } catch (Exception e) {
            throw JSR77Logger.ROOT_LOGGER.couldNotCreateObjectName(e, name);
        }
    }

}

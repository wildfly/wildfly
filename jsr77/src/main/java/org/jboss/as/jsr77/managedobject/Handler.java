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
import javax.management.InstanceNotFoundException;
import javax.management.MBeanInfo;
import javax.management.ObjectName;
import javax.management.QueryExp;

import org.jboss.as.jsr77.subsystem.Constants;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
abstract class Handler {

    static final String JMX_DOMAIN = Constants.JMX_DOMAIN;
    static final String J2EE_TYPE = "j2eeType";
    static final String NAME = "name";

    abstract Set<ObjectName> queryObjectNames(ModelReader reader, ObjectName name, QueryExp query);
    abstract Object getAttribute(ModelReader reader, ObjectName name, String attribute) throws AttributeNotFoundException, InstanceNotFoundException;
    abstract MBeanInfo getMBeanInfo(ModelReader reader, ObjectName name) throws InstanceNotFoundException;
}

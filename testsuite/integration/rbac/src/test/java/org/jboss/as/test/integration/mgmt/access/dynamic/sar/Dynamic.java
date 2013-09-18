/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.mgmt.access.dynamic.sar;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ReflectionException;

/**
 * @author Ladislav Thon <lthon@redhat.com>
 */
public class Dynamic implements DynamicMBean {
    @Override
    public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
        return null; // not used in the test
    }

    @Override
    public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        // not used in the test
    }

    @Override
    public AttributeList getAttributes(String[] attributes) {
        return null; // not used in the test
    }

    @Override
    public AttributeList setAttributes(AttributeList attributes) {
        return null; // not used in the test
    }

    @Override
    public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException {
        return null; // implementation not important for the test purposes
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        return new MBeanInfo(
                Dynamic.class.getName(), "MBean for RBAC testing of JMX non-core MBeans", null, null,
                new MBeanOperationInfo[] {
                        new MBeanOperationInfo("helloReadOnly", "helloReadOnly", null, "void", MBeanOperationInfo.INFO),
                        new MBeanOperationInfo("helloWriteOnly", "helloWriteOnly", null, "void", MBeanOperationInfo.ACTION),
                        new MBeanOperationInfo("helloReadWrite", "helloReadWrite", null, "void", MBeanOperationInfo.ACTION_INFO),
                        new MBeanOperationInfo("helloUnknown", "helloUnknown", null, "void", MBeanOperationInfo.UNKNOWN)
                },
                null, null
        );
    }
}

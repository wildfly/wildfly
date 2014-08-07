/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jsr77.logging;

import java.lang.reflect.Method;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * @author Kabir Khan
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@MessageLogger(projectCode = "WFLYEEMGMT", length = 4)
public interface JSR77Logger extends BasicLogger {

    /**
     * A logger with a category of the package name.
     */
    JSR77Logger ROOT_LOGGER = Logger.getMessageLogger(JSR77Logger.class, "org.jboss.as.jsr77");

    @Message(id = Message.NONE, value = "The object name")
    String attrInfoAttrName();

    @Message(id = Message.NONE, value = "Whether this managed object is state manageable")
    String attrInfoStateManageable();

    @Message(id = Message.NONE, value = "Whether this managed object is a statistics provider")
    String attrInfoStatisticsProvider();

    @Message(id = Message.NONE, value = "Whether this managed object is an event provider")
    String attrInfoEventProvider();

    @Message(id = Message.NONE, value = "The deployment descriptor")
    String attrInfoDeploymentDescriptor();

    @Message(id = Message.NONE, value = "The server object name descriptor")
    String attrInfoServer();

    @Message(id = Message.NONE, value = "The servers")
    String attrInfoServers();

    @Message(id = Message.NONE, value = "The deployed objects")
    String attrInfoDeployedObjects();

    @Message(id = Message.NONE, value = "The resources")
    String attrInfoResources();

    @Message(id = Message.NONE, value = "The java vms")
    String attrInfoJavaVms();

    @Message(id = Message.NONE, value = "The server vendor")
    String attrInfoServerVendor();

    @Message(id = Message.NONE, value = "The server version")
    String attrInfoServerVersion();

    @Message(id = Message.NONE, value = "The jvm name")
    String attrInfoJvmName();

    @Message(id = Message.NONE, value = "The java vendor")
    String attrInfoJavaVendor();

    @Message(id = Message.NONE, value = "The node")
    String attrInfoNode();

    @Message(id = 1, value = "Only required in local view")
    UnsupportedOperationException onlyRequiredInLocalView();

    @Message(id = 2, value = "Expected at least %d elements in parameter array with size %d")
    IllegalArgumentException wrongParamLength(int index, int length);

    @Message(id = 3, value = "Bad type for parameter at %d. Expected %s, but was %s")
    IllegalArgumentException wrongParamType(int index, String expected, String actual);

    @Message(id = 4, value = "No attribute called %s")
    AttributeNotFoundException noAttributeCalled(String attribute);

    @Message(id = 5, value = "No mbean found called %s")
    InstanceNotFoundException noMBeanCalled(ObjectName name);

    //@Message(id = 6, value = "Should not get called")
    //IllegalStateException shouldNotGetCalled();

    @Message(id = 7, value = "Could not find %s")
    InstanceNotFoundException couldNotFindJ2eeType(String j2eeType);

    @Message(id = 8, value = "Invalid ObjectName: %s")
    IllegalStateException invalidObjectName(@Cause Throwable t, String s);

    @Message(id = 9, value = "Could not create ObjectName: %s")
    IllegalStateException couldNotCreateObjectName(@Cause Throwable t, String s);

    @Message(id = 10, value = "%s is read-only")
    IllegalStateException mbeanIsReadOnly(ObjectName on);

    @Message(id = 11, value = "Not yet implemented")
    IllegalStateException notYetImplemented();

    @Message(id = 12, value = "Unknown method: %s")
    IllegalArgumentException unknownMethod(Method m);

}

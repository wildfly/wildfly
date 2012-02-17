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

package org.jboss.as.platform.mbean;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;

/**
 * This module is using message IDs in the range 12300-12399. This file is using the subset 12350-12399 for
 * non-logger messages. See http://community.jboss.org/docs/DOC-16810 for the full list of currently reserved
 * JBAS message id blocks.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
@MessageBundle(projectCode = "JBAS")
public interface PlatformMBeanMessages {

    /**
     * The messages.
     */
    PlatformMBeanMessages MESSAGES = Messages.getBundle(PlatformMBeanMessages.class);

    /**
     * Creates an exception indicating that an operation parameter attribute name is unknown
     *
     * @param attributeName the name of the attribute
     *
     * @return the {@link OperationFailedException}
     */
    @Message(id = 12300, value = "No known attribute %s")
    OperationFailedException unknownAttribute(String attributeName);

    @Message(id = 12301, value = "A platform mbean resource does not have a writable model")
    UnsupportedOperationException modelNotWritable();

    @Message(id = 12302, value = "Adding child resources is not supported")
    UnsupportedOperationException addingChildrenNotSupported();

    @Message(id = 12303, value = "Removing child resources is not supported")
    UnsupportedOperationException removingChildrenNotSupported();

    @Message(id = 12304, value = "No BufferPoolMXBean with name %s currently exists")
    OperationFailedException unknownBufferPool(String poolName);

    @Message(id = 12305, value = "Read support for attribute %s was not properly implemented")
    IllegalStateException badReadAttributeImpl1(String attributeName);
    @Message(id = 12306, value = "Read support for attribute %s was not properly implemented")
    IllegalStateException badReadAttributeImpl2(String attributeName);
    @Message(id = 12307, value = "Read support for attribute %s was not properly implemented")
    IllegalStateException badReadAttributeImpl3(String attributeName);
    @Message(id = 12308, value = "Read support for attribute %s was not properly implemented")
    IllegalStateException badReadAttributeImpl4(String attributeName);
    @Message(id = 12309, value = "Read support for attribute %s was not properly implemented")
    IllegalStateException badReadAttributeImpl5(String attributeName);
    @Message(id = 12310, value = "Read support for attribute %s was not properly implemented")
    IllegalStateException badReadAttributeImpl6(String attributeName);
    @Message(id = 12311, value = "Read support for attribute %s was not properly implemented")
    IllegalStateException badReadAttributeImpl7(String attributeName);
    @Message(id = 12312, value = "Read support for attribute %s was not properly implemented")
    IllegalStateException badReadAttributeImpl8(String attributeName);
    @Message(id = 12313, value = "Read support for attribute %s was not properly implemented")
    IllegalStateException badReadAttributeImpl9(String attributeName);
    @Message(id = 12314, value = "Read support for attribute %s was not properly implemented")
    IllegalStateException badReadAttributeImpl10(String attributeName);
    @Message(id = 12315, value = "Read support for attribute %s was not properly implemented")
    IllegalStateException badReadAttributeImpl11(String attributeName);

    @Message(id = 12316, value = "Write support for attribute %s was not properly implemented")
    IllegalStateException badWriteAttributeImpl1(String attributeName);
    @Message(id = 12317, value = "Write support for attribute %s was not properly implemented")
    IllegalStateException badWriteAttributeImpl2(String attributeName);
    @Message(id = 12318, value = "Write support for attribute %s was not properly implemented")
    IllegalStateException badWriteAttributeImpl3(String attributeName);
    @Message(id = 12319, value = "Write support for attribute %s was not properly implemented")
    IllegalStateException badWriteAttributeImpl4(String attributeName);

    @Message(id = 12320, value = "No GarbageCollectorMXBean with name %s currently exists")
    OperationFailedException unknownGarbageCollector(String gcName);

    @Message(id = 12321, value = "No MemoryManagerMXBean with name %s currently exists")
    OperationFailedException unknownMemoryManager(String mmName);

    @Message(id = 12322, value = "No MemoryPoolMXBean with name %s currently exists")
    OperationFailedException unknownMemoryPool1(String mmName);
    @Message(id = 12323, value = "No MemoryPoolMXBean with name %s currently exists")
    OperationFailedException unknownMemoryPool2(String mmName);
}

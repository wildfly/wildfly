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

package org.jboss.as.platform.mbean.logging;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
@MessageLogger(projectCode = "WFLYPMB", length = 4)
public interface PlatformMBeanLogger extends BasicLogger {

    /**
     * A logger with the category of the package.
     */
    PlatformMBeanLogger ROOT_LOGGER = Logger.getMessageLogger(PlatformMBeanLogger.class, "org.jboss.as.platform.mbean");

    /**
     * Creates an exception indicating that an operation parameter attribute name is unknown
     *
     * @param attributeName the name of the attribute
     *
     * @return the {@link OperationFailedException}
     */
    @Message(id = 1, value = "No known attribute %s")
    OperationFailedException unknownAttribute(String attributeName);

    @Message(id = 2, value = "A platform mbean resource does not have a writable model")
    UnsupportedOperationException modelNotWritable();

    @Message(id = 3, value = "Adding child resources is not supported")
    UnsupportedOperationException addingChildrenNotSupported();

    @Message(id = 4, value = "Removing child resources is not supported")
    UnsupportedOperationException removingChildrenNotSupported();

    @Message(id = 5, value = "No BufferPoolMXBean with name %s currently exists")
    OperationFailedException unknownBufferPool(String poolName);

    @Message(id = 6, value = "Read support for attribute %s was not properly implemented")
    IllegalStateException badReadAttributeImpl(String attributeName);

    @Message(id = 7, value = "Write support for attribute %s was not properly implemented")
    IllegalStateException badWriteAttributeImpl(String attributeName);

    @Message(id = 8, value = "No GarbageCollectorMXBean with name %s currently exists")
    OperationFailedException unknownGarbageCollector(String gcName);

    @Message(id = 9, value = "No MemoryManagerMXBean with name %s currently exists")
    OperationFailedException unknownMemoryManager(String mmName);

    @Message(id = 10, value = "No MemoryPoolMXBean with name %s currently exists")
    OperationFailedException unknownMemoryPool(String mmName);
}

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

package org.jboss.as.threads;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;
import org.jboss.logging.Param;
import org.jboss.msc.service.ServiceName;

/**
 * This module is using message IDs in the range 12400-12499. This file is using the subset 12450-12499 for
 * non-logger messages. See http://community.jboss.org/docs/DOC-16810 for the full list of currently reserved
 * JBAS message id blocks.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBAS")
interface ThreadsMessages {

    /**
     * The messages
     */
    ThreadsMessages MESSAGES = Messages.getBundle(ThreadsMessages.class);

    @Message(id = 12450, value = "Unsupported attribute '%s'")
    IllegalStateException unsupportedBoundedQueueThreadPoolMetric(String attributeName);

    @Message(id = 12451, value = "Unsupported attribute '%s'")
    IllegalStateException unsupportedBoundedQueueThreadPoolAttribute(String attributeName);

    @Message(id = 12452, value = "Unsupported attribute '%s'")
    IllegalStateException unsupportedQueuelessThreadPoolMetric(String attributeName);

    @Message(id = 12453, value = "Unsupported attribute '%s'")
    IllegalStateException unsupportedQueuelessThreadPoolAttribute(String attributeName);

    @Message(id = 12454, value = "Unsupported attribute '%s'")
    IllegalStateException unsupportedScheduledThreadPoolMetric(String attributeName);

    @Message(id = 12455, value = "Unsupported attribute '%s'")
    IllegalStateException unsupportedScheduledThreadPoolAttribute(String attributeName);

    @Message(id = 12456, value = "Unsupported attribute '%s'")
    IllegalStateException unsupportedThreadFactoryAttribute(String attributeName);

    @Message(id = 12457, value = "Unsupported attribute '%s'")
    IllegalStateException unsupportedUnboundedQueueThreadPoolMetric(String attributeName);

    @Message(id = 12458, value = "Unsupported attribute '%s'")
    IllegalStateException unsupportedUnboundedQueueThreadPoolAttribute(String attributeName);

    @Message(id = 12459, value = "The executor service hasn't been initialized.")
    IllegalStateException boundedQueueThreadPoolExecutorUninitialized();

    @Message(id = 12460, value = "The executor service hasn't been initialized.")
    IllegalStateException queuelessThreadPoolExecutorUninitialized();

    @Message(id = 12461, value = "The executor service hasn't been initialized.")
    IllegalStateException scheduledThreadPoolExecutorUninitialized();

    @Message(id = 12462, value = "The thread factory service hasn't been initialized.")
    IllegalStateException threadFactoryUninitialized();

    @Message(id = 12463, value = "The executor service hasn't been initialized.")
    IllegalStateException unboundedQueueThreadPoolExecutorUninitialized();

    @Message(id = 12464, value = "Service '%s' not found.")
    OperationFailedException boundedQueueThreadPoolServiceNotFound(ServiceName serviceName);

    @Message(id = 12465, value = "Service '%s' not found.")
    OperationFailedException queuelessThreadPoolServiceNotFound(ServiceName serviceName);

    @Message(id = 12466, value = "Service '%s' not found.")
    OperationFailedException scheduledThreadPoolServiceNotFound(ServiceName serviceName);

    @Message(id = 12467, value = "Service '%s' not found.")
    OperationFailedException threadFactoryServiceNotFound(ServiceName serviceName);

    @Message(id = 12468, value = "Service '%s' not found.")
    OperationFailedException unboundedQueueThreadPoolServiceNotFound(ServiceName serviceName);

    @Message(id = 12469, value = "Failed to locate executor service '%s'")
    OperationFailedException threadPoolServiceNotFoundForMetrics(ServiceName serviceName);

    @Message(id = 12470, value = "Attribute %s expects values of type %s but got %s of type %s")
    OperationFailedException invalidKeepAliveType(String parameterName, ModelType objectType, ModelNode value, ModelType valueType);

    @Message(id = 12471, value = "Attribute %s expects values consisting of '%s' and '%s' but the new value consists of %s")
    OperationFailedException invalidKeepAliveKeys(String parameterName, String time, String unit, Set<String> keys);

    @Message(id = 12472, value = "Missing '%s' for parameter '%s'")
    OperationFailedException missingKeepAliveTime(String time, String parameterName);

    @Message(id = 12473, value = "Missing '%s' for parameter '%s'")
    OperationFailedException missingKeepAliveUnit(String unit, String parameterName);

    @Message(id = 12474, value = "executor is null")
    IllegalArgumentException nullExecutor();

    @Message(id = 12475, value = "%s must be greater than or equal to zero")
    XMLStreamException countMustBePositive(Attribute count, @Param Location location);

    @Message(id = 12476, value = "%s must be greater than or equal to zero")
    XMLStreamException perCpuMustBePositive(Attribute perCpu, @Param Location location);

    @Message(id = 12477, value = "Missing '%s' for '%s'")
    OperationFailedException missingTimeSpecTime(String time, String parameterName);

    @Message(id = 12478, value = "Failed to parse '%s', allowed values are: %s")
    OperationFailedException failedToParseUnit(String unit, List<TimeUnit> allowed);

    @Message(id = 12479, value = "unit is null")
    IllegalArgumentException nullUnit();
}

/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.jboss.as.controller.transform;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.as.controller.ControllerLogger;
import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * Logger utility class that provides a unified mechanism to log warnings that occur as part of the transformation process.
 * <p/>
 * All log messages are queued for time of transformation and then written in the log as a single entry for all problems that occurred.
 * This way it is simple to see what potential problems could happen for each host that is of different version than the domain controller
 * <p/>
 * Sample output would look like this:
 * There were some problems during transformation process for target host: 'host-name'
 * Problems found:
 * Transforming operation %s at resource %s to subsystem '%s' model version '%s' -- attributes %s attributes were rejected
 * Transforming operation %s at resource %s to core model '%s' model version '%s' -- attributes %s attributes were rejected
 *
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
public class TransformersLogger {
    private TransformationTarget target;
    private ControllerLogger logger;
    private List<LogEntry> messageQueue = Collections.synchronizedList(new LinkedList<LogEntry>());
    private static final ConcurrentHashMap<String, TransformersLogger> loggers = new ConcurrentHashMap<String, TransformersLogger>();

    private TransformersLogger(TransformationTarget target) {
        this.target = target;
        logger = Logger.getMessageLogger(ControllerLogger.class, ControllerLogger.class.getPackage().getName() + ".transformer." + target.getHostName());
    }

    public static TransformersLogger getLogger(TransformationTarget target){
        String hostName = target.getHostName()==null?"<unknown>":target.getHostName();
        TransformersLogger result = loggers.get(hostName);
        if (result == null) {
            result = new TransformersLogger(target);
            TransformersLogger existing = loggers.putIfAbsent(hostName, result);
            result = existing == null ? result : existing;
        }
        return result;
    }

    private static String findSubsystemName(PathAddress pathAddress) {
        for (PathElement element : pathAddress) {
            if (element.getKey().equals(SUBSYSTEM)) {
                return element.getValue();
            }
        }
        return null;
    }

    /**
     * Log a warning for the resource at the provided address and a single attribute. The detail message is a default
     * 'Attributes are not understood in the target model version and this resource will need to be ignored on the target host.'
     *
     * @param address   where warning occurred
     * @param attribute attribute we are warning about
     */
    public void logAttributeWarning(PathAddress address, String attribute) {
        logAttributeWarning(address, null, null, attribute);
    }

    /**
     * Log a warning for the resource at the provided address and the given attributes. The detail message is a default
     * 'Attributes are not understood in the target model version and this resource will need to be ignored on the target host.'
     *
     * @param address    where warning occurred
     * @param attributes attributes we are warning about
     */
    public void logAttributeWarning(PathAddress address, Set<String> attributes) {
        logAttributeWarning(address, null, null, attributes);
    }

    /**
     * Log warning for the resource at the provided address and single attribute, using the provided detail
     * message.
     *
     * @param address   where warning occurred
     * @param message   custom error message to append
     * @param attribute attribute we are warning about
     */
    public void logAttributeWarning(PathAddress address, String message, String attribute) {
        logAttributeWarning(address, null, message, attribute);
    }

    /**
     * Log a warning for the resource at the provided address and the given attributes, using the provided detail
     * message.
     *
     * @param address    where warning occurred
     * @param message    custom error message to append
     * @param attributes attributes we that have problems about
     */
    public void logAttributeWarning(PathAddress address, String message, Set<String> attributes) {
        messageQueue.add(new AttributeLogEntry(address, null, message, attributes));
    }


    /**
     * Log a warning for the given operation at the provided address for the given attribute, using the provided detail
     * message.
     *
     * @param address   where warning occurred
     * @param operation where which problem occurred
     * @param message   custom error message to append
     * @param attribute attribute we that has problem
     */
    public void logAttributeWarning(PathAddress address, ModelNode operation, String message, String attribute) {
        messageQueue.add(new AttributeLogEntry(address, operation, message, attribute));
    }

    /**
     * Log a warning for the given operation at the provided address for the given attributes, using the provided detail
     * message.
     *
     * @param address    where warning occurred
     * @param operation  where which problem occurred
     * @param message    custom error message to append
     * @param attributes attributes we that have problems about
     */
    public void logAttributeWarning(PathAddress address, ModelNode operation, String message, Set<String> attributes) {
        messageQueue.add(new AttributeLogEntry(address, operation, message, attributes));
    }

    /**
     * Get a warning message for the given operation at the provided address for the passed attributes with the given
     * custom message appended. Intended for use in providing a failure description for an operation
     * or an exception message for an {@link OperationFailedException}.
     *
     * @param address    where warning occurred
     * @param operation  where which problem occurred
     * @param message    custom error message to append
     * @param attributes attributes we that have problems about
     */
    public String getAttributeWarning(PathAddress address, ModelNode operation, String message, Set<String> attributes) {
        return new AttributeLogEntry(address, operation, message, attributes).getMessage();
    }

    /**
     * Get a warning message for the given operation at the provided address for the passed attributes with the given
     * custom message appended. Intended for use in providing a failure description for an operation
     * or an exception message for an {@link OperationFailedException}.
     *
     * @param address    where warning occurred
     * @param operation  where which problem occurred
     * @param message    custom error message to append
     * @param attributes attributes we that have problems about
     */
    private String getAttributeWarning(PathAddress address, ModelNode operation, String message, String... attributes) {
        return new AttributeLogEntry(address, operation, message, attributes).getMessage();
    }

    /**
     * Get a warning message for the given operation at the provided address for the passed attributes
     * with a default message appended. Intended for use in providing a failure description for an operation
     * or an exception message for an {@link OperationFailedException}.
     * The default appended message is 'Attributes are not understood in the target model version and this resource
     * will need to be ignored on the target host.'
     *
     * @param address    where warning occurred
     * @param operation  where which problem occurred
     * @param attributes attributes we that have problems about
     */
    public String getAttributeWarning(PathAddress address, ModelNode operation, String... attributes) {
        return getAttributeWarning(address, operation, null, attributes);
    }

    /**
     * Get a warning message for the given operation at the provided address for the passed attributes
     * with a default message appended. This is useful when you need to pass it as result of getFailureMessage()
     * The default appended message is 'Attributes are not understood in the target model version and this resource
     * will need to be ignored on the target host.'
     *
     * @param address    where warning occurred
     * @param operation  where which problem occurred
     * @param attributes attributes we that have problems about
     */
    public String getAttributeWarning(PathAddress address, ModelNode operation, Set<String> attributes) {
        return new AttributeLogEntry(address, operation, null, attributes).getMessage();
    }

    public String getRejectedResourceWarning(PathAddress address, ModelNode operation) {
        return new RejectResourceLogEntry(address, operation).getMessage();
    }

    public void logRejectedResourceWarning(PathAddress address, ModelNode operation) {
        messageQueue.add(new RejectResourceLogEntry(address, null));
    }

    /**
     * flushes log queue, this actually writes combined log message into system log
     */
    void flushLogQueue() {
        Set<String> problems = new LinkedHashSet<String>();
        for (LogEntry entry : messageQueue) {
            problems.add("\t\t" + entry.getMessage() + "\n");
        }
        if (!problems.isEmpty()) {
            logger.tranformationWarnings(target.getHostName(), problems);
        }
    }

    private interface LogEntry {
        String getMessage();
    }

    private class AttributeLogEntry implements LogEntry {
        private final PathAddress address;
        private final ModelNode operation;
        private final String message;
        private final Set<String> attributes;

        private AttributeLogEntry(PathAddress address, ModelNode operation, String message, String... attributes) {
            this(address, operation, message, new TreeSet<String>(Arrays.asList(attributes)));
        }

        private AttributeLogEntry(PathAddress address, ModelNode operation, String message, Set<String> attributes) {
            assert message != null || (attributes != null && attributes.size() > 0) : "a message must be provided or a list of attributes or both";
            this.address = address;
            this.operation = operation;
            this.message = message;
            this.attributes = attributes;
        }

        public String getMessage() {
            final ModelVersion coreVersion = target.getVersion();
            final String subsystemName = findSubsystemName(address);
            final ModelVersion usedVersion = subsystemName == null ? coreVersion : target.getSubsystemVersion(subsystemName);
            String msg = message == null ? ControllerMessages.MESSAGES.attributesAreNotUnderstoodAndMustBeIgnored() : message;
            String attributeSet = attributes != null && attributes.size() > 0 ? ControllerMessages.MESSAGES.attributeNames(attributes) : "";
            if (operation == null) {//resource transformation
                if (subsystemName != null) {
                    return ControllerMessages.MESSAGES.transformerLoggerSubsystemModelResourceTransformerAttributes(address, subsystemName, usedVersion, attributeSet, msg);
                } else {
                    return ControllerMessages.MESSAGES.transformerLoggerCoreModelResourceTransformerAttributes(address, usedVersion, attributeSet, msg);
                }
            } else {//operation transformation
                if (subsystemName != null) {
                    return ControllerMessages.MESSAGES.transformerLoggerSubsystemModelOperationTransformerAttributes(operation, address, subsystemName, usedVersion, attributeSet, msg);
                } else {
                    return ControllerMessages.MESSAGES.transformerLoggerCoreModelOperationTransformerAttributes(operation, address, usedVersion, attributeSet, msg);
                }
            }
        }
    }

    private class RejectResourceLogEntry implements LogEntry {
        private final PathAddress address;
        private final ModelNode operation;

        private RejectResourceLogEntry(PathAddress address, ModelNode operation) {
            this.address = address;
            this.operation = operation;
        }

        @Override
        public String getMessage() {
            if (operation != null) {
                return ControllerMessages.MESSAGES.rejectResourceOperationTransformation(address, operation);
            } else {
                return ControllerMessages.MESSAGES.rejectedResourceResourceTransformation(address);
            }

        }
    }
}

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

import org.jboss.as.controller.ControllerLogger;
import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
public class TransformersLogger {
    private TransformationTarget target;
    private ControllerLogger logger;
    private List<LogEntry> messageQueue = Collections.synchronizedList(new LinkedList<LogEntry>());

    public TransformersLogger(TransformationTarget target) {
        this.target = target;
        logger = Logger.getMessageLogger(ControllerLogger.class, ControllerLogger.class.getPackage().getName() + ".transformer." + target.getHostName());
    }

    private static String findSubsystemName(PathAddress pathAddress) {
        for (PathElement element : pathAddress) {
            if (element.getKey().equals(SUBSYSTEM)) {
                return element.getValue();
            }
        }
        return null;
    }

    public void logWarning(PathAddress address, String attribute) {
        logWarning(address, null, null, attribute);
    }

    public void logWarning(PathAddress address, Set<String> attributes) {
        logWarning(address, null, null, attributes);
    }

    public void logWarning(PathAddress address, String message, String attribute) {
        logWarning(address, null, message, attribute);
    }

    public void logWarning(PathAddress address, String message, Set<String> attributes) {
        messageQueue.add(new LogEntry(address, null, message, attributes));
    }

    public void logWarning(PathAddress address, ModelNode operation, String message, String attribute) {
        messageQueue.add(new LogEntry(address, operation, message, attribute));
    }

    public void logWarning(PathAddress address, ModelNode operation, String message, Set<String> attributes) {
        messageQueue.add(new LogEntry(address, operation, message, attributes));
    }

    public String getWarning(PathAddress address, ModelNode operation, String message, Set<String> attributes) {
        return getMessage(new LogEntry(address, operation, message, attributes));
    }

    private String getWarning(PathAddress address, ModelNode operation, String message, String... attributes) {
        return getMessage(new LogEntry(address, operation, message, attributes));
    }

    public String getWarning(PathAddress address, ModelNode operation, String... attributes) {
        return getWarning(address, operation, null, attributes);
    }

    public String getWarning(PathAddress address, ModelNode operation, Set<String> attributes) {
        return getMessage(new LogEntry(address, operation, null, attributes));
    }

    void flushLogQueue() {
        Set<String> problems = new LinkedHashSet<String>();
        for (LogEntry entry : messageQueue) {
            problems.add("\t\t" + getMessage(entry) + "\n");
        }
        if (!problems.isEmpty()) {
            logger.tranformationWarnings(target.getHostName(), problems);
        }
    }

    private String getMessage(LogEntry entry) {
        final String hostName = target.getHostName();
        final ModelVersion coreVersion = target.getVersion();
        final String subsystemName = findSubsystemName(entry.address);
        final ModelVersion usedVersion = subsystemName == null ? coreVersion : target.getSubsystemVersion(subsystemName);
        ModelNode operation = entry.operation;
        PathAddress address = entry.address;
        String msg = entry.message == null ? ControllerMessages.MESSAGES.attributesAreNotUnderstoodAndWillBeIgnored() : entry.message;
        if (operation == null) {//resource transformation
            if (subsystemName != null) {
                return ControllerMessages.MESSAGES.transformerLoggerSubsystemModelResourceTransformerAttributes(address, hostName, subsystemName, usedVersion, entry.attributes, msg);
            } else {
                return ControllerMessages.MESSAGES.transformerLoggerCoreModelResourceTransformerAttributes(address, hostName, usedVersion, entry.attributes, msg);
            }
        } else {//operation transformation
            if (subsystemName != null) {
                return ControllerMessages.MESSAGES.transformerLoggerSubsystemModelOperationTransformerAttributes(operation, address, hostName, subsystemName, usedVersion, entry.attributes, msg);
            } else {
                return ControllerMessages.MESSAGES.transformerLoggerCoreModelOperationTransformerAttributes(operation, address, hostName, usedVersion, entry.attributes, msg);
            }
        }
    }

    private static class LogEntry {
        private PathAddress address;
        private ModelNode operation;
        private String message;
        private Set<String> attributes;

        private LogEntry(PathAddress address, ModelNode operation, String message, String... attributes) {
            this.address = address;
            this.operation = operation;
            this.message = message;
            this.attributes = new TreeSet<String>(Arrays.asList(attributes));
        }

        private LogEntry(PathAddress address, ModelNode operation, String message, Set<String> attributes) {
            this.address = address;
            this.operation = operation;
            this.message = message;
            this.attributes = attributes;
        }
    }
}

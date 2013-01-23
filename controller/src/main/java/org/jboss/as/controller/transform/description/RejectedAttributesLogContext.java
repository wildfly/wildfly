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
package org.jboss.as.controller.transform.description;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.transform.TransformationTarget;
import org.jboss.as.controller.transform.TransformersLogger;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class RejectedAttributesLogContext {

    private final TransformationRule.AbstractChainedContext context;
    private final PathAddress address;
    private final ModelNode op;
    Map<String, RejectAttributeChecker> failedCheckers;
    Map<String, Map<String, ModelNode>> failedAttributes;

    RejectedAttributesLogContext(TransformationRule.AbstractChainedContext context, PathAddress address, ModelNode op) {
        this.context = context;
        this.address = address;
        this.op = op;
    }

    void checkAttribute(RejectAttributeChecker checker, String attributeName, ModelNode attributeValue) {
        if (op == null) {
            if (checker.rejectResourceAttribute(address, attributeName, attributeValue, context.getContext())) {
                reject(checker, attributeName, attributeValue);
            }
        } else {
            if (checker.rejectOperationParameter(address, attributeName, attributeValue, op, context.getContext())){
                reject(checker, attributeName, attributeValue);
            }
        }
    }

    private void reject(RejectAttributeChecker checker, String attributeName, ModelNode attributeValue) {
        assert checker.getRejectionLogMessageId() != null : "Null log id";
        final String id = checker.getRejectionLogMessageId();
        if (failedCheckers == null) {
            failedCheckers = new HashMap<String, RejectAttributeChecker>();
        }
        if (failedCheckers.get(id) == null) {
            failedCheckers.put(id, checker);
        }

        if (failedAttributes == null) {
            failedAttributes = new LinkedHashMap<String, Map<String, ModelNode>>();
        }
        Map<String, ModelNode> attributes = failedAttributes.get(checker.getRejectionLogMessageId());
        if (attributes == null) {
            attributes = new HashMap<String, ModelNode>();
            failedAttributes.put(checker.getRejectionLogMessageId(), attributes);
        }
        attributes.put(attributeName, attributeValue);
    }

    boolean hasRejections() {
        return failedAttributes != null;
    }

    @Deprecated //todo replace with context.getLogger()....
    String errorOrWarn() throws OperationFailedException {
        if (failedAttributes == null) {
            return "";
        }
        //TODO the determining of whether the version is 1.4.0, i.e. knows about ignored resources or not could be moved to a utility method

        final TransformationTarget tgt = context.getContext().getTarget();
        final String legacyHostName = tgt.getHostName();
        final ModelVersion coreVersion = tgt.getVersion();
        final String subsystemName = findSubsystemName(address);
        final ModelVersion usedVersion = subsystemName == null ? coreVersion : tgt.getSubsystemVersion(subsystemName);

        final TransformersLogger logger = context.getContext().getLogger();
        final boolean error = op == null && context.getContext().doesTargetSupportIgnoredResources(context.getContext().getTarget());
        List<String> messages = error ? new ArrayList<String>() : null;

        for (Map.Entry<String, Map<String, ModelNode>> entry : failedAttributes.entrySet()) {
            RejectAttributeChecker checker = failedCheckers.get(entry.getKey());
            String message = checker.getRejectionLogMessage(entry.getValue());

            if (error) {
                //Create our own custom exception containing everything
                messages.add(message);
            } else {
                if (op == null) {
                    logger.logWarning(address, null, message, entry.getValue().keySet());
                } else {
                    return logger.getWarning(address, op, message, entry.getValue().keySet());
                }
            }
        }

        if (error) {
            //We are 7.2.x so we should throw an error
            if (subsystemName != null) {
                throw ControllerMessages.MESSAGES.rejectAttributesSubsystemModelResourceTransformer(address, legacyHostName, subsystemName, usedVersion, messages);
            }
            throw ControllerMessages.MESSAGES.rejectAttributesCoreModelResourceTransformer(address, legacyHostName, usedVersion, messages);
        }
        return null;
    }

    private static String findSubsystemName(PathAddress pathAddress) {
        for (PathElement element : pathAddress) {
            if (element.getKey().equals(SUBSYSTEM)) {
                return element.getValue();
            }
        }
        return null;
    }
}

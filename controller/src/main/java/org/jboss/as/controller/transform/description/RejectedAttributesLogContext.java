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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.ControllerLogger;
import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.TransformationTarget;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
@Deprecated //todo replace with context.getLogger
class RejectedAttributesLogContext {

    private final TransformationContext context;
    private final PathAddress address;
    private final ModelNode op;
    Map<RejectAttributeLogAdapter, Set<String>> failedAttributes;

    RejectedAttributesLogContext(TransformationContext context, PathAddress address, ModelNode op) {
        this.context = context;
        this.address = address;
        this.op = op;
    }

    void checkAttribute(RejectAttributeChecker checker, String attributeName, ModelNode attributeValue) {
      //Protect the value so badly behaved reject checkers cannot modify it
        ModelNode protectedAttributeValue = attributeValue.clone();
        protectedAttributeValue.protect();
        if (op == null) {
            if (checker.rejectResourceAttribute(address, attributeName, protectedAttributeValue, context)) {
                reject(checker, attributeName);
            }
        } else {
            if (checker.rejectOperationParameter(address, attributeName, protectedAttributeValue, op, context)){
                reject(checker, attributeName);
            }
        }
    }

    private void reject(RejectAttributeChecker checker, String attributeName) {
        assert checker.getLogAdapter() != null : "Null log adapter";
        if (failedAttributes == null) {
            failedAttributes = new LinkedHashMap<RejectAttributeLogAdapter, Set<String>>();
        }
        Set<String> attributes = failedAttributes.get(checker.getLogAdapter());
        if (attributes == null) {
            attributes = new HashSet<String>();
            failedAttributes.put(checker.getLogAdapter(), attributes);
        }
        attributes.add(attributeName);
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

        final TransformationTarget tgt = context.getTarget();
        final String legacyHostName = tgt.getHostName();
        final ModelVersion coreVersion = tgt.getVersion();
        final String subsystemName = findSubsystemName(address);
        final ModelVersion usedVersion = subsystemName == null ? coreVersion : tgt.getSubsystemVersion(subsystemName);

        List<String> messages = new ArrayList<String>();
        for (Map.Entry<RejectAttributeLogAdapter, Set<String>> entry : failedAttributes.entrySet()) {
            messages.add(entry.getKey().getDetailMessage(entry.getValue()));
        }

        if (op == null) {
            if (coreVersion.getMajor() >= 1 && coreVersion.getMinor() >= 4) {
                //We are 7.2.x so we should throw an error
                if (subsystemName != null) {
                    throw ControllerMessages.MESSAGES.rejectAttributesSubsystemModelResourceTransformer(address, legacyHostName, subsystemName, usedVersion, messages);
                }
                throw ControllerMessages.MESSAGES.rejectAttributesCoreModelResourceTransformer(address, legacyHostName, usedVersion, messages);
            }
        }

        if (op == null) {
            if (subsystemName != null) {
                ControllerLogger.TRANSFORMER_LOGGER.rejectAttributesSubsystemModelResourceTransformer(address, legacyHostName, subsystemName, usedVersion, messages);
            } else {
                ControllerLogger.TRANSFORMER_LOGGER.rejectAttributesCoreModelResourceTransformer(address, legacyHostName, usedVersion, messages);
            }
            return null;
        } else {
            if (subsystemName != null) {
                return ControllerMessages.MESSAGES.rejectAttributesSubsystemModelOperationTransformer(op, address, legacyHostName, subsystemName, usedVersion, messages);
            } else {
                return ControllerMessages.MESSAGES.rejectAttributesCoreModelOperationTransformer(op, address, legacyHostName, usedVersion, messages);
            }
        }
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

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

package org.jboss.as.jdr;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.EnumSet;
import java.util.Locale;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.OperationEntry.Flag;
import org.jboss.dmr.ModelNode;

/**
 * Extension for triggering, requesting, generating and accessing JDR reports.
 *
 * @author Brian Stansberry
 */
public class JdrReportExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "jdr";

    private static final int MANAGEMENT_API_MAJOR_VERSION = 1;
    private static final int MANAGEMENT_API_MINOR_VERSION = 0;
    private static final int MANAGEMENT_API_MICRO_VERSION = 0;

    public void initialize(ExtensionContext context) {
        final DescriptionProvider subsystemDescription = new DescriptionProvider() {
            public ModelNode getModelDescription(Locale locale) {
                return JdrReportDescriptions.getJdrSubsystemDescription(locale);
            }
        };

        SubsystemRegistration subsystemRegistration = context.registerSubsystem(SUBSYSTEM_NAME, MANAGEMENT_API_MAJOR_VERSION,
                MANAGEMENT_API_MINOR_VERSION, MANAGEMENT_API_MICRO_VERSION);

        ManagementResourceRegistration root = subsystemRegistration.registerSubsystemModel(subsystemDescription);
        root.registerOperationHandler(JdrReportSubsystemAdd.OPERATION_NAME, JdrReportSubsystemAdd.INSTANCE, JdrReportSubsystemAdd.INSTANCE);
        root.registerOperationHandler(DESCRIBE, JdrSubsystemDescribeHandler.INSTANCE,
                JdrSubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);
        root.registerOperationHandler(JdrReportSubsystemRemove.OPERATION_NAME, JdrReportSubsystemRemove.INSTANCE, JdrReportSubsystemRemove.INSTANCE);

        if (context.isRuntimeOnlyRegistrationValid()) {
            root.registerOperationHandler(JdrReportRequestHandler.OPERATION_NAME, JdrReportRequestHandler.INSTANCE, JdrReportRequestHandler.INSTANCE, EnumSet.of(Flag.RUNTIME_ONLY));
        }
        subsystemRegistration.registerXMLElementWriter(JdrReportSubsystemParser.INSTANCE);
    }

    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.CURRENT.getUriString(), JdrReportSubsystemParser.INSTANCE);
    }

    private static class JdrSubsystemDescribeHandler implements OperationStepHandler, DescriptionProvider {
        static final JdrSubsystemDescribeHandler INSTANCE = new JdrSubsystemDescribeHandler();

        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            ModelNode result = context.getResult();

            result.add(Util.getEmptyOperation(ADD, pathAddress(PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME))));

            // TODO if child resources are developed, add operations to create those

            context.completeStep();
        }

        public ModelNode getModelDescription(Locale locale) {
            return CommonDescriptions.getSubsystemDescribeOperation(locale);
        }
    }

    private static ModelNode pathAddress(PathElement... elements) {
        return PathAddress.pathAddress(elements).toModelNode();
    }
}

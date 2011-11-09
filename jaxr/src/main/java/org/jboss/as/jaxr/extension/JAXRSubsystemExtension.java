/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jaxr.extension;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.jaxr.extension.JAXRConstants.Namespace;
import org.jboss.dmr.ModelNode;

import java.util.EnumSet;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;


/**
 * @author Thomas.Diesler@jboss.com
 * @since 26-Oct-2011
 */
public class JAXRSubsystemExtension implements Extension {

    /**
     * The parser used for parsing our subsystem
     */
    private final JAXRSubsystemParser parser = new JAXRSubsystemParser();

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), parser);
    }


    @Override
    public void initialize(ExtensionContext context) {
        SubsystemRegistration subsystem = context.registerSubsystem(JAXRConstants.SUBSYSTEM_NAME);
        ManagementResourceRegistration registration = subsystem.registerSubsystemModel(JAXRSubsystemProviders.SUBSYSTEM);
        registration.registerOperationHandler(ADD, JAXRSubsystemAdd.INSTANCE, JAXRSubsystemAdd.DESCRIPTION, false);
        registration.registerReadWriteAttribute(ModelConstants.CONNECTION, null, JAXRConnectionAttributeHandler.INSTANCE, EnumSet.of(AttributeAccess.Flag.STORAGE_CONFIGURATION, AttributeAccess.Flag.RESTART_ALL_SERVICES));
        registration.registerReadWriteAttribute(ModelConstants.DATASOURCE, null, JAXRDatasourceAttributeHandler.INSTANCE, EnumSet.of(AttributeAccess.Flag.STORAGE_CONFIGURATION, AttributeAccess.Flag.RESTART_ALL_SERVICES));
        registration.registerReadWriteAttribute(ModelConstants.DROPONSTART, null, JAXRDropOnStartAttributeHandler.INSTANCE, EnumSet.of(AttributeAccess.Flag.STORAGE_CONFIGURATION, AttributeAccess.Flag.RESTART_ALL_SERVICES));
        registration.registerReadWriteAttribute(ModelConstants.CREATEONSTART, null, JAXRCreateOnStartAttributeHandler.INSTANCE, EnumSet.of(AttributeAccess.Flag.STORAGE_CONFIGURATION, AttributeAccess.Flag.RESTART_ALL_SERVICES));
        registration.registerReadWriteAttribute(ModelConstants.DROPONSTOP, null, JAXRDropOnStopAttributeHandler.INSTANCE, EnumSet.of(AttributeAccess.Flag.STORAGE_CONFIGURATION, AttributeAccess.Flag.RESTART_ALL_SERVICES));
        registration.registerOperationHandler(DESCRIBE, SubsystemDescribeHandler.INSTANCE, SubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);
        subsystem.registerXMLElementWriter(JAXRSubsystemWriter.INSTANCE);
    }

    /**
     * Recreate the steps to put the subsystem in the same state it was in.
     * This is used in domain mode to query the profile being used, in order to
     * get the steps needed to create the servers
     */
    private static class SubsystemDescribeHandler extends GenericSubsystemDescribeHandler {
        static final SubsystemDescribeHandler INSTANCE = new SubsystemDescribeHandler();

        // Hide ctor
        private SubsystemDescribeHandler() {
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            super.execute(context, operation);
            context.completeStep();
        }
    }
}

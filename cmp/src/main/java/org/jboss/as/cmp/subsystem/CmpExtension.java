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

package org.jboss.as.cmp.subsystem;

import static org.jboss.as.cmp.subsystem.CmpConstants.HILO_KEY_GENERATOR;
import static org.jboss.as.cmp.subsystem.CmpConstants.UUID_KEY_GENERATOR;
import static org.jboss.as.cmp.subsystem.CmpSubsystemProviders.HILO_KEY_GENERATOR_DESC;
import static org.jboss.as.cmp.subsystem.CmpSubsystemProviders.UUID_KEY_GENERATOR_DESC;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;

/**
 * @author John Bailey
 */
public class CmpExtension implements Extension {
    public static final String SUBSYSTEM_NAME = "cmp";

    public void initialize(final ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, 1, 0);
        final ManagementResourceRegistration subsystemRegistration = subsystem.registerSubsystemModel(CmpSubsystemProviders.SUBSYSTEM);
        subsystem.registerXMLElementWriter(CmpSubsystem10Parser.INSTANCE);

        subsystemRegistration.registerOperationHandler(ADD, CmpSubsystemAdd.INSTANCE, CmpSubsystemAdd.INSTANCE, false);
        subsystemRegistration.registerOperationHandler(DESCRIBE, GenericSubsystemDescribeHandler.INSTANCE, GenericSubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);
        subsystemRegistration.registerOperationHandler(REMOVE, ReloadRequiredRemoveStepHandler.INSTANCE, CmpSubsystemProviders.SUBSYSTEM_REMOVE, false);

        final ManagementResourceRegistration uuidKeyGenerators = subsystemRegistration.registerSubModel(PathElement.pathElement(UUID_KEY_GENERATOR), UUID_KEY_GENERATOR_DESC);
        uuidKeyGenerators.registerOperationHandler(ADD, UUIDKeyGeneratorAdd.INSTANCE, UUIDKeyGeneratorAdd.INSTANCE, false);
        uuidKeyGenerators.registerOperationHandler(REMOVE, UUIDKeyGeneratorRemove.INSTANCE, UUIDKeyGeneratorRemove.INSTANCE, false);

        final ManagementResourceRegistration hiLoKeyGenerators = subsystemRegistration.registerSubModel(PathElement.pathElement(HILO_KEY_GENERATOR), HILO_KEY_GENERATOR_DESC);
        hiLoKeyGenerators.registerOperationHandler(ADD, HiLoKeyGeneratorAdd.INSTANCE, HiLoKeyGeneratorAdd.INSTANCE, false);
        hiLoKeyGenerators.registerOperationHandler(REMOVE, HiLoKeyGeneratorRemove.INSTANCE, HiLoKeyGeneratorRemove.INSTANCE, false);
    }

    public void initializeParsers(final ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.CMP_1_0.getUriString(), CmpSubsystem10Parser.INSTANCE);
    }
}

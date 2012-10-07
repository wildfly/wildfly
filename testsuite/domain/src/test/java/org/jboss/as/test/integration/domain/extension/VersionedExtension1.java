/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.domain.extension;

import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;

import java.util.EnumSet;

/**
 * Version 1 of an extension.
 *
 * @author Emanuel Muckenhuber
 */
public class VersionedExtension1 extends VersionedExtensionCommon {

    private static final PathElement ORIGINAL = PathElement.pathElement("element", "renamed");

    private static final SubsystemInitialization TEST_SUBSYSTEM = new SubsystemInitialization(SUBSYSTEM_NAME, false);

    @Override
    public void initialize(final ExtensionContext context) {

        // Register the test subsystem
        final SubsystemInitialization.RegistrationResult result = TEST_SUBSYSTEM.initializeSubsystem(context, ModelVersion.create(1, 0, 0));

        final ManagementResourceRegistration registration = result.getResourceRegistration();
        // Register an element which is going to get renamed
        registration.registerSubModel(createResourceDefinition(ORIGINAL));
        registration.registerOperationHandler("test", new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                context.getResult().set(true);
                context.stepCompleted();
            }
        }, DESCRIPTION_PROVIDER, false, OperationEntry.EntryType.PUBLIC, EnumSet.of(OperationEntry.Flag.READ_ONLY));

        // No transformers for the first version of the model!
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        TEST_SUBSYSTEM.initializeParsers(context);
    }
}

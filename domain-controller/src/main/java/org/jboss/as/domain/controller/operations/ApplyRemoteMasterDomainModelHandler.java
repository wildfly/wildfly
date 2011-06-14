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

package org.jboss.as.domain.controller.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_MODEL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SCHEMA_LOCATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.NewOperationContext.Stage;
import org.jboss.as.controller.NewStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.ExtensionDescription;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;

/**
 * Step handler responsible for taking in a domain model and updating the local domain model to match.
 *
 * @author John Bailey
 */
public class ApplyRemoteMasterDomainModelHandler implements NewStepHandler, DescriptionProvider {
    public static final String OPERATION_NAME = "apply-remote-domain-model";

    private final ExtensionContext extensionContext;

    public ApplyRemoteMasterDomainModelHandler(ExtensionContext extensionContext) {
        this.extensionContext = extensionContext;
    }

    public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {
        final ModelNode domainModel = operation.get(DOMAIN_MODEL);
        final ModelNode rootModel = context.readModelForUpdate(PathAddress.EMPTY_ADDRESS);

        if (domainModel.hasDefined(NAMESPACES)) {
            rootModel.get(NAMESPACES).set(domainModel.get(NAMESPACES));
        }
        if (domainModel.hasDefined(SCHEMA_LOCATIONS)) {
            rootModel.get(SCHEMA_LOCATIONS).set(domainModel.get(SCHEMA_LOCATIONS));
        }
        if (domainModel.hasDefined(EXTENSION)) {
            rootModel.get(EXTENSION).set(domainModel.get(EXTENSION));
        }
        if (domainModel.hasDefined(PATH)) {
            rootModel.get(PATH).set(domainModel.get(PATH));
        }
        if (domainModel.hasDefined(SYSTEM_PROPERTY)) {
            rootModel.get(SYSTEM_PROPERTY).set(domainModel.get(SYSTEM_PROPERTY));
        }
        if (domainModel.hasDefined(PROFILE)) {
            rootModel.get(PROFILE).set(domainModel.get(PROFILE));
        }
        if (domainModel.hasDefined(INTERFACE)) {
            rootModel.get(INTERFACE).set(domainModel.get(INTERFACE));
        }
        if (domainModel.hasDefined(SOCKET_BINDING_GROUP)) {
            rootModel.get(SOCKET_BINDING_GROUP).set(domainModel.get(SOCKET_BINDING_GROUP));
        }
        if (domainModel.hasDefined(DEPLOYMENT)) {
            rootModel.get(DEPLOYMENT).set(domainModel.get(DEPLOYMENT));
        }
        if (domainModel.hasDefined(SERVER_GROUP)) {
            rootModel.get(SERVER_GROUP).set(domainModel.get(SERVER_GROUP));
        }

        final Set<String> modules = new HashSet<String>();
        // If we were provided a model, we're a slave and need to initialize all extensions
        if (rootModel != null && rootModel.hasDefined(EXTENSION)) {
            for (Property prop : rootModel.get(EXTENSION).asPropertyList()) {
                try {
                    final String module = prop.getValue().get(ExtensionDescription.MODULE).asString();
                    if (modules.contains(module)) {
                        continue;
                    }
                    for (final Extension extension : Module.loadServiceFromCallerModuleLoader(ModuleIdentifier.fromString(module), Extension.class)) {
                        modules.add(module);

                        context.addStep(new NewStepHandler() {
                            public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {
                                extension.initialize(extensionContext);
                                context.completeStep();
                            }
                        }, Stage.IMMEDIATE);
                    }
                } catch (ModuleLoadException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        context.completeStep();
    }

    public ModelNode getModelDescription(Locale locale) {
        return new ModelNode(); // PRIVATE operation requires no description
    }
}

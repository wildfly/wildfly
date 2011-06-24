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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_MODEL;

import java.util.Locale;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;

/**
 * Step handler responsible for taking in a domain model and updating the local domain model to match.
 *
 * @author John Bailey
 */
public class ApplyRemoteMasterDomainModelHandler implements OperationStepHandler, DescriptionProvider {
    public static final String OPERATION_NAME = "apply-remote-domain-model";

    //This is a hack to avoid initializing the extensions again for the case when master is restarted and we reconnect
    private boolean appliedExensions;

    private final ExtensionContext extensionContext;

    public ApplyRemoteMasterDomainModelHandler(ExtensionContext extensionContext) {
        this.extensionContext = extensionContext;
    }

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final ModelNode domainModel = operation.get(DOMAIN_MODEL);
        // We get the model as a list of resources descriptions

        if (!appliedExensions) {
            for(final ModelNode resourceDescription : domainModel.asList()) {
                appliedExensions = true;
                final PathAddress resourceAddress = PathAddress.pathAddress(resourceDescription.require("domain-resource-address"));
                final Resource resource = context.createResource(resourceAddress);
                if(resourceAddress.size() == 1 && resourceAddress.getElement(0).getKey().equals(ModelDescriptionConstants.EXTENSION)) {
                    final String module = resourceAddress.getElement(0).getValue();
                    try {
                        for (final Extension extension : Module.loadServiceFromCallerModuleLoader(ModuleIdentifier.fromString(module), Extension.class)) {
                            extension.initialize(extensionContext);
                        }
                    } catch (ModuleLoadException e) {
                        throw new RuntimeException(e);
                    }
                }
                resource.writeModel(resourceDescription.get("domain-resource-model"));
            }
        }
        context.completeStep();
    }

    public ModelNode getModelDescription(Locale locale) {
        return new ModelNode(); // PRIVATE operation requires no description
    }
}

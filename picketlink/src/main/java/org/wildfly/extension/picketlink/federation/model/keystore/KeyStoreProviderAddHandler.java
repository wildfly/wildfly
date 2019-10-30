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

package org.wildfly.extension.picketlink.federation.model.keystore;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.picketlink.config.federation.AuthPropertyType;
import org.picketlink.config.federation.KeyProviderType;
import org.picketlink.identity.federation.core.impl.KeyStoreKeyManager;
import org.wildfly.extension.picketlink.federation.service.FederationService;
import org.wildfly.extension.picketlink.federation.service.KeyStoreProviderService;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class KeyStoreProviderAddHandler extends AbstractAddStepHandler {

    public static final KeyStoreProviderAddHandler INSTANCE = new KeyStoreProviderAddHandler();

    private KeyStoreProviderAddHandler() {
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (AttributeDefinition attribute : KeyStoreProviderResourceDefinition.INSTANCE.getAttributes()) {
            attribute.validateAndSet(operation, model);
        }
    }

    static void launchServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        PathAddress pathAddress = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS));
        String federationAlias = pathAddress.subAddress(0, pathAddress.size() - 1).getLastElement().getValue();

        ModelNode relativeToNode = KeyStoreProviderResourceDefinition.RELATIVE_TO.resolveModelAttribute(context, model);
        String relativeTo = null;

        if (relativeToNode.isDefined()) {
            relativeTo = relativeToNode.asString();
        }

        String file = KeyStoreProviderResourceDefinition.FILE.resolveModelAttribute(context, model).asString();

        KeyStoreProviderService service = new KeyStoreProviderService(toKeyProviderType(context, model), file, relativeTo);
        context.getServiceTarget().addService(KeyStoreProviderService
            .createServiceName(federationAlias), service)
                .addDependency(FederationService.createServiceName(federationAlias), FederationService.class,
                                        service.getFederationService())
                .addDependency(PathManagerService.SERVICE_NAME, PathManager.class, service.getPathManager())
                .setInitialMode(ServiceController.Mode.PASSIVE)
                .install();
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
        launchServices(context, operation, model);
    }

    static KeyProviderType toKeyProviderType(OperationContext context, ModelNode model) throws OperationFailedException {
        KeyProviderType keyProviderType = new KeyProviderType();

        keyProviderType.setClassName(KeyStoreKeyManager.class.getName());

        keyProviderType.setSigningAlias(KeyStoreProviderResourceDefinition.SIGN_KEY_ALIAS.resolveModelAttribute(context, model).asString());

        AuthPropertyType keyStorePass = new AuthPropertyType();

        keyStorePass.setKey("KeyStorePass");
        keyStorePass.setValue(KeyStoreProviderResourceDefinition.PASSWORD.resolveModelAttribute(context, model).asString());

        keyProviderType.add(keyStorePass);

        AuthPropertyType signingKeyPass = new AuthPropertyType();

        signingKeyPass.setKey("SigningKeyPass");
        signingKeyPass.setValue(KeyStoreProviderResourceDefinition.SIGN_KEY_PASSWORD.resolveModelAttribute(context, model).asString());

        keyProviderType.add(signingKeyPass);

        AuthPropertyType signingKeyAlias = new AuthPropertyType();

        signingKeyAlias.setKey("SigningKeyAlias");
        signingKeyAlias.setValue(KeyStoreProviderResourceDefinition.SIGN_KEY_ALIAS.resolveModelAttribute(context, model).asString());

        keyProviderType.add(signingKeyAlias);

        return keyProviderType;
    }
}

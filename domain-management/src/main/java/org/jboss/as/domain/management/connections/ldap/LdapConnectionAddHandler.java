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

package org.jboss.as.domain.management.connections.ldap;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.domain.management.connections.ldap.LdapConnectionResourceDefinition.SECURITY_REALM;
import static org.jboss.as.domain.management.connections.ldap.LdapConnectionResourceDefinition.INITIAL_CONTEXT_FACTORY;
import static org.jboss.as.domain.management.connections.ldap.LdapConnectionResourceDefinition.URL;
import static org.jboss.as.domain.management.connections.ldap.LdapConnectionResourceDefinition.SEARCH_DN;
import static org.jboss.as.domain.management.connections.ldap.LdapConnectionResourceDefinition.SEARCH_CREDENTIAL;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.as.domain.management.connections.ldap.LdapConnectionManagerService.Config;
import org.jboss.as.domain.management.security.SSLContextService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * Handler for adding ldap management connections.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class LdapConnectionAddHandler extends AbstractAddStepHandler {

    public static final LdapConnectionAddHandler INSTANCE = new LdapConnectionAddHandler();

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (AttributeDefinition attr : LdapConnectionResourceDefinition.ATTRIBUTE_DEFINITIONS) {
            attr.validateAndSet(operation, model);
        }
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return true;
    }

    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model,
                                  final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> controllers) throws OperationFailedException {
        PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();

        final ServiceTarget serviceTarget = context.getServiceTarget();
        final LdapConnectionManagerService connectionManagerService = new LdapConnectionManagerService();
        updateRuntime(context, model, connectionManagerService);

        ServiceBuilder<LdapConnectionManagerService> sb = serviceTarget.addService(
                LdapConnectionManagerService.ServiceUtil.createServiceName(name), connectionManagerService).setInitialMode(
                ServiceController.Mode.ON_DEMAND);

        if (verificationHandler != null) {
            sb.addListener(verificationHandler);
        }

        ModelNode securityRealm = SECURITY_REALM.resolveModelAttribute(context, model);
        if (securityRealm.isDefined()) {
            String realmName = securityRealm.asString();
            SSLContextService.ServiceUtil.addDependency(sb, connectionManagerService.getFullSSLContextInjector(), SecurityRealm.ServiceUtil.createServiceName(realmName), false);
            SSLContextService.ServiceUtil.addDependency(sb, connectionManagerService.getTrustOnlySSLContextInjector(), SecurityRealm.ServiceUtil.createServiceName(realmName), true);
        }

        ServiceController<LdapConnectionManagerService> sc = sb.install();
        if (controllers != null) {
            controllers.add(sc);
        }
    }


    static Config updateRuntime(final OperationContext context, final ModelNode model, final LdapConnectionManagerService connectionManagerService) throws OperationFailedException {
        String initialContextFactory = INITIAL_CONTEXT_FACTORY.resolveModelAttribute(context, model).asString();
        String url = URL.resolveModelAttribute(context, model).asString();
        ModelNode searchDnNode = SEARCH_DN.resolveModelAttribute(context, model);
        String searchDn = searchDnNode.isDefined() ? searchDnNode.asString() : null;
        ModelNode searchCredentialNode = SEARCH_CREDENTIAL.resolveModelAttribute(context, model);
        String searchCredential = searchCredentialNode.isDefined() ? searchCredentialNode.asString() : null;

        return connectionManagerService.setConfiguration(initialContextFactory, url, searchDn, searchCredential);
    }

}

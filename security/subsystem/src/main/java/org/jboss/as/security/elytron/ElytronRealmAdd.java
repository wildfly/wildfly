/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.security.elytron;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.security.plugins.SecurityDomainContext;
import org.jboss.as.security.service.SecurityDomainService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.security.auth.server.SecurityRealm;

/**
 * An {@link org.jboss.as.controller.AbstractAddStepHandler} that installs  the {@link SecurityDomainContextRealmService}
 * that will create and export an Elytron compatible realm. The created realm delegates the underlying authentication
 * decision to a legacy {@link org.jboss.as.security.plugins.SecurityDomainContext}.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class ElytronRealmAdd extends AbstractAddStepHandler {

    ElytronRealmAdd(final RuntimeCapability<Void> capability, final AttributeDefinition... attributes) {
        super(capability, attributes);
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String elytronRealmName = address.getLastElement().getValue();
        final ServiceTarget target = context.getServiceTarget();

        // install the service, adding a dependency on the legacy security domain service.
        final ModelNode securityDomainName = ElytronRealmResourceDefinition.LEGACY_DOMAIN_NAME.resolveModelAttribute(context, model);
        if (securityDomainName.isDefined()) {
            final ServiceName realmServiceName = context.getCapabilityServiceName(ElytronRealmResourceDefinition.SECURITY_REALM_CAPABILITY, elytronRealmName, SecurityRealm.class);
            final SecurityDomainContextRealmService domainContextRealmService = new SecurityDomainContextRealmService();
            target.addService(realmServiceName, domainContextRealmService)
                    .addDependency(SecurityDomainService.SERVICE_NAME.append(securityDomainName.asString()), SecurityDomainContext.class, domainContextRealmService.getSecurityDomainContextInjector())
                    .setInitialMode(ServiceController.Mode.ACTIVE).install();
        }

    }
}

/*
 * Copyright 2021 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.messaging.activemq;

import static org.wildfly.extension.messaging.activemq.ServerDefinition.ELYTRON_DOMAIN;
import static org.wildfly.extension.messaging.activemq.ServerDefinition.SECURITY_DOMAIN;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

/**
 * OSH to add a capability requirement on legacy security domain.
 * @author Emmanuel Hugonnet (c) 2021 Red Hat, Inc.
 */
class SecurityDomainCheckHandler implements OperationStepHandler {

    static final SecurityDomainCheckHandler INSTANCE = new SecurityDomainCheckHandler();

    private SecurityDomainCheckHandler() {
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS, false).getModel();
        String legacyDomain = SECURITY_DOMAIN.resolveModelAttribute(context, model).asString();
        if (!ELYTRON_DOMAIN.resolveModelAttribute(context, model).isDefined() && SECURITY_DOMAIN.resolveModelAttribute(context, model).isDefined()) {
            context.registerAdditionalCapabilityRequirement(
                    Capabilities.LEGACY_SECURITY_DOMAIN_CAPABILITY.getDynamicName(legacyDomain),
                    Capabilities.ACTIVEMQ_SERVER_CAPABILITY.getDynamicName(context.getCurrentAddress()),
                    SECURITY_DOMAIN.getName());
        } else {
            context.deregisterCapabilityRequirement(
                    Capabilities.LEGACY_SECURITY_DOMAIN_CAPABILITY.getDynamicName(legacyDomain),
                    Capabilities.ACTIVEMQ_SERVER_CAPABILITY.getDynamicName(context.getCurrentAddress()));
        }
    }

}

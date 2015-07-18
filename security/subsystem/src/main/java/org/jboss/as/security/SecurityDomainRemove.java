/*
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2015, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.security;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

/**
 * <p>
 * A handler that removes the security domain configuration. This implementation overrides some of the
 * {@link org.jboss.as.controller.ServiceRemoveStepHandler} methods to ensure that the security realm capability is properly
 * deregistered and that the security realm service is also removed. It differs from its superclass in that it uses the
 * value defined in the {@code export-elytron-realm} attribute to dynamically build the capability and service names, whereas
 * the superclass uses the security domain name.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
class SecurityDomainRemove extends ServiceRemoveStepHandler {

    SecurityDomainRemove(final ServiceName serviceName, final AbstractAddStepHandler addHandler) {
        super(serviceName, addHandler);
    }

    @Override
    protected void recordCapabilitiesAndRequirements(final OperationContext context, final ModelNode operation, final Resource resource) throws OperationFailedException {
        super.recordCapabilitiesAndRequirements(context, operation, resource);
        // make sure the security realm capability is deregistered if needed.
        ModelNode elytronRealm = SecurityDomainResourceDefinition.EXPORT_ELYTRON_REALM.resolveModelAttribute(context, resource.getModel());
        if (elytronRealm.isDefined()) {
            context.deregisterCapability(Capabilities.SECURITY_REALM_RUNTIME_CAPABILITY.getDynamicName(elytronRealm.asString()));
        }
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model) {
        super.performRuntime(context, operation, model);
        if (context.isResourceServiceRestartAllowed()) {
            ModelNode elytronRealm = model.get(Constants.EXPORT_ELYTRON_REALM);
            if (elytronRealm.isDefined()) {
                ServiceName serviceName = Capabilities.SECURITY_REALM_RUNTIME_CAPABILITY.getCapabilityServiceName(elytronRealm.asString());
                context.removeService(serviceName);
            }
        }
    }
}

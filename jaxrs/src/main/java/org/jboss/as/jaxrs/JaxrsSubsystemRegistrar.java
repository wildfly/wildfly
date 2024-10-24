/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jaxrs;

import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.SubsystemResourceDefinitionRegistrar;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class JaxrsSubsystemRegistrar implements SubsystemResourceDefinitionRegistrar {

    @Override
    public ManagementResourceRegistration register(final SubsystemRegistration parent, final ManagementResourceRegistrationContext context) {
        final ManagementResourceRegistration registration = parent.registerSubsystemModel(JaxrsSubsystemDefinition.INSTANCE);

        // /deployment=*/subsystem=jaxrs
        final ManagementResourceRegistration deployment = parent.registerDeploymentModel(JaxrsDeploymentDefinition.INSTANCE);
        deployment.registerSubModel(new DeploymentRestResourcesDefintion());
        return registration;
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.ResourceDefinition;

/**
 * Enhanced {@link org.jboss.as.controller.SubsystemRegistration} that also exposes the registration context.
 * @author Paul Ferraro
 */
public interface SubsystemRegistration extends org.jboss.as.controller.SubsystemRegistration, ManagementRegistrationContext {

    @Override
    ManagementResourceRegistration registerSubsystemModel(ResourceDefinition definition);

    @Override
    ManagementResourceRegistration registerDeploymentModel(ResourceDefinition definition);
}

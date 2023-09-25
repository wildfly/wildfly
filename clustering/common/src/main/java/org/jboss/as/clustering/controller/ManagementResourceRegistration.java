/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.descriptions.OverrideDescriptionProvider;

/**
 * Enhanced {@link org.jboss.as.controller.registry.ManagementResourceRegistration} that also exposes the registration context.
 * @author Paul Ferraro
 */
public interface ManagementResourceRegistration extends org.jboss.as.controller.registry.ManagementResourceRegistration, ManagementRegistrationContext {

    @Override
    ManagementResourceRegistration registerSubModel(ResourceDefinition resourceDefinition);

    @Override
    ManagementResourceRegistration registerOverrideModel(String name, OverrideDescriptionProvider descriptionProvider);
}

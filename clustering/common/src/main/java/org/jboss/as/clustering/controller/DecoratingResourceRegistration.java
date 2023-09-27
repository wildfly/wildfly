/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.function.Function;

import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.descriptions.OverrideDescriptionProvider;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Generic {@link ManagementResourceRegistration} decorator.
 * @author Paul Ferraro
 */
public class DecoratingResourceRegistration<R extends ManagementResourceRegistration> extends org.jboss.as.controller.registry.DelegatingManagementResourceRegistration {

    private final Function<ManagementResourceRegistration, R> decorator;

    public DecoratingResourceRegistration(ManagementResourceRegistration delegate, Function<ManagementResourceRegistration, R> decorator) {
        super(delegate);
        this.decorator = decorator;
    }

    @Override
    public R registerSubModel(ResourceDefinition definition) {
        return this.decorator.apply(super.registerSubModel(definition));
    }

    @Override
    public R registerOverrideModel(String name, OverrideDescriptionProvider descriptionProvider) {
        return this.decorator.apply(super.registerOverrideModel(name, descriptionProvider));
    }
}

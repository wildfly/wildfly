/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

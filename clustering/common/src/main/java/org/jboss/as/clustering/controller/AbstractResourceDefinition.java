/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Resource definition for resources that performs all registration via {@link Registration#register(Object)}.
 * All other registerXXX(...) methods have been made final - to prevent misuse.
 * @author Paul Ferraro
 */
public abstract class AbstractResourceDefinition extends SimpleResourceDefinition {

    protected AbstractResourceDefinition(PathElement path, ResourceDescriptionResolver resolver) {
        super(new Parameters(path, resolver));
    }

    protected AbstractResourceDefinition(Parameters parameters) {
        super(parameters);
    }

    @Override
    public final void registerOperations(ManagementResourceRegistration resourceRegistration) {
    }

    @Override
    public final void registerAttributes(ManagementResourceRegistration resourceRegistration) {
    }

    @Override
    public final void registerNotifications(ManagementResourceRegistration resourceRegistration) {
    }

    @Override
    public final void registerChildren(ManagementResourceRegistration resourceRegistration) {
    }

    @Override
    public final void registerCapabilities(ManagementResourceRegistration resourceRegistration) {
    }
}

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

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Convenience extension of {@link AbstractBoottimeAddStepHandler} that that delegates service installation/rollback to a {@link ResourceServiceHandler}.
 * @author Paul Ferraro
 */
public class BoottimeAddStepHandler extends AbstractBoottimeAddStepHandler implements Registration {

    private final ResourceDescriptionResolver resolver;
    private final ResourceServiceHandler handler;
    private final List<Attribute> attributes = new LinkedList<>();

    public <E extends Enum<E> & Attribute> BoottimeAddStepHandler(ResourceDescriptionResolver resolver, ResourceServiceHandler handler) {
        this.resolver = resolver;
        this.handler = handler;
    }

    public <E extends Enum<E> & Attribute> BoottimeAddStepHandler addAttributes(Class<E> enumClass) {
        return this.addAttributes(EnumSet.allOf(enumClass));
    }

    public BoottimeAddStepHandler addAttributes(Attribute... attributes) {
        return this.addAttributes(Arrays.asList(attributes));
    }

    public BoottimeAddStepHandler addAttributes(Collection<? extends Attribute> attributes) {
        this.attributes.addAll(attributes);
        return this;
    }

    @Override
    protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        ModelNode model = resource.getModel();
        for (Attribute attribute : this.attributes) {
            attribute.getDefinition().validateAndSet(operation, model);
        }
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        this.handler.installServices(context, resource.getModel());
    }

    @Override
    protected void rollbackRuntime(OperationContext context, ModelNode operation, Resource resource) {
        try {
            this.handler.removeServices(context, resource.getModel());
        } catch (OperationFailedException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void register(ManagementResourceRegistration registration) {
        SimpleOperationDefinitionBuilder builder = new SimpleOperationDefinitionBuilder(ModelDescriptionConstants.ADD, this.resolver).withFlag(OperationEntry.Flag.RESTART_NONE);
        for (Attribute attribute : this.attributes) {
            builder.addParameter(attribute.getDefinition());
        }
        registration.registerOperationHandler(builder.build(), this);
    }
}

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

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RestartParentResourceAddHandler;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

/**
 * {@link RestartParentResourceAddHandler} that leverages a {@link ResourceServiceBuilderFactory} for service recreation.
 * @author Paul Ferraro
 */
public class RestartParentAddHandler<T> extends RestartParentResourceAddHandler implements Registration {

    private final ResourceDescriptionResolver resolver;
    private final ResourceServiceBuilderFactory<T> builderFactory;
    private final List<Attribute> attributes = new LinkedList<>();

    public RestartParentAddHandler(ResourceDescriptionResolver resolver, ResourceServiceBuilderFactory<T> builderFactory) {
        super(null);
        this.resolver = resolver;
        this.builderFactory = builderFactory;
    }

    public <E extends Enum<E> & Attribute> RestartParentAddHandler<T> addAttributes(Class<E> enumClass) {
        return this.addAttributes(EnumSet.allOf(enumClass));
    }

    public RestartParentAddHandler<T> addAttributes(Attribute... attributes) {
        return this.addAttributes(Arrays.asList(attributes));
    }

    public RestartParentAddHandler<T> addAttributes(Collection<? extends Attribute> attributes) {
        this.attributes.addAll(attributes);
        return this;
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (Attribute attribute : this.attributes) {
            attribute.getDefinition().validateAndSet(operation, model);
        }
    }

    @Override
    protected void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode parentModel) throws OperationFailedException {
        this.builderFactory.createBuilder(parentAddress).configure(context, parentModel).build(context.getServiceTarget()).install();
    }

    @Override
    protected ServiceName getParentServiceName(PathAddress parentAddress) {
        return this.builderFactory.createBuilder(parentAddress).getServiceName();
    }

    @Override
    protected PathAddress getParentAddress(PathAddress address) {
        return address.getParent();
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

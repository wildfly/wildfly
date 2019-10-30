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

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.ModelVersionRange;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.CombinedTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.staxmapper.XMLElementWriter;

/**
 * Generic {@link SubsystemRegistration} decorator.
 * @author Paul Ferraro
 */
public class DecoratingSubsystemRegistration<R extends ManagementResourceRegistration> implements SubsystemRegistration {

    private final SubsystemRegistration registration;
    private final Function<ManagementResourceRegistration, R> decorator;

    public DecoratingSubsystemRegistration(SubsystemRegistration registration, Function<ManagementResourceRegistration, R> decorator) {
        this.registration = registration;
        this.decorator = decorator;
    }

    @Override
    public void setHostCapable() {
        this.registration.setHostCapable();
    }

    @Deprecated
    @Override
    public void registerXMLElementWriter(XMLElementWriter<SubsystemMarshallingContext> writer) {
        this.registration.registerXMLElementWriter(writer);
    }

    @Deprecated
    @Override
    public TransformersSubRegistration registerModelTransformers(ModelVersionRange version, ResourceTransformer resourceTransformer) {
        return this.registration.registerModelTransformers(version, resourceTransformer);
    }

    @Deprecated
    @Override
    public TransformersSubRegistration registerModelTransformers(ModelVersionRange version, ResourceTransformer resourceTransformer, OperationTransformer operationTransformer) {
        return this.registration.registerModelTransformers(version, resourceTransformer, operationTransformer);
    }

    @Deprecated
    @Override
    public TransformersSubRegistration registerModelTransformers(ModelVersionRange version, ResourceTransformer resourceTransformer, OperationTransformer operationTransformer, boolean placeholder) {
        return this.registration.registerModelTransformers(version, resourceTransformer, operationTransformer, placeholder);
    }

    @Deprecated
    @Override
    public TransformersSubRegistration registerModelTransformers(ModelVersionRange version, CombinedTransformer combinedTransformer) {
        return this.registration.registerModelTransformers(version, combinedTransformer);
    }

    @Override
    public ModelVersion getSubsystemVersion() {
        return this.registration.getSubsystemVersion();
    }

    @Override
    public R registerSubsystemModel(ResourceDefinition definition) {
        return this.decorator.apply(this.registration.registerSubsystemModel(definition));
    }

    @Override
    public R registerDeploymentModel(ResourceDefinition definition) {
        return this.decorator.apply(this.registration.registerDeploymentModel(definition));
    }
}

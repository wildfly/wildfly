/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
import org.jboss.as.version.Stability;
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

    @Deprecated // note that since https://issues.redhat.com/browse/WFCORE-3441 this method is no longer deprecated in the interface
    @Override
    public void registerXMLElementWriter(XMLElementWriter<SubsystemMarshallingContext> writer) {
        this.registration.registerXMLElementWriter(writer);
    }

    /**
     * Do not use. Always throws {@code UnsupportedOperationException}. See https://issues.redhat.com/browse/WFLY-17319
     */
    @Deprecated
    public TransformersSubRegistration registerModelTransformers(ModelVersionRange version, ResourceTransformer resourceTransformer) {
        throw new UnsupportedOperationException("WFLY-17319");
    }

    /**
     * Do not use. Always throws {@code UnsupportedOperationException}. See https://issues.redhat.com/browse/WFLY-17319
     */
    @Deprecated
    public TransformersSubRegistration registerModelTransformers(ModelVersionRange version, ResourceTransformer resourceTransformer, OperationTransformer operationTransformer) {
        throw new UnsupportedOperationException("WFLY-17319");
    }

    /**
     * Do not use. Always throws {@code UnsupportedOperationException}. See https://issues.redhat.com/browse/WFLY-17319
     */
    @Deprecated
    public TransformersSubRegistration registerModelTransformers(ModelVersionRange version, ResourceTransformer resourceTransformer, OperationTransformer operationTransformer, boolean placeholder) {
        throw new UnsupportedOperationException("WFLY-17319");
    }

    /**
     * Do not use. Always throws {@code UnsupportedOperationException}. See https://issues.redhat.com/browse/WFLY-17319
     */
    @Deprecated
    public TransformersSubRegistration registerModelTransformers(ModelVersionRange version, CombinedTransformer combinedTransformer) {
        throw new UnsupportedOperationException("WFLY-17319");
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

    @Override
    public Stability getStability() {
        return this.registration.getStability();
    }
}

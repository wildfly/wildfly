/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.ejb;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

import java.util.function.Consumer;

/**
 * Transformer for bean management resources.
 * @author rachmato@ibm.com
 */
public class EjbClientServicesResourceTransformer implements Consumer<ModelVersion> {

    private final ResourceTransformationDescriptionBuilder parent;

    EjbClientServicesResourceTransformer(ResourceTransformationDescriptionBuilder parent) {
        this.parent = parent;
    }

    @Override
    public void accept(ModelVersion version) {

        if (DistributableEjbSubsystemModel.VERSION_2_0_0.requiresTransformation(version)) {
            // change the name of the child adressable resource
            parent.addChildRedirection(PathElement.pathElement("ejb-client-services"), PathElement.pathElement("client-mappings-registry"));
        }
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.ejb;

import java.util.function.Consumer;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Transformer for EJB client services provider resources.
 * @author Richard Achmatowicz
 */
public class EjbClientServicesProviderResourceTransformer implements Consumer<ModelVersion> {

    private final ResourceTransformationDescriptionBuilder parent;

    EjbClientServicesProviderResourceTransformer(ResourceTransformationDescriptionBuilder parent) {
        this.parent = parent;
    }

    @Override
    public void accept(ModelVersion version) {
        if (DistributableEjbSubsystemModel.VERSION_3_0_0.requiresTransformation(version)) {
            // This resource was addressed via client-mappings-registry prior to VERSION_3_0_0
            this.parent.addChildRedirection(EjbClientServicesProviderResourceRegistration.WILDCARD.getPathElement(), PathElement.pathElement(EjbClientServicesProviderResourceDefinitionRegistrar.LEGACY_PATH_KEY));
        }
    }
}

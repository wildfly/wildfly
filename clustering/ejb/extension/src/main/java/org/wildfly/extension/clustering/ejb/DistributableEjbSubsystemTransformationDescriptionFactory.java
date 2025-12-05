package org.wildfly.extension.clustering.ejb;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;

import java.util.function.Function;

/**
 * Used to create TransformationDescription instances for the distributable-ejb subsystem.
 * @author rachnato@ibm.com
 */
public enum DistributableEjbSubsystemTransformationDescriptionFactory implements Function<ModelVersion, TransformationDescription> {
    INSTANCE;

    public TransformationDescription apply(ModelVersion version) {
        ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();
        if (DistributableEjbSubsystemModel.VERSION_2_0_0.requiresTransformation(version)) {
            // the name of the child addressable resource in the model
            builder.addChildRedirection(PathElement.pathElement("ejb-client-services"), PathElement.pathElement("client-mappings-registry"));
        }
        return builder.build();
    }
}

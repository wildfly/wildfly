package org.wildfly.extension.clustering.ejb;

import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemExtensionTransformerRegistration;
import org.kohsuke.MetaInfServices;

/**
 * Registers model transformers for the distributable-ejb subsystem model when used in dmian mode.
 * @author rachmato@ibm.com
 */
@MetaInfServices(ExtensionTransformerRegistration.class)
public class DistributableEjbTransformerRegistration extends SubsystemExtensionTransformerRegistration {
    public DistributableEjbTransformerRegistration() {
        super(DistributableEjbSubsystemResourceDefinitionRegistrar.REGISTRATION.getName(), DistributableEjbSubsystemModel.CURRENT,
                DistributableEjbSubsystemTransformationDescriptionFactory.INSTANCE);
    }
}

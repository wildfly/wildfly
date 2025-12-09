/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.resourceadapters;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.kohsuke.MetaInfServices;

import static org.jboss.as.connector.subsystems.resourceadapters.Constants.REPORT_DIRECTORY;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RESOURCEADAPTER_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY;
import static org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersExtension.VERSION_6_0_0;
import static org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersExtension.VERSION_6_1_0;

/**
 * Resource Adapters Transformers used to transform current model version to legacy model versions for domain mode.
 *
 * @author <a href="pberan@redhat.com">Petr Beran</a>
 */
@MetaInfServices(ExtensionTransformerRegistration.class)
public class ResourceAdaptersTransformers implements ExtensionTransformerRegistration {

    private static final ModelVersion VERSION_7_0_0 = ModelVersion.create(7, 0, 0);

    @Override
    public String getSubsystemName() {
        return ResourceAdaptersExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystemRegistration) {
        ModelVersion currentModel = subsystemRegistration.getCurrentSubsystemVersion();

        ChainedTransformationDescriptionBuilder chainedBuilder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(currentModel);

        //no transformation here - just XML parsing change
        chainedBuilder.createBuilder(subsystemRegistration.getCurrentSubsystemVersion(), VERSION_7_0_0).build();
        register700Transformers(chainedBuilder.createBuilder(VERSION_7_0_0, VERSION_6_1_0)); // 7.0.0 to 6.1.0 transformer
        register610Transformers(chainedBuilder.createBuilder(VERSION_6_1_0, VERSION_6_0_0)); // 6.1.0 to 6.0.0 transformer

        chainedBuilder.buildAndRegister(subsystemRegistration, new ModelVersion[] { VERSION_6_1_0, VERSION_6_0_0 });
    }

    private static void register700Transformers(ResourceTransformationDescriptionBuilder parentBuilder) {
        // 6.1.0 cannot resolve expressions introduced in 7.0.0 for WM_SECURITY
        parentBuilder.addChildResource(PathElement.pathElement(RESOURCEADAPTER_NAME)).getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, WM_SECURITY);
    }

    private static void register610Transformers(ResourceTransformationDescriptionBuilder parentBuilder) {
        // 6.0.0 doesn't contain the report-directory attribute introduced in 6.1.0
        parentBuilder.getAttributeBuilder().setDiscard(DiscardAttributeChecker.ALWAYS, REPORT_DIRECTORY);
    }
}

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.connector.subsystems.resourceadapters.Constants.REPORT_DIRECTORY_NAME;
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

    @Override
    public String getSubsystemName() {
        return ResourceAdaptersExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystemRegistration) {
        ModelVersion currentModel = subsystemRegistration.getCurrentSubsystemVersion();

        ChainedTransformationDescriptionBuilder chainedBuilder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(currentModel);

        register700Transformers(chainedBuilder.createBuilder(currentModel, VERSION_6_1_0)); // 7.0.0 to 6.1.0 transformer
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
        parentBuilder.getAttributeBuilder().setDiscard(DiscardAttributeChecker.ALWAYS, REPORT_DIRECTORY_NAME);
    }
}

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

package org.jboss.as.weld;

import java.util.Map;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.as.weld.logging.WeldLogger;
import org.jboss.dmr.ModelNode;

/**
 * @author Tomaz Cerar (c) 2017 Red Hat Inc.
 */
public class WeldTransformers implements ExtensionTransformerRegistration {
    @Override
    public String getSubsystemName() {
        return WeldExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystem) {
        ModelVersion version1_0_0 = ModelVersion.create(1, 0, 0);
        ModelVersion version3_0_0 = ModelVersion.create(3, 0, 0);

        ChainedTransformationDescriptionBuilder chainedBuilder = TransformationDescriptionBuilder.Factory
                .createChainedSubystemInstance(subsystem.getCurrentSubsystemVersion());

        // Differences between the current version and 3.0.0
        ResourceTransformationDescriptionBuilder builder300 = chainedBuilder.createBuilder(subsystem.getCurrentSubsystemVersion(), version3_0_0);
        builder300.getAttributeBuilder().setDiscard(DiscardAttributeChecker.UNDEFINED, WeldResourceDefinition.THREAD_POOL_SIZE_ATTRIBUTE)
                // Reject thread-pool-size attribute if defined
                .addRejectCheck(RejectAttributeChecker.DEFINED, WeldResourceDefinition.THREAD_POOL_SIZE_ATTRIBUTE).end();

        // Differences between 3.0.0 and 1.0.0
        ResourceTransformationDescriptionBuilder builder100 = chainedBuilder.createBuilder(version3_0_0, version1_0_0);
        builder100.getAttributeBuilder()
                // These new attributes are assumed to be 'true' in the old version but default to false in the current version. So discard if 'true' and reject
                // if 'undefined'.
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, false, new ModelNode(true)),
                        WeldResourceDefinition.NON_PORTABLE_MODE_ATTRIBUTE, WeldResourceDefinition.REQUIRE_BEAN_DESCRIPTOR_ATTRIBUTE)
                .addRejectCheck(new RejectAttributeChecker.DefaultRejectAttributeChecker() {

                    @Override
                    public String getRejectionLogMessage(Map<String, ModelNode> attributes) {
                        return WeldLogger.ROOT_LOGGER.rejectAttributesMustBeTrue(attributes.keySet());
                    }

                    @Override
                    protected boolean rejectAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
                        // This will not get called if it was discarded, so reject if it is undefined (default==false) or if defined and != 'true'
                        return !attributeValue.isDefined() || !attributeValue.asString().equals("true");
                    }
                }, WeldResourceDefinition.NON_PORTABLE_MODE_ATTRIBUTE, WeldResourceDefinition.REQUIRE_BEAN_DESCRIPTOR_ATTRIBUTE)

                // development mode - not supported in older versions
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(false)), WeldResourceDefinition.DEVELOPMENT_MODE_ATTRIBUTE)
                // if the attribute was not discarded it means that it is defined as 'true'. Therefore, reject.
                .addRejectCheck(RejectAttributeChecker.DEFINED, WeldResourceDefinition.DEVELOPMENT_MODE_ATTRIBUTE).end();

        chainedBuilder.buildAndRegister(subsystem, new ModelVersion[]{version1_0_0, version3_0_0});
    }
}

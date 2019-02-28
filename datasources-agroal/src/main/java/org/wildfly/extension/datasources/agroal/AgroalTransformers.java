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
package org.wildfly.extension.datasources.agroal;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.AttributeTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;

import static org.wildfly.extension.datasources.agroal.AgroalExtension.VERSION_1_0_0;
import static org.wildfly.extension.datasources.agroal.AgroalExtension.VERSION_2_0_0;
import static org.wildfly.extension.datasources.agroal.XADataSourceDefinition.*;

/**
 * Defines an extension to provide DataSources based on the Agroal project
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
public class AgroalTransformers implements ExtensionTransformerRegistration {

    @Override
    public String getSubsystemName() {
        return AgroalExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystemRegistration) {
        ChainedTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(subsystemRegistration.getCurrentSubsystemVersion());

        transformers_2_0_0(builder);

        builder.buildAndRegister(subsystemRegistration, new ModelVersion[] { VERSION_1_0_0, VERSION_2_0_0 });
    }

    private void transformers_2_0_0(ChainedTransformationDescriptionBuilder parentBuilder) {
        AttributeTransformationDescriptionBuilder attributeBuilder = parentBuilder.createBuilder(VERSION_2_0_0, VERSION_1_0_0).getAttributeBuilder();

        attributeBuilder.setDiscard(DiscardAttributeChecker.UNDEFINED, RECOVERY_USERNAME_ATTRIBUTE, RECOVERY_PASSWORD_ATTRIBUTE, RECOVERY_AUTHENTICATION_CONTEXT, RECOVERY_CREDENTIAL_REFERENCE);
        attributeBuilder.addRejectCheck(RejectAttributeChecker.DEFINED, RECOVERY_USERNAME_ATTRIBUTE, RECOVERY_PASSWORD_ATTRIBUTE, RECOVERY_AUTHENTICATION_CONTEXT, RECOVERY_CREDENTIAL_REFERENCE);
        attributeBuilder.addRejectCheck(new RejectAttributeChecker.SimpleAcceptAttributeChecker(ModelNode.FALSE), RECOVERY);
    }

}

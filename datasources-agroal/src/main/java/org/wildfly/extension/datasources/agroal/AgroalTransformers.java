/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.logging.ControllerLogger.ROOT_LOGGER;
import static org.jboss.as.controller.security.CredentialReference.CLEAR_TEXT;
import static org.jboss.as.controller.security.CredentialReference.STORE;
import static org.wildfly.extension.datasources.agroal.AbstractDataSourceDefinition.CONNECTION_FACTORY_ATTRIBUTE;
import static org.wildfly.extension.datasources.agroal.AbstractDataSourceDefinition.CREDENTIAL_REFERENCE;

import java.util.Map;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;

public class AgroalTransformers implements ExtensionTransformerRegistration {

    static final ModelVersion AGROAL_1_0 = ModelVersion.create(1, 0, 0);
    static final ModelVersion AGROAL_2_0 = ModelVersion.create(2, 0, 0);

    @Override
    public String getSubsystemName() {
        return AgroalExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration registration) {
        ChainedTransformationDescriptionBuilder chainedBuilder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(registration.getCurrentSubsystemVersion());

        // 2.0.0 (WildFly 18) to 1.0.0 (WildFly 17)
        from2(chainedBuilder);

        chainedBuilder.buildAndRegister(registration, new ModelVersion[] { AGROAL_1_0 });

    }

    private static void from2(ChainedTransformationDescriptionBuilder chainedBuilder) {
        ResourceTransformationDescriptionBuilder builder = chainedBuilder.createBuilder(AGROAL_2_0, AGROAL_1_0);

        ResourceTransformationDescriptionBuilder datasourceBuilder = builder.addChildResource(PathElement.pathElement("datasource"));
        datasourceBuilder
                .getAttributeBuilder()
                .addRejectCheck(REJECT_CREDENTIAL_REFERENCE_WITH_BOTH_STORE_AND_CLEAR_TEXT, CONNECTION_FACTORY_ATTRIBUTE)
                .end();
        ResourceTransformationDescriptionBuilder xaDatasourceBuilder = builder.addChildResource(PathElement.pathElement("xa-datasource"));
        xaDatasourceBuilder
                .getAttributeBuilder()
                .addRejectCheck(REJECT_CREDENTIAL_REFERENCE_WITH_BOTH_STORE_AND_CLEAR_TEXT, CONNECTION_FACTORY_ATTRIBUTE)
                .end();
    }

    private static final RejectAttributeChecker REJECT_CREDENTIAL_REFERENCE_WITH_BOTH_STORE_AND_CLEAR_TEXT = new RejectAttributeChecker.DefaultRejectAttributeChecker() {

        @Override
        public String getRejectionLogMessage(Map<String, ModelNode> attributes) {
            return ROOT_LOGGER.invalidAttributeValue(CLEAR_TEXT).getMessage();
        }

        @Override
        protected boolean rejectAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
            if (attributeValue.isDefined()
                    && attributeValue.hasDefined(CREDENTIAL_REFERENCE.getName())) {
                ModelNode credentialReference = attributeValue.get(CREDENTIAL_REFERENCE.getName());
                String store = null;
                String secret = null;
                if (credentialReference.hasDefined(STORE)) {
                    store = credentialReference.get(STORE).asString();
                }
                if (credentialReference.hasDefined(CLEAR_TEXT)) {
                    secret = credentialReference.get(CLEAR_TEXT).asString();
                }
                return store != null && secret != null;
            }
            return false;
        }
    };
}

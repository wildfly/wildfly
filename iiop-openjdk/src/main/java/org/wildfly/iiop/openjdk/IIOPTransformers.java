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

package org.wildfly.iiop.openjdk;

import static org.wildfly.iiop.openjdk.IIOPExtension.CURRENT_MODEL_VERSION;
import static org.wildfly.iiop.openjdk.IIOPExtension.VERSION_2;
import static org.wildfly.iiop.openjdk.IIOPExtension.VERSION_1;

import java.util.Map;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.wildfly.iiop.openjdk.logging.IIOPLogger;

/**
 * @author Tomaz Cerar (c) 2017 Red Hat Inc.
 */
public class IIOPTransformers implements ExtensionTransformerRegistration {

    @Override
    public String getSubsystemName() {
        return IIOPExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystemRegistration) {
        ChainedTransformationDescriptionBuilder chained = ResourceTransformationDescriptionBuilder.Factory.createChainedSubystemInstance(CURRENT_MODEL_VERSION);

        ResourceTransformationDescriptionBuilder builder_2_0 = chained.createBuilder(CURRENT_MODEL_VERSION, VERSION_2);
        builder_2_0.getAttributeBuilder()
                .setValueConverter(new AttributeConverter.DefaultValueAttributeConverter(IIOPRootDefinition.SOCKET_BINDING), IIOPRootDefinition.SOCKET_BINDING);

        /*
        --- Problems for relative address to root []:
        Missing attributes in current: []; missing in legacy [server-requires-ssl, server-ssl-context, client-requires-ssl, authentication-context, client-ssl-context]
        Different 'default' for attribute 'confidentiality'. Current: undefined; legacy: "none"
        Different 'default' for attribute 'detect-misordering'. Current: undefined; legacy: "none"
        Different 'default' for attribute 'detect-replay'. Current: undefined; legacy: "none"
        Different 'default' for attribute 'integrity'. Current: undefined; legacy: "none"
        Different 'default' for attribute 'ssl-socket-binding'. Current: undefined; legacy: "iiop-ssl"
        Different 'default' for attribute 'trust-in-client'. Current: undefined; legacy: "none"
        Different 'default' for attribute 'trust-in-target'. Current: undefined; legacy: "none"
        Missing parameters for operation 'add' in current: []; missing in legacy [server-requires-ssl, server-ssl-context, client-requires-ssl, authentication-context, client-ssl-context]
         */
        ResourceTransformationDescriptionBuilder builder_1_0 = chained.createBuilder(VERSION_2, VERSION_1);
        builder_1_0.getAttributeBuilder()
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(IIOPRootDefinition.CLIENT_REQUIRES_SSL.getDefaultValue()), IIOPRootDefinition.CLIENT_REQUIRES_SSL)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(IIOPRootDefinition.SERVER_REQUIRES_SSL.getDefaultValue()), IIOPRootDefinition.SERVER_REQUIRES_SSL)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(IIOPRootDefinition.INTEROP_IONA.getDefaultValue()), IIOPRootDefinition.INTEROP_IONA)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, IIOPRootDefinition.AUTHENTICATION_CONTEXT, IIOPRootDefinition.SERVER_SSL_CONTEXT, IIOPRootDefinition.CLIENT_SSL_CONTEXT)
                .addRejectCheck(new RejectAttributeChecker.DefaultRejectAttributeChecker() {
                    @Override
                    protected boolean rejectAttribute(PathAddress pathAddress, String s, ModelNode attributeValue, TransformationContext transformationContext) {
                        return attributeValue.asString().equalsIgnoreCase(Constants.ELYTRON);
                    }

                    @Override
                    public String getRejectionLogMessage(Map<String, ModelNode> map) {
                        return IIOPLogger.ROOT_LOGGER.elytronInitializerNotSupportedInPreviousVersions();
                    }
                }, IIOPRootDefinition.SECURITY)
                .addRejectCheck(RejectAttributeChecker.DEFINED, IIOPRootDefinition.SERVER_SSL_CONTEXT, IIOPRootDefinition.CLIENT_SSL_CONTEXT, IIOPRootDefinition.AUTHENTICATION_CONTEXT, IIOPRootDefinition.CLIENT_REQUIRES_SSL, IIOPRootDefinition.SERVER_REQUIRES_SSL, IIOPRootDefinition.INTEROP_IONA)
        ;

        chained.buildAndRegister(subsystemRegistration, new ModelVersion[]{
                VERSION_2,
                VERSION_1
        });
    }
}

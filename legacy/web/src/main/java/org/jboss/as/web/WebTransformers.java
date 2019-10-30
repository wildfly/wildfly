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

package org.jboss.as.web;

import static org.jboss.as.web.WebExtension.CONNECTOR_PATH;
import static org.jboss.as.web.WebExtension.HOST_PATH;
import static org.jboss.as.web.WebExtension.SSL_PATH;
import static org.jboss.as.web.WebExtension.SSO_PATH;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;

/**
 * @author Tomaz Cerar (c) 2017 Red Hat Inc.
 */
public class WebTransformers implements ExtensionTransformerRegistration {
    @Override
    public String getSubsystemName() {
        return WebExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystemRegistration) {
        registerTransformers_1_3_0(subsystemRegistration);
        registerTransformers_1_4_0(subsystemRegistration);
        registerTransformers_2_x_0(subsystemRegistration, 0);
        registerTransformers_2_x_0(subsystemRegistration, 1);
    }

    private void registerTransformers_1_3_0(SubsystemTransformerRegistration registration) {
        final ResourceTransformationDescriptionBuilder subsystemRoot = TransformationDescriptionBuilder.Factory.createSubsystemInstance();
        subsystemRoot.getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.DEFINED, WebDefinition.DEFAULT_SESSION_TIMEOUT)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, true, new ModelNode(30)), WebDefinition.DEFAULT_SESSION_TIMEOUT)
                .end();

        final ResourceTransformationDescriptionBuilder hostBuilder = subsystemRoot.addChildResource(HOST_PATH);
        final ResourceTransformationDescriptionBuilder ssoBuilder = hostBuilder.addChildResource(SSO_PATH);
        ssoBuilder.getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.DEFINED, WebSSODefinition.HTTP_ONLY)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, true, new ModelNode(true)), WebSSODefinition.HTTP_ONLY)
                .end();

        final ResourceTransformationDescriptionBuilder connectorBuilder = subsystemRoot.addChildResource(CONNECTOR_PATH);
        connectorBuilder.getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.DEFINED, WebConnectorDefinition.PROXY_BINDING, WebConnectorDefinition.REDIRECT_BINDING)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, WebSSLDefinition.SSL_PROTOCOL, WebConnectorDefinition.PROXY_BINDING, WebConnectorDefinition.REDIRECT_BINDING)
                .end();

        connectorBuilder.addChildResource(SSL_PATH).getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.UNDEFINED, WebSSLDefinition.CIPHER_SUITE)
                .end();

        TransformationDescription.Tools.register(subsystemRoot.build(), registration, ModelVersion.create(1, 3, 0));
    }

    private void registerTransformers_1_4_0(SubsystemTransformerRegistration registration) {
        final ResourceTransformationDescriptionBuilder subsystemRoot = TransformationDescriptionBuilder.Factory.createSubsystemInstance();
        subsystemRoot.getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.DEFINED, WebDefinition.DEFAULT_SESSION_TIMEOUT)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, true, new ModelNode(30)), WebDefinition.DEFAULT_SESSION_TIMEOUT)
                .end();

        final ResourceTransformationDescriptionBuilder hostBuilder = subsystemRoot.addChildResource(HOST_PATH);
        final ResourceTransformationDescriptionBuilder ssoBuilder = hostBuilder.addChildResource(SSO_PATH);
        ssoBuilder.getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.DEFINED, WebSSODefinition.HTTP_ONLY)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, true, new ModelNode(true)), WebSSODefinition.HTTP_ONLY)
                .end();

        TransformationDescription.Tools.register(subsystemRoot.build(), registration, ModelVersion.create(1, 4, 0));
    }

    //todo, could probably be removed as 2_x was never in EAP 6.x
    private void registerTransformers_2_x_0(SubsystemTransformerRegistration registration, int minor) {
        final ResourceTransformationDescriptionBuilder subsystemRoot = TransformationDescriptionBuilder.Factory.createSubsystemInstance();
        subsystemRoot.getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.DEFINED, WebDefinition.DEFAULT_SESSION_TIMEOUT)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, true, new ModelNode(30)), WebDefinition.DEFAULT_SESSION_TIMEOUT)
                .end();

        final ResourceTransformationDescriptionBuilder hostBuilder = subsystemRoot.addChildResource(HOST_PATH);
        final ResourceTransformationDescriptionBuilder ssoBuilder = hostBuilder.addChildResource(SSO_PATH);
        ssoBuilder.getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.DEFINED, WebSSODefinition.HTTP_ONLY)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, true, new ModelNode(true)), WebSSODefinition.HTTP_ONLY)
                .end();

        if (minor == 0) {
            final ResourceTransformationDescriptionBuilder connectorBuilder = subsystemRoot.addChildResource(CONNECTOR_PATH);
            connectorBuilder.getAttributeBuilder()
                    .addRejectCheck(RejectAttributeChecker.DEFINED, WebConnectorDefinition.PROXY_BINDING, WebConnectorDefinition.REDIRECT_BINDING)
                    .setDiscard(DiscardAttributeChecker.UNDEFINED, WebSSLDefinition.SSL_PROTOCOL, WebConnectorDefinition.PROXY_BINDING, WebConnectorDefinition.REDIRECT_BINDING)
                    .end();

            connectorBuilder.addChildResource(SSL_PATH).getAttributeBuilder()
                    .addRejectCheck(RejectAttributeChecker.UNDEFINED, WebSSLDefinition.CIPHER_SUITE)
                    .end();
        }

        TransformationDescription.Tools.register(subsystemRoot.build(), registration, ModelVersion.create(2, minor, 0));
    }
}

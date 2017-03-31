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

package org.wildfly.extension.undertow;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.undertow.filters.FailoverStrategy;


/**
 * @author Tomaz Cerar (c) 2016 Red Hat Inc.
 */
public class UndertowTransformers implements ExtensionTransformerRegistration {
    @Override
    public String getSubsystemName() {
        return UndertowExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystemRegistration) {
        registerTransformers_EAP_7_0_0(subsystemRegistration);
    }


    private static void registerTransformers_EAP_7_0_0(SubsystemTransformerRegistration subsystemRegistration) {
        final ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();

        // Version 4.0.0 adds the new SSL_CONTEXT attribute, however it is mutually exclusive to the SECURITY_REALM attribute, both of which can
        // now be set to 'undefined' so instead of rejecting a defined SSL_CONTEXT, reject an undefined SECURITY_REALM as that covers the
        // two new combinations.
        builder.addChildResource(UndertowExtension.HTTPS_LISTENER_PATH)
                .getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.UNDEFINED, Constants.SECURITY_REALM)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(false)), HttpListenerResourceDefinition.REQUIRE_HOST_HTTP11.getName())
                .addRejectCheck(RejectAttributeChecker.DEFINED, HttpListenerResourceDefinition.REQUIRE_HOST_HTTP11.getName())
                .end();

        builder.addChildResource(UndertowExtension.HTTP_LISTENER_PATH)
                .getAttributeBuilder()
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(false)), HttpListenerResourceDefinition.REQUIRE_HOST_HTTP11.getName())
                .addRejectCheck(RejectAttributeChecker.DEFINED, HttpListenerResourceDefinition.REQUIRE_HOST_HTTP11.getName())
                .end();

        builder.addChildResource(UndertowExtension.PATH_SERVLET_CONTAINER)
                .getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.DEFINED, Constants.DISABLE_FILE_WATCH_SERVICE)
                .end()
                .addChildResource(UndertowExtension.PATH_WEBSOCKETS)
                .getAttributeBuilder()
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(false)), Constants.PER_MESSAGE_DEFLATE)
                .addRejectCheck(RejectAttributeChecker.DEFINED, Constants.PER_MESSAGE_DEFLATE, Constants.DEFLATER_LEVEL)
                .end();

        builder.addChildResource(UndertowExtension.PATH_FILTERS)
                .addChildResource(PathElement.pathElement(Constants.MOD_CLUSTER))
                .getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.DEFINED, Constants.SSL_CONTEXT)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(1)), Constants.MAX_RETRIES)
                .addRejectCheck(RejectAttributeChecker.DEFINED, Constants.MAX_RETRIES)
                .addRejectCheck(RejectAttributeChecker.UNDEFINED, Constants.ADVERTISE_SOCKET_BINDING)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(FailoverStrategy.LOAD_BALANCED.name())), Constants.FAILOVER_STRATEGY)
                .addRejectCheck(RejectAttributeChecker.DEFINED, Constants.FAILOVER_STRATEGY)
                .end();

        builder.addChildResource(UndertowExtension.PATH_HANDLERS)
                .addChildResource(PathElement.pathElement(Constants.REVERSE_PROXY))
                .getAttributeBuilder()
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(1)), Constants.MAX_RETRIES)
                .addRejectCheck(RejectAttributeChecker.DEFINED, Constants.MAX_RETRIES)
                .end()
                .addChildResource(PathElement.pathElement(Constants.HOST))
                .getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.DEFINED, Constants.SSL_CONTEXT)
                .end();

        builder.discardChildResource(PathElement.pathElement(Constants.APPLICATION_SECURITY_DOMAIN));


        TransformationDescription.Tools.register(builder.build(), subsystemRegistration, UndertowExtension.MODEL_VERSION_EAP7_0_0);
    }
}

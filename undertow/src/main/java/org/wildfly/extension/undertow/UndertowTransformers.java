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

import static org.wildfly.extension.undertow.HttpsListenerResourceDefinition.SSL_CONTEXT;
import static org.wildfly.extension.undertow.ListenerResourceDefinition.RFC6265_COOKIE_VALIDATION;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.AttributeTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.undertow.filters.ModClusterDefinition;
import org.wildfly.extension.undertow.handlers.ReverseProxyHandler;


/**
 * @author Tomaz Cerar (c) 2016 Red Hat Inc.
 */
public class UndertowTransformers implements ExtensionTransformerRegistration {
    private static ModelVersion MODEL_VERSION_EAP7_0_0 = ModelVersion.create(3, 1, 0);
    private static ModelVersion MODEL_VERSION_EAP7_1_0 = ModelVersion.create(4, 0, 0);

    @Override
    public String getSubsystemName() {
        return UndertowExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystemRegistration) {
        registerTransformers_EAP_7_0_0(subsystemRegistration);
        registerTransformers_EAP_7_1_0(subsystemRegistration);
    }


    private static void registerTransformers_EAP_7_1_0(SubsystemTransformerRegistration subsystemRegistration) {
        final ResourceTransformationDescriptionBuilder subsystemBuilder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();
        subsystemBuilder
                .addChildResource(UndertowExtension.PATH_SERVLET_CONTAINER)
                .getAttributeBuilder()
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(10 * 1024 * 1024)), ServletContainerDefinition.FILE_CACHE_MAX_FILE_SIZE)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ServletContainerDefinition.FILE_CACHE_MAX_FILE_SIZE)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(100)), ServletContainerDefinition.FILE_CACHE_METADATA_SIZE)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ServletContainerDefinition.FILE_CACHE_METADATA_SIZE)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, ServletContainerDefinition.FILE_CACHE_TIME_TO_LIVE)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ServletContainerDefinition.FILE_CACHE_TIME_TO_LIVE)
                .end();

        final ResourceTransformationDescriptionBuilder serverBuilder = subsystemBuilder.addChildResource(UndertowExtension.SERVER_PATH);
        final AttributeTransformationDescriptionBuilder http = serverBuilder.addChildResource(UndertowExtension.HTTP_LISTENER_PATH).getAttributeBuilder();
        convertCommonListenerAttributes(http).end();
        final AttributeTransformationDescriptionBuilder https = serverBuilder.addChildResource(UndertowExtension.HTTPS_LISTENER_PATH).getAttributeBuilder();
        convertCommonListenerAttributes(https).end();
        final AttributeTransformationDescriptionBuilder ajp = serverBuilder.addChildResource(UndertowExtension.AJP_LISTENER_PATH).getAttributeBuilder();
        convertCommonListenerAttributes(ajp).end();

        TransformationDescription.Tools.register(subsystemBuilder.build(), subsystemRegistration, MODEL_VERSION_EAP7_1_0);
    }

    private static void registerTransformers_EAP_7_0_0(SubsystemTransformerRegistration subsystemRegistration) {
        final ResourceTransformationDescriptionBuilder subsystemBuilder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();
        final ResourceTransformationDescriptionBuilder serverBuilder = subsystemBuilder.addChildResource(UndertowExtension.SERVER_PATH);
        final ResourceTransformationDescriptionBuilder hostBuilder = serverBuilder.addChildResource(UndertowExtension.HOST_PATH);

        // Version 4.0.0 adds the new SSL_CONTEXT attribute, however it is mutually exclusive to the SECURITY_REALM attribute, both of which can
        // now be set to 'undefined' so instead of rejecting a defined SSL_CONTEXT, reject an undefined SECURITY_REALM as that covers the
        // two new combinations.
        DiscardAttributeChecker.DiscardAttributeValueChecker falseDiscardChecker = new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(false));
        AttributeTransformationDescriptionBuilder https =
                serverBuilder.addChildResource(UndertowExtension.HTTPS_LISTENER_PATH)
                        .getAttributeBuilder()
                        .addRejectCheck(RejectAttributeChecker.DEFINED, RFC6265_COOKIE_VALIDATION)
                        .setDiscard(falseDiscardChecker, RFC6265_COOKIE_VALIDATION)
                        .addRejectCheck(RejectAttributeChecker.DEFINED, SSL_CONTEXT)
                        .addRejectCheck(RejectAttributeChecker.UNDEFINED, Constants.SECURITY_REALM)
                        .setDiscard(DiscardAttributeChecker.UNDEFINED, SSL_CONTEXT)
                        .setDiscard(falseDiscardChecker, HttpListenerResourceDefinition.CERTIFICATE_FORWARDING)
                        .addRejectCheck(RejectAttributeChecker.DEFINED, HttpListenerResourceDefinition.CERTIFICATE_FORWARDING)
                        .setDiscard(falseDiscardChecker, HttpListenerResourceDefinition.PROXY_ADDRESS_FORWARDING)
                        .addRejectCheck(RejectAttributeChecker.DEFINED, HttpListenerResourceDefinition.PROXY_ADDRESS_FORWARDING);
        addCommonListenerRules(https).end();

        AttributeTransformationDescriptionBuilder http = serverBuilder.addChildResource(UndertowExtension.HTTP_LISTENER_PATH)
                .getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.DEFINED, RFC6265_COOKIE_VALIDATION)
                .setDiscard(falseDiscardChecker, RFC6265_COOKIE_VALIDATION);
        addCommonListenerRules(http).end();

        serverBuilder.addChildResource(UndertowExtension.AJP_LISTENER_PATH)
                .getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.DEFINED, RFC6265_COOKIE_VALIDATION)
                .setDiscard(falseDiscardChecker, RFC6265_COOKIE_VALIDATION)
        .end();


        subsystemBuilder
                .addChildResource(UndertowExtension.PATH_SERVLET_CONTAINER)
                .getAttributeBuilder()
                    .setDiscard(falseDiscardChecker, ServletContainerDefinition.DISABLE_FILE_WATCH_SERVICE)
                    .addRejectCheck(RejectAttributeChecker.DEFINED, ServletContainerDefinition.DISABLE_FILE_WATCH_SERVICE)
                    .setDiscard(falseDiscardChecker, ServletContainerDefinition.DISABLE_SESSION_ID_REUSE)
                    .addRejectCheck(RejectAttributeChecker.DEFINED, ServletContainerDefinition.DISABLE_SESSION_ID_REUSE)
                    .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(10 * 1024 * 1024)), ServletContainerDefinition.FILE_CACHE_MAX_FILE_SIZE)
                    .addRejectCheck(RejectAttributeChecker.DEFINED, ServletContainerDefinition.FILE_CACHE_MAX_FILE_SIZE)
                    .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(100)), ServletContainerDefinition.FILE_CACHE_METADATA_SIZE)
                    .addRejectCheck(RejectAttributeChecker.DEFINED, ServletContainerDefinition.FILE_CACHE_METADATA_SIZE)
                    .setDiscard(DiscardAttributeChecker.UNDEFINED, ServletContainerDefinition.FILE_CACHE_TIME_TO_LIVE)
                    .addRejectCheck(RejectAttributeChecker.DEFINED, ServletContainerDefinition.FILE_CACHE_TIME_TO_LIVE)
                .end()
                .addChildResource(UndertowExtension.PATH_WEBSOCKETS)
                .getAttributeBuilder()
                    .addRejectCheck(RejectAttributeChecker.DEFINED, Constants.PER_MESSAGE_DEFLATE, Constants.DEFLATER_LEVEL)
                    .setDiscard(falseDiscardChecker, WebsocketsDefinition.PER_MESSAGE_DEFLATE)
                    .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(0)), WebsocketsDefinition.DEFLATER_LEVEL)

                .end();

        subsystemBuilder.addChildResource(UndertowExtension.PATH_HANDLERS)
                .addChildResource(PathElement.pathElement(Constants.REVERSE_PROXY))
                .getAttributeBuilder()
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(1L)), Constants.MAX_RETRIES)
                .addRejectCheck(RejectAttributeChecker.DEFINED, Constants.MAX_RETRIES)
                .setValueConverter(new AttributeConverter.DefaultValueAttributeConverter(ReverseProxyHandler.CONNECTIONS_PER_THREAD), ReverseProxyHandler.CONNECTIONS_PER_THREAD)
                .end()
                .addChildResource(PathElement.pathElement(Constants.HOST))
                .getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.UNDEFINED, SSL_CONTEXT)
                .addRejectCheck(RejectAttributeChecker.DEFINED, Constants.SSL_CONTEXT)
                .setDiscard(DiscardAttributeChecker.ALWAYS, Constants.ENABLE_HTTP2) //we just discard, as older versions will just continue to use HTTP/1.1, and enabling this does not guarentee a HTTP/2 connection anyway (if the backend does not support it)
                .end();

        subsystemBuilder.addChildResource(UndertowExtension.PATH_FILTERS)
                .addChildResource(PathElement.pathElement(Constants.MOD_CLUSTER))
                .getAttributeBuilder()
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(ModClusterDefinition.FAILOVER_STRATEGY.getDefaultValue()), ModClusterDefinition.FAILOVER_STRATEGY)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(ModClusterDefinition.MAX_RETRIES.getDefaultValue()), ModClusterDefinition.MAX_RETRIES)
                .setValueConverter(new AttributeConverter.DefaultValueAttributeConverter(ModClusterDefinition.MAX_AJP_PACKET_SIZE), ModClusterDefinition.MAX_AJP_PACKET_SIZE)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, SSL_CONTEXT)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ModClusterDefinition.MAX_RETRIES, ModClusterDefinition.FAILOVER_STRATEGY)
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, ModClusterDefinition.MAX_AJP_PACKET_SIZE)
                .addRejectCheck(RejectAttributeChecker.DEFINED, SSL_CONTEXT)
                .addRejectCheck(RejectAttributeChecker.UNDEFINED, Constants.SECURITY_REALM)
                .end();

        hostBuilder.rejectChildResource(UndertowExtension.PATH_HTTP_INVOKER);
        subsystemBuilder.rejectChildResource(UndertowExtension.PATH_APPLICATION_SECURITY_DOMAIN);

        TransformationDescription.Tools.register(subsystemBuilder.build(), subsystemRegistration, MODEL_VERSION_EAP7_0_0);
    }

    private static AttributeTransformationDescriptionBuilder addCommonListenerRules(AttributeTransformationDescriptionBuilder builder) {
        return builder
                .addRejectCheck(new RejectAttributeChecker.SimpleRejectAttributeChecker(new ModelNode(true)), HttpListenerResourceDefinition.REQUIRE_HOST_HTTP11.getName())
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(false)), HttpListenerResourceDefinition.REQUIRE_HOST_HTTP11)
                .setValueConverter(new AttributeConverter.DefaultValueAttributeConverter(HttpListenerResourceDefinition.HTTP2_HEADER_TABLE_SIZE), HttpListenerResourceDefinition.HTTP2_HEADER_TABLE_SIZE)
                .setValueConverter(new AttributeConverter.DefaultValueAttributeConverter(HttpListenerResourceDefinition.HTTP2_INITIAL_WINDOW_SIZE), HttpListenerResourceDefinition.HTTP2_INITIAL_WINDOW_SIZE)
                .setValueConverter(new AttributeConverter.DefaultValueAttributeConverter(HttpListenerResourceDefinition.HTTP2_MAX_FRAME_SIZE), HttpListenerResourceDefinition.HTTP2_MAX_FRAME_SIZE)

                ;
    }

    private static AttributeTransformationDescriptionBuilder convertCommonListenerAttributes(AttributeTransformationDescriptionBuilder builder) {
        return builder.setValueConverter(new AttributeConverter.DefaultAttributeConverter() {
            @Override
            protected void convertAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
                if (attributeValue.isDefined() && attributeValue.asLong() == 0L) {
                    attributeValue.set(Long.MAX_VALUE);
                }
            }
        }, ListenerResourceDefinition.MAX_ENTITY_SIZE);
    }
}

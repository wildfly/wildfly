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

import static org.jboss.as.controller.security.CredentialReference.REJECT_CREDENTIAL_REFERENCE_WITH_BOTH_STORE_AND_CLEAR_TEXT;
import static org.wildfly.extension.undertow.ApplicationSecurityDomainDefinition.ENABLE_JASPI;
import static org.wildfly.extension.undertow.ApplicationSecurityDomainDefinition.INTEGRATED_JASPI;
import static org.wildfly.extension.undertow.ApplicationSecurityDomainDefinition.SECURITY_DOMAIN;
import static org.wildfly.extension.undertow.Constants.ENABLE_HTTP2;
import static org.wildfly.extension.undertow.HostDefinition.QUEUE_REQUESTS_ON_START;
import static org.wildfly.extension.undertow.HttpListenerResourceDefinition.CERTIFICATE_FORWARDING;
import static org.wildfly.extension.undertow.HttpListenerResourceDefinition.HTTP2_HEADER_TABLE_SIZE;
import static org.wildfly.extension.undertow.HttpListenerResourceDefinition.HTTP2_INITIAL_WINDOW_SIZE;
import static org.wildfly.extension.undertow.HttpListenerResourceDefinition.HTTP2_MAX_FRAME_SIZE;
import static org.wildfly.extension.undertow.HttpListenerResourceDefinition.PROXY_ADDRESS_FORWARDING;
import static org.wildfly.extension.undertow.HttpListenerResourceDefinition.PROXY_PROTOCOL;
import static org.wildfly.extension.undertow.HttpListenerResourceDefinition.REQUIRE_HOST_HTTP11;
import static org.wildfly.extension.undertow.HttpsListenerResourceDefinition.SSL_CONTEXT;
import static org.wildfly.extension.undertow.ListenerResourceDefinition.ALLOW_UNESCAPED_CHARACTERS_IN_URL;
import static org.wildfly.extension.undertow.ListenerResourceDefinition.RFC6265_COOKIE_VALIDATION;
import static org.wildfly.extension.undertow.ServletContainerDefinition.DEFAULT_COOKIE_VERSION;
import static org.wildfly.extension.undertow.ServletContainerDefinition.DISABLE_FILE_WATCH_SERVICE;
import static org.wildfly.extension.undertow.ServletContainerDefinition.DISABLE_SESSION_ID_REUSE;
import static org.wildfly.extension.undertow.ServletContainerDefinition.FILE_CACHE_MAX_FILE_SIZE;
import static org.wildfly.extension.undertow.ServletContainerDefinition.FILE_CACHE_METADATA_SIZE;
import static org.wildfly.extension.undertow.ServletContainerDefinition.FILE_CACHE_TIME_TO_LIVE;
import static org.wildfly.extension.undertow.ServletContainerDefinition.PRESERVE_PATH_ON_FORWARD;
import static org.wildfly.extension.undertow.WebsocketsDefinition.DEFLATER_LEVEL;
import static org.wildfly.extension.undertow.WebsocketsDefinition.PER_MESSAGE_DEFLATE;
import static org.wildfly.extension.undertow.filters.ModClusterDefinition.FAILOVER_STRATEGY;
import static org.wildfly.extension.undertow.filters.ModClusterDefinition.MAX_AJP_PACKET_SIZE;
import static org.wildfly.extension.undertow.handlers.ReverseProxyHandler.CONNECTIONS_PER_THREAD;
import static org.wildfly.extension.undertow.handlers.ReverseProxyHandler.MAX_RETRIES;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.AttributeConverter.DefaultValueAttributeConverter;
import org.jboss.as.controller.transform.description.AttributeTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker.SimpleRejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.undertow.filters.ModClusterDefinition;
import org.wildfly.extension.undertow.filters.NoAffinityResourceDefinition;
import org.wildfly.extension.undertow.filters.RankedAffinityResourceDefinition;
import org.wildfly.extension.undertow.filters.SingleAffinityResourceDefinition;


/**
 * @author Tomaz Cerar (c) 2016 Red Hat Inc.
 */
public class UndertowTransformers implements ExtensionTransformerRegistration {
    private static ModelVersion MODEL_VERSION_EAP7_0_0 = ModelVersion.create(3, 1, 0);
    private static ModelVersion MODEL_VERSION_EAP7_1_0 = ModelVersion.create(4, 0, 0);
    private static ModelVersion MODEL_VERSION_EAP7_2_0 = ModelVersion.create(7, 0, 0);
    private static final ModelVersion MODEL_VERSION_WILDFLY_16 = ModelVersion.create(8, 0, 0);
    private static final ModelVersion MODEL_VERSION_WILDFLY_18 = ModelVersion.create(10, 0, 0);

    @Override
    public String getSubsystemName() {
        return UndertowExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystemRegistration) {

        ChainedTransformationDescriptionBuilder chainedBuilder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(subsystemRegistration.getCurrentSubsystemVersion());

        registerTransformersWildFly18(chainedBuilder.createBuilder(subsystemRegistration.getCurrentSubsystemVersion(), MODEL_VERSION_WILDFLY_18));
        registerTransformersWildFly16(chainedBuilder.createBuilder(MODEL_VERSION_WILDFLY_18, MODEL_VERSION_WILDFLY_16));
        registerTransformers_EAP_7_2_0(chainedBuilder.createBuilder(MODEL_VERSION_WILDFLY_16, MODEL_VERSION_EAP7_2_0));
        registerTransformers_EAP_7_1_0(chainedBuilder.createBuilder(MODEL_VERSION_EAP7_2_0, MODEL_VERSION_EAP7_1_0));
        registerTransformers_EAP_7_0_0(chainedBuilder.createBuilder(MODEL_VERSION_EAP7_1_0, MODEL_VERSION_EAP7_0_0));

        chainedBuilder.buildAndRegister(subsystemRegistration, new ModelVersion[]{MODEL_VERSION_WILDFLY_18, MODEL_VERSION_WILDFLY_16, MODEL_VERSION_EAP7_2_0, MODEL_VERSION_EAP7_1_0, MODEL_VERSION_EAP7_0_0});
    }

    private static void registerTransformersWildFly18(ResourceTransformationDescriptionBuilder subsystemBuilder) {
        subsystemBuilder.addChildResource(UndertowExtension.PATH_APPLICATION_SECURITY_DOMAIN)
                .addChildResource(UndertowExtension.PATH_SSO)
                .getAttributeBuilder()
                    .addRejectCheck(REJECT_CREDENTIAL_REFERENCE_WITH_BOTH_STORE_AND_CLEAR_TEXT, ApplicationSecurityDomainSingleSignOnDefinition.Attribute.CREDENTIAL.getName())
                .end();
    }

    private static void registerTransformersWildFly16(ResourceTransformationDescriptionBuilder subsystemBuilder) {
        subsystemBuilder
                .addChildResource(UndertowExtension.SERVER_PATH)
                .addChildResource(UndertowExtension.HOST_PATH)
                .rejectChildResource(ConsoleAccessLogDefinition.INSTANCE.getPathElement());
    }

    private static void registerTransformers_EAP_7_2_0(ResourceTransformationDescriptionBuilder subsystemBuilder) {
        subsystemBuilder
                .addChildResource(UndertowExtension.PATH_APPLICATION_SECURITY_DOMAIN)
                .getAttributeBuilder()
                    .addRejectCheck(RejectAttributeChecker.DEFINED, ENABLE_JASPI, INTEGRATED_JASPI)
                    .setDiscard(DiscardAttributeChecker.ALWAYS, ENABLE_JASPI, INTEGRATED_JASPI) // Discard so we don't send over the defaults.
                .end();

        ResourceTransformationDescriptionBuilder builder = subsystemBuilder.addChildResource(UndertowExtension.PATH_FILTERS)
                .addChildResource(PathElement.pathElement(Constants.MOD_CLUSTER));

        builder.rejectChildResource(NoAffinityResourceDefinition.PATH);
        builder.discardChildResource(SingleAffinityResourceDefinition.PATH);
        builder.rejectChildResource(RankedAffinityResourceDefinition.PATH);

        subsystemBuilder
                .addChildResource(UndertowExtension.PATH_SERVLET_CONTAINER)
                .getAttributeBuilder()
                    .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, PRESERVE_PATH_ON_FORWARD)
                    .addRejectCheck(RejectAttributeChecker.DEFINED, PRESERVE_PATH_ON_FORWARD)
                .end();
    }

    private static void registerTransformers_EAP_7_1_0(ResourceTransformationDescriptionBuilder subsystemBuilder) {
        final ResourceTransformationDescriptionBuilder serverBuilder = subsystemBuilder.addChildResource(UndertowExtension.SERVER_PATH);
        final ResourceTransformationDescriptionBuilder hostBuilder = serverBuilder.addChildResource(UndertowExtension.HOST_PATH);
        subsystemBuilder
                .addChildResource(UndertowExtension.PATH_APPLICATION_SECURITY_DOMAIN)
                .getAttributeBuilder()
                    .addRejectCheck(RejectAttributeChecker.DEFINED, SECURITY_DOMAIN)
                .end();

        subsystemBuilder
                .addChildResource(UndertowExtension.PATH_SERVLET_CONTAINER)
                .getAttributeBuilder()
                    .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, FILE_CACHE_MAX_FILE_SIZE, FILE_CACHE_METADATA_SIZE, DEFAULT_COOKIE_VERSION)
                    .setDiscard(DiscardAttributeChecker.UNDEFINED, FILE_CACHE_TIME_TO_LIVE)
                    .addRejectCheck(RejectAttributeChecker.DEFINED,
                            FILE_CACHE_MAX_FILE_SIZE, FILE_CACHE_METADATA_SIZE, FILE_CACHE_TIME_TO_LIVE, DEFAULT_COOKIE_VERSION)
                .end();

        final AttributeTransformationDescriptionBuilder http = serverBuilder.addChildResource(UndertowExtension.HTTP_LISTENER_PATH).getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, PROXY_PROTOCOL)
                .addRejectCheck(new SimpleRejectAttributeChecker(ModelNode.TRUE), PROXY_PROTOCOL.getName());
        addCommonListenerRules_EAP_7_1_0(http);

        final AttributeTransformationDescriptionBuilder https = serverBuilder.addChildResource(UndertowExtension.HTTPS_LISTENER_PATH).getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, PROXY_PROTOCOL)
                .addRejectCheck(new SimpleRejectAttributeChecker(ModelNode.TRUE), PROXY_PROTOCOL);
        addCommonListenerRules_EAP_7_1_0(https);

        final AttributeTransformationDescriptionBuilder ajp = serverBuilder.addChildResource(UndertowExtension.AJP_LISTENER_PATH).getAttributeBuilder();
        addCommonListenerRules_EAP_7_1_0(ajp);

        hostBuilder.getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, QUEUE_REQUESTS_ON_START)
                .addRejectCheck(RejectAttributeChecker.DEFINED, QUEUE_REQUESTS_ON_START)
                .end();
        subsystemBuilder.rejectChildResource(UndertowExtension.BYTE_BUFFER_POOL_PATH);
    }

    private static void addCommonListenerRules_EAP_7_1_0(AttributeTransformationDescriptionBuilder listener) {
        convertCommonListenerAttributes(listener);
        listener
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, ALLOW_UNESCAPED_CHARACTERS_IN_URL)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ALLOW_UNESCAPED_CHARACTERS_IN_URL);
    }

    private static void registerTransformers_EAP_7_0_0(ResourceTransformationDescriptionBuilder subsystemBuilder) {
        final ResourceTransformationDescriptionBuilder serverBuilder = subsystemBuilder.addChildResource(UndertowExtension.SERVER_PATH);
        final ResourceTransformationDescriptionBuilder hostBuilder = serverBuilder.addChildResource(UndertowExtension.HOST_PATH);

        // Version 4.0.0 adds the new SSL_CONTEXT attribute, however it is mutually exclusive to the SECURITY_REALM attribute, both of which can
        // now be set to 'undefined' so instead of rejecting a defined SSL_CONTEXT, reject an undefined SECURITY_REALM as that covers the
        // two new combinations.
        AttributeTransformationDescriptionBuilder https =
                serverBuilder.addChildResource(UndertowExtension.HTTPS_LISTENER_PATH)
                        .getAttributeBuilder()
                        .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, CERTIFICATE_FORWARDING, PROXY_ADDRESS_FORWARDING)
                        .setDiscard(DiscardAttributeChecker.UNDEFINED, SSL_CONTEXT)
                        .addRejectCheck(RejectAttributeChecker.DEFINED,
                                CERTIFICATE_FORWARDING, PROXY_ADDRESS_FORWARDING, SSL_CONTEXT)
                        .addRejectCheck(RejectAttributeChecker.UNDEFINED, Constants.SECURITY_REALM);
        addCommonListenerRules_EAP_7_0_0(https).end();

        AttributeTransformationDescriptionBuilder http =
                serverBuilder.addChildResource(UndertowExtension.HTTP_LISTENER_PATH).getAttributeBuilder();
        addCommonListenerRules_EAP_7_0_0(http);
        http.end();

        serverBuilder.addChildResource(UndertowExtension.AJP_LISTENER_PATH)
                .getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, RFC6265_COOKIE_VALIDATION)
                .addRejectCheck(RejectAttributeChecker.DEFINED, RFC6265_COOKIE_VALIDATION)
        .end();


        subsystemBuilder
                .addChildResource(UndertowExtension.PATH_SERVLET_CONTAINER)
                .getAttributeBuilder()
                    .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, DISABLE_FILE_WATCH_SERVICE, DISABLE_SESSION_ID_REUSE)
                    .addRejectCheck(RejectAttributeChecker.DEFINED, DISABLE_FILE_WATCH_SERVICE, DISABLE_SESSION_ID_REUSE)
                .end()
                .addChildResource(UndertowExtension.PATH_WEBSOCKETS)
                .getAttributeBuilder()
                    .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, PER_MESSAGE_DEFLATE, DEFLATER_LEVEL)
                    .addRejectCheck(RejectAttributeChecker.DEFINED, PER_MESSAGE_DEFLATE, DEFLATER_LEVEL)
                .end();

        subsystemBuilder.addChildResource(UndertowExtension.PATH_HANDLERS)
                .addChildResource(PathElement.pathElement(Constants.REVERSE_PROXY))
                .getAttributeBuilder()
                    .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, MAX_RETRIES)
                    .addRejectCheck(RejectAttributeChecker.DEFINED, MAX_RETRIES)
                    .setValueConverter(new DefaultValueAttributeConverter(CONNECTIONS_PER_THREAD), CONNECTIONS_PER_THREAD)
                .end()
                .addChildResource(PathElement.pathElement(Constants.HOST))
                .getAttributeBuilder()
                    .setDiscard(DiscardAttributeChecker.UNDEFINED, SSL_CONTEXT)
                    .setDiscard(DiscardAttributeChecker.ALWAYS, ENABLE_HTTP2) //we just discard, as older versions will just continue to use HTTP/1.1, and enabling this does not guarentee a HTTP/2 connection anyway (if the backend does not support it)
                    .addRejectCheck(RejectAttributeChecker.DEFINED, SSL_CONTEXT)
                .end();

        subsystemBuilder.addChildResource(UndertowExtension.PATH_FILTERS)
                .addChildResource(PathElement.pathElement(Constants.MOD_CLUSTER))
                .getAttributeBuilder()
                    .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, FAILOVER_STRATEGY, ModClusterDefinition.MAX_RETRIES)
                    .setDiscard(DiscardAttributeChecker.UNDEFINED, SSL_CONTEXT)
                    .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, MAX_AJP_PACKET_SIZE)
                    .addRejectCheck(RejectAttributeChecker.DEFINED,
                            ModClusterDefinition.MAX_RETRIES, FAILOVER_STRATEGY, SSL_CONTEXT)
                    .addRejectCheck(RejectAttributeChecker.UNDEFINED, Constants.SECURITY_REALM)
                    .setValueConverter(new DefaultValueAttributeConverter(MAX_AJP_PACKET_SIZE), MAX_AJP_PACKET_SIZE)
                .end();

        hostBuilder.rejectChildResource(UndertowExtension.PATH_HTTP_INVOKER);

        subsystemBuilder.rejectChildResource(UndertowExtension.PATH_APPLICATION_SECURITY_DOMAIN);
    }

    private static AttributeTransformationDescriptionBuilder addCommonListenerRules_EAP_7_0_0(AttributeTransformationDescriptionBuilder builder) {
        return builder
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, REQUIRE_HOST_HTTP11, RFC6265_COOKIE_VALIDATION)
                .addRejectCheck(RejectAttributeChecker.DEFINED, REQUIRE_HOST_HTTP11, RFC6265_COOKIE_VALIDATION)
                .setValueConverter(new DefaultValueAttributeConverter(HTTP2_HEADER_TABLE_SIZE), HTTP2_HEADER_TABLE_SIZE)
                .setValueConverter(new DefaultValueAttributeConverter(HTTP2_INITIAL_WINDOW_SIZE), HTTP2_INITIAL_WINDOW_SIZE)
                .setValueConverter(new DefaultValueAttributeConverter(HTTP2_MAX_FRAME_SIZE), HTTP2_MAX_FRAME_SIZE);
    }

    private static AttributeTransformationDescriptionBuilder convertCommonListenerAttributes(AttributeTransformationDescriptionBuilder builder) {
        builder.setValueConverter(new AttributeConverter.DefaultAttributeConverter() {
            @Override
            protected void convertAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
                if (attributeValue.isDefined() && attributeValue.asLong() == 0L) {
                    attributeValue.set(Long.MAX_VALUE);
                }
            }
        }, ListenerResourceDefinition.MAX_ENTITY_SIZE);

        return builder;
    }
}

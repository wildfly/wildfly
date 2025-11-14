/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import java.util.EnumSet;
import java.util.Set;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.AttributeTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.kohsuke.MetaInfServices;
import org.wildfly.extension.undertow.handlers.HandlerDefinitions;
import org.wildfly.extension.undertow.handlers.ReverseProxyHandlerDefinition;

/**
 * Registers transformers for the Undertow subsystem.
 * @author Paul Ferraro
 */
@MetaInfServices
public class UndertowExtensionTransformerRegistration implements ExtensionTransformerRegistration {

    @Override
    public String getSubsystemName() {
        return UndertowExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration registration) {
        for (UndertowSubsystemModel model : EnumSet.complementOf(EnumSet.of(UndertowSubsystemModel.CURRENT))) {
            ModelVersion version = model.getVersion();
            ResourceTransformationDescriptionBuilder subsystem = TransformationDescriptionBuilder.Factory.createSubsystemInstance();

            ResourceTransformationDescriptionBuilder server = subsystem.addChildResource(ServerDefinition.PATH_ELEMENT);
            for (PathElement listenerPath : Set.of(HttpListenerResourceDefinition.PATH_ELEMENT, HttpsListenerResourceDefinition.PATH_ELEMENT)) {
                if (UndertowSubsystemModel.VERSION_13_0_0.requiresTransformation(version)) {
                    server.addChildResource(listenerPath).getAttributeBuilder()
                        .setValueConverter(AttributeConverter.DEFAULT_VALUE, ListenerResourceDefinition.WRITE_TIMEOUT, ListenerResourceDefinition.READ_TIMEOUT)
                        .end();
                }
            }

            if (UndertowSubsystemModel.VERSION_15_0_0.requiresTransformation(version)) {
                final ResourceTransformationDescriptionBuilder servletContainer = subsystem.addChildResource(ServletContainerDefinition.PATH_ELEMENT);
                final AttributeTransformationDescriptionBuilder sevletContainerAttribyteDescriptionBuilder = servletContainer.getAttributeBuilder();
                sevletContainerAttribyteDescriptionBuilder
                    .setDiscard(DiscardAttributeChecker.UNDEFINED, ServletContainerDefinition.DEFAULT_ASYNC_CONTEXT_TIMEOUT)
                    .addRejectCheck(RejectAttributeChecker.DEFINED, ServletContainerDefinition.DEFAULT_ASYNC_CONTEXT_TIMEOUT);


                if (UndertowSubsystemModel.VERSION_14_0_0.requiresTransformation(version)) {
                    final ResourceTransformationDescriptionBuilder handlers = subsystem.addChildResource(HandlerDefinitions.PATH_ELEMENT);
                    final ResourceTransformationDescriptionBuilder reverseProxy = handlers.addChildResource(ReverseProxyHandlerDefinition.PATH_ELEMENT);
                    final AttributeTransformationDescriptionBuilder reverseProxyAttributeTransformationDescriptionBuilder = reverseProxy.getAttributeBuilder();
                    final ResourceTransformationDescriptionBuilder ajpListener = server.addChildResource(AjpListenerResourceDefinition.PATH_ELEMENT);

                    reverseProxyAttributeTransformationDescriptionBuilder.setDiscard(DiscardAttributeChecker.UNDEFINED, ReverseProxyHandlerDefinition.REUSE_X_FORWARDED_HEADER)
                    .addRejectCheck(RejectAttributeChecker.DEFINED, ReverseProxyHandlerDefinition.REUSE_X_FORWARDED_HEADER)
                    .setDiscard(DiscardAttributeChecker.UNDEFINED, ReverseProxyHandlerDefinition.REWRITE_HOST_HEADER)
                    .addRejectCheck(RejectAttributeChecker.DEFINED, ReverseProxyHandlerDefinition.REWRITE_HOST_HEADER)
                    .end();

                    final AttributeTransformationDescriptionBuilder ajpListenerAttributeTransformationDescriptionBuilder = ajpListener.getAttributeBuilder();
                    ajpListenerAttributeTransformationDescriptionBuilder.setDiscard(DiscardAttributeChecker.UNDEFINED, AjpListenerResourceDefinition.ALLOWED_REQUEST_ATTRIBUTES_PATTERN)
                    .addRejectCheck(RejectAttributeChecker.DEFINED, AjpListenerResourceDefinition.ALLOWED_REQUEST_ATTRIBUTES_PATTERN)
                    .end();


                    if (UndertowSubsystemModel.VERSION_13_0_0.requiresTransformation(version)) {
                        sevletContainerAttribyteDescriptionBuilder
                            .setDiscard(DiscardAttributeChecker.UNDEFINED, ServletContainerDefinition.ORPHAN_SESSION_ALLOWED)
                            .addRejectCheck(RejectAttributeChecker.DEFINED, ServletContainerDefinition.ORPHAN_SESSION_ALLOWED);

                        servletContainer.rejectChildResource(AffinityCookieDefinition.PATH_ELEMENT);

                        ajpListenerAttributeTransformationDescriptionBuilder
                        .setValueConverter(AttributeConverter.DEFAULT_VALUE, ListenerResourceDefinition.WRITE_TIMEOUT, ListenerResourceDefinition.READ_TIMEOUT)
                        .end();
                    }
                }
                sevletContainerAttribyteDescriptionBuilder.end();
            }
            TransformationDescription.Tools.register(subsystem.build(), registration, version);
        }
    }
}

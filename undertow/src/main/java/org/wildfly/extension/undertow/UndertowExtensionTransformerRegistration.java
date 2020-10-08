/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import static org.wildfly.extension.undertow.AccessLogDefinition.CLOSE_RETRY_COUNT;
import static org.wildfly.extension.undertow.AccessLogDefinition.CLOSE_RETRY_DELAY;

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
import org.jboss.as.controller.transform.description.RejectAttributeChecker.SimpleRejectAttributeChecker;
import org.jboss.dmr.ModelNode;
import org.kohsuke.MetaInfServices;

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
            for (PathElement listenerPath : Set.of(HttpListenerResourceDefinition.PATH_ELEMENT, HttpsListenerResourceDefinition.PATH_ELEMENT, AjpListenerResourceDefinition.PATH_ELEMENT)) {
                if (UndertowSubsystemModel.VERSION_13_0_0.requiresTransformation(version)) {
                    server.addChildResource(listenerPath).getAttributeBuilder()
                        .setValueConverter(AttributeConverter.DEFAULT_VALUE, ListenerResourceDefinition.WRITE_TIMEOUT, ListenerResourceDefinition.READ_TIMEOUT)
                        .end();
                }
            }

            ResourceTransformationDescriptionBuilder servletContainer = subsystem.addChildResource(ServletContainerDefinition.PATH_ELEMENT);
            if (UndertowSubsystemModel.VERSION_13_0_0.requiresTransformation(version)) {
                servletContainer.getAttributeBuilder()
                    .setDiscard(DiscardAttributeChecker.UNDEFINED, ServletContainerDefinition.ORPHAN_SESSION_ALLOWED)
                    .addRejectCheck(RejectAttributeChecker.DEFINED, ServletContainerDefinition.ORPHAN_SESSION_ALLOWED)
                    .end();

                servletContainer.rejectChildResource(AffinityCookieDefinition.PATH_ELEMENT);
            }

            if(UndertowSubsystemModel.VERSION_14_0_0.requiresTransformation(version)) {
                final AttributeTransformationDescriptionBuilder accessLogAttributeBuilder = server.addChildResource(HostDefinition.PATH_ELEMENT).addChildResource(AccessLogDefinition.PATH_ELEMENT).getAttributeBuilder()
                        .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, CLOSE_RETRY_COUNT)
                        .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, CLOSE_RETRY_DELAY)
                        .addRejectCheck(new SimpleRejectAttributeChecker(ModelNode.TRUE), CLOSE_RETRY_COUNT.getName())
                        .addRejectCheck(new SimpleRejectAttributeChecker(ModelNode.TRUE), CLOSE_RETRY_DELAY.getName());
                accessLogAttributeBuilder.end();
            }

            TransformationDescription.Tools.register(subsystem.build(), registration, version);
        }
    }
}

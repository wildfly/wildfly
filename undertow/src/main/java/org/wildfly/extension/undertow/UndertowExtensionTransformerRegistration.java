/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
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

import java.util.EnumSet;
import java.util.Set;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
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

            TransformationDescription.Tools.register(subsystem.build(), registration, version);
        }
    }
}

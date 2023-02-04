/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

import static org.wildfly.extension.undertow.ListenerResourceDefinition.READ_TIMEOUT;
import static org.wildfly.extension.undertow.ListenerResourceDefinition.WRITE_TIMEOUT;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.AttributeTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker.SimpleRejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class UndertowTransformers implements ExtensionTransformerRegistration {
    private static final ModelVersion MODEL_VERSION_WILDFLY_23 = ModelVersion.create(12, 0, 0);

    @Override
    public String getSubsystemName() {
        return UndertowExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystemRegistration) {

        ChainedTransformationDescriptionBuilder chainedBuilder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(subsystemRegistration.getCurrentSubsystemVersion());

        registerTransformersWildFly23(chainedBuilder.createBuilder(subsystemRegistration.getCurrentSubsystemVersion(), MODEL_VERSION_WILDFLY_23));

        chainedBuilder.buildAndRegister(subsystemRegistration, new ModelVersion[]{MODEL_VERSION_WILDFLY_23});
    }

    private static void registerTransformersWildFly23(ResourceTransformationDescriptionBuilder subsystemBuilder) {
        final ResourceTransformationDescriptionBuilder serverBuilder = subsystemBuilder.addChildResource(ServerDefinition.PATH_ELEMENT);

        final AttributeTransformationDescriptionBuilder http = serverBuilder.addChildResource(HttpListenerResourceDefinition.PATH_ELEMENT).getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, WRITE_TIMEOUT)
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, READ_TIMEOUT)
                .addRejectCheck(new SimpleRejectAttributeChecker(ModelNode.TRUE), WRITE_TIMEOUT.getName())
                .addRejectCheck(new SimpleRejectAttributeChecker(ModelNode.TRUE), READ_TIMEOUT.getName());
        http.end();

        final AttributeTransformationDescriptionBuilder https = serverBuilder.addChildResource(HttpsListenerResourceDefinition.PATH_ELEMENT).getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, WRITE_TIMEOUT)
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, READ_TIMEOUT)
                .addRejectCheck(new SimpleRejectAttributeChecker(ModelNode.TRUE), WRITE_TIMEOUT.getName())
                .addRejectCheck(new SimpleRejectAttributeChecker(ModelNode.TRUE), READ_TIMEOUT.getName());
        https.end();

        final AttributeTransformationDescriptionBuilder ajp = serverBuilder.addChildResource(AjpListenerResourceDefinition.PATH_ELEMENT).getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, WRITE_TIMEOUT)
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, READ_TIMEOUT)
                .addRejectCheck(new SimpleRejectAttributeChecker(ModelNode.TRUE), WRITE_TIMEOUT.getName())
                .addRejectCheck(new SimpleRejectAttributeChecker(ModelNode.TRUE), READ_TIMEOUT.getName());
        ajp.end();
    }

}

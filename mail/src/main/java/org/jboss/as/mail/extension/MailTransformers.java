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

package org.jboss.as.mail.extension;

import static org.jboss.as.mail.extension.MailExtension.CURRENT_MODEL_VERSION;
import static org.jboss.as.mail.extension.MailExtension.MAIL_SESSION_PATH;
import static org.jboss.as.mail.extension.MailSubsystemModel.CUSTOM_SERVER_PATH;
import static org.jboss.as.mail.extension.MailSubsystemModel.SERVER_TYPE;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * @author Tomaz Cerar (c) 2017 Red Hat Inc.
 */
public class MailTransformers implements ExtensionTransformerRegistration {
    static final ModelVersion MODEL_VERSION_EAP6X = ModelVersion.create(1, 3, 0); //EAP6.2,6.3 & 6.4 have version 1.3.0
    static final ModelVersion MODEL_VERSION_EAP70 = ModelVersion.create(2, 0, 0);

    @Override
    public String getSubsystemName() {
        return MailExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystem) {
        ChainedTransformationDescriptionBuilder chained = ResourceTransformationDescriptionBuilder.Factory.createChainedSubystemInstance(CURRENT_MODEL_VERSION);


        ResourceTransformationDescriptionBuilder builder70 = chained.createBuilder(CURRENT_MODEL_VERSION, MODEL_VERSION_EAP70);
        ResourceTransformationDescriptionBuilder sessionBuilder70 = builder70.addChildResource(MAIL_SESSION_PATH);
        sessionBuilder70.addChildResource(PathElement.pathElement(SERVER_TYPE))
                    .getAttributeBuilder()
                    .addRejectCheck(RejectAttributeChecker.DEFINED, MailServerDefinition.CREDENTIAL_REFERENCE)
                    .setDiscard(DiscardAttributeChecker.UNDEFINED, MailServerDefinition.CREDENTIAL_REFERENCE)
                    .end();
        sessionBuilder70.addChildResource(CUSTOM_SERVER_PATH)
                    .getAttributeBuilder()
                    .addRejectCheck(RejectAttributeChecker.DEFINED, MailServerDefinition.CREDENTIAL_REFERENCE)
                    .setDiscard(DiscardAttributeChecker.UNDEFINED, MailServerDefinition.CREDENTIAL_REFERENCE)
                    .end();

        //there are no differences between EAP6.2 --> EAP7
        chained.createBuilder(MODEL_VERSION_EAP70, MODEL_VERSION_EAP6X);


        chained.buildAndRegister(subsystem, new ModelVersion[]{MODEL_VERSION_EAP70, MODEL_VERSION_EAP6X});
    }
}

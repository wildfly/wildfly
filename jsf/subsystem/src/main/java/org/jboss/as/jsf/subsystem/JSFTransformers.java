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

package org.jboss.as.jsf.subsystem;

import static org.jboss.as.jsf.subsystem.JSFResourceDefinition.DISALLOW_DOCTYPE_DECL;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;

/**
 * @author <a href="fjuma@redhat.com">Farah Juma</a>
 */
public class JSFTransformers implements ExtensionTransformerRegistration {

    private static final ModelVersion VERSION_1_0 = ModelVersion.create(1, 0);

    @Override
    public String getSubsystemName() {
        return JSFExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystemRegistration) {
        final ChainedTransformationDescriptionBuilder chainedBuilder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(subsystemRegistration.getCurrentSubsystemVersion());
        chainedBuilder.createBuilder(subsystemRegistration.getCurrentSubsystemVersion(), VERSION_1_0)
                .getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.UNDEFINED, DISALLOW_DOCTYPE_DECL)
                .addRejectCheck(RejectAttributeChecker.DEFINED, DISALLOW_DOCTYPE_DECL)
                .end();
        chainedBuilder.buildAndRegister(subsystemRegistration, new ModelVersion[]{VERSION_1_0});
    }

}

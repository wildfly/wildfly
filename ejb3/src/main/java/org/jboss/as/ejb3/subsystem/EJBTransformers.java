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

package org.jboss.as.ejb3.subsystem;

import static org.jboss.as.ejb3.subsystem.EJB3Model.VERSION_9_0_0;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.kohsuke.MetaInfServices;

/**
 * Jakarta Enterprise Beans Transformers used to transform current model version to legacy model versions for domain mode.
 *
 * @author Tomaz Cerar (c) 2017 Red Hat Inc.
 * @author Richard Achmatowicz (c) 2020 Red Hat Inc.
 */
@MetaInfServices(ExtensionTransformerRegistration.class)
public class EJBTransformers implements ExtensionTransformerRegistration {

    @Override
    public String getSubsystemName() {
        return EJB3Extension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystemRegistration) {

        ModelVersion currentModel = subsystemRegistration.getCurrentSubsystemVersion();
        ChainedTransformationDescriptionBuilder chainedBuilder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(currentModel);

        // register the transformations required for each legacy version after 9.0.0
        registerTransformers_9_0_0(chainedBuilder.createBuilder(currentModel, VERSION_9_0_0.getVersion()));

        // create the chained builder which incorporates all transformations
        chainedBuilder.buildAndRegister(subsystemRegistration, new ModelVersion[] {
                VERSION_9_0_0.getVersion() });
    }

    /*
     * Transformers for changes in model version 10.0.0
     */
    private static void registerTransformers_9_0_0(ResourceTransformationDescriptionBuilder subsystemBuilder) {
        // Reject ejb3/caches/simple-cache element
        subsystemBuilder.rejectChildResource(EJB3SubsystemModel.SIMPLE_CACHE_PATH);
        // Reject ejb3/caches/distributable-cache element
        subsystemBuilder.rejectChildResource(EJB3SubsystemModel.DISTRIBUTABLE_CACHE_PATH);

        subsystemBuilder.addChildResource(EJB3SubsystemModel.TIMER_SERVICE_PATH).getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.UNDEFINED, TimerServiceResourceDefinition.DEFAULT_PERSISTENT_TIMER_MANAGEMENT, TimerServiceResourceDefinition.DEFAULT_TRANSIENT_TIMER_MANAGEMENT)
                .addRejectCheck(RejectAttributeChecker.DEFINED, TimerServiceResourceDefinition.DEFAULT_PERSISTENT_TIMER_MANAGEMENT, TimerServiceResourceDefinition.DEFAULT_TRANSIENT_TIMER_MANAGEMENT)
                .addRejectCheck(RejectAttributeChecker.UNDEFINED, TimerServiceResourceDefinition.THREAD_POOL_NAME, TimerServiceResourceDefinition.DEFAULT_DATA_STORE)
                .end();
    }
}

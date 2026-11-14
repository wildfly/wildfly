/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import static org.jboss.as.ejb3.subsystem.EJB3Model.VERSION_10_0_0;
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

        // register the transformations required for each legacy version
        registerTransformers_10_0_0(chainedBuilder.createBuilder(currentModel, VERSION_10_0_0.getVersion()));
        registerTransformers_9_0_0(chainedBuilder.createBuilder(VERSION_10_0_0.getVersion(), VERSION_9_0_0.getVersion()));

        // create the chained builder which incorporates all transformations
        chainedBuilder.buildAndRegister(subsystemRegistration, new ModelVersion[] {
                VERSION_10_0_0.getVersion(),
                VERSION_9_0_0.getVersion()
        });
    }

    /*
     * Transformers for changes in model version 11.0.0 (current).
     * Rejects resources introduced in 11.0.0 when transforming to 10.0.0.
     */
    private static void registerTransformers_10_0_0(ResourceTransformationDescriptionBuilder subsystemBuilder) {
        // Reject /subsystem=ejb3/service=access-log — no equivalent in model 10.0.0
        subsystemBuilder.rejectChildResource(EJB3SubsystemModel.ACCESS_LOG_PATH);
    }

    /*
     * Transformers for changes introduced between 10.0.0 and 9.0.0.
     */
    private static void registerTransformers_9_0_0(ResourceTransformationDescriptionBuilder subsystemBuilder) {
        // Reject ejb3/caches/simple-cache resource
        subsystemBuilder.rejectChildResource(EJB3SubsystemModel.SIMPLE_CACHE_PATH);

        // Reject ejb3/caches/distributable-cache resource
        subsystemBuilder.rejectChildResource(EJB3SubsystemModel.DISTRIBUTABLE_CACHE_PATH);

        subsystemBuilder.addChildResource(EJB3SubsystemModel.TIMER_SERVICE_PATH).getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.UNDEFINED, TimerServiceResourceDefinition.DEFAULT_PERSISTENT_TIMER_MANAGEMENT, TimerServiceResourceDefinition.DEFAULT_TRANSIENT_TIMER_MANAGEMENT)
                .addRejectCheck(RejectAttributeChecker.DEFINED, TimerServiceResourceDefinition.DEFAULT_PERSISTENT_TIMER_MANAGEMENT, TimerServiceResourceDefinition.DEFAULT_TRANSIENT_TIMER_MANAGEMENT)
                .addRejectCheck(RejectAttributeChecker.UNDEFINED, TimerServiceResourceDefinition.THREAD_POOL_NAME, TimerServiceResourceDefinition.DEFAULT_DATA_STORE)
                .end();
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.compensations;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.jandex.DotName;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.narayana.compensations.api.CancelOnFailure;
import org.jboss.narayana.compensations.api.Compensatable;
import org.jboss.narayana.compensations.api.CompensationScoped;
import org.jboss.narayana.compensations.api.TxCompensate;
import org.jboss.narayana.compensations.api.TxConfirm;
import org.jboss.narayana.compensations.api.TxLogged;


/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class CompensationsDependenciesDeploymentProcessor implements DeploymentUnitProcessor {

    private static final ModuleIdentifier COMPENSATIONS_MODULE = ModuleIdentifier.create("org.jboss.narayana.compensations");

    private static final Class<?>[] COMPENSATABLE_ANNOTATIONS = {
            Compensatable.class,
            CancelOnFailure.class,
            CompensationScoped.class,
            TxCompensate.class,
            TxConfirm.class,
            TxLogged.class
    };

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit unit = phaseContext.getDeploymentUnit();

        final CompositeIndex compositeIndex = unit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        if (compositeIndex == null) {
            return;
        }

        if (isCompensationAnnotationPresent(compositeIndex)) {
            addCompensationsModuleDependency(unit);
        }
    }

    private boolean isCompensationAnnotationPresent(final CompositeIndex compositeIndex) {
        for (Class<?> annotation : COMPENSATABLE_ANNOTATIONS) {
            if (!compositeIndex.getAnnotations(DotName.createSimple(annotation.getName())).isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private void addCompensationsModuleDependency(final DeploymentUnit unit) {
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();
        final ModuleSpecification moduleSpec = unit.getAttachment(Attachments.MODULE_SPECIFICATION);
        moduleSpec.addSystemDependency(new ModuleDependency(moduleLoader, COMPENSATIONS_MODULE, false, false, true, false));
    }

}

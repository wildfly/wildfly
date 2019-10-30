/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.txn.deployment;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;

import javax.transaction.TransactionScoped;
import javax.transaction.Transactional;
import java.util.List;


/**
 * Looks for usage of the @Transactional CDI interceptor (JTA 1.2) or the @TransactionScoped CDI context (JTA 1.2)
 * and adds the org.jboss.jts module dependency if they are found.
 *
 * Also adds the transaction API to deployments
 *
 * @author Paul Robinson
 */
public class TransactionDependenciesProcessor implements DeploymentUnitProcessor {

    public static final ModuleIdentifier JTS_MODULE = ModuleIdentifier.create("org.jboss.jts");
    public static final ModuleIdentifier TRANSACTION_API = ModuleIdentifier.create("javax.transaction.api");

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit unit = phaseContext.getDeploymentUnit();
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();

        final ModuleSpecification moduleSpec = unit.getAttachment(Attachments.MODULE_SPECIFICATION);
        moduleSpec.addSystemDependency(new ModuleDependency(moduleLoader, TRANSACTION_API, false, false, true, false));
        moduleSpec.addSystemDependency(new ModuleDependency(moduleLoader, ModuleIdentifier.fromString("org.wildfly.transaction.client"), false, false, true, false));

        final CompositeIndex compositeIndex = unit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        if (compositeIndex == null) {
            return;
        }

        final List<AnnotationInstance> transactionalAnnotations = compositeIndex.getAnnotations(DotName.createSimple(Transactional.class.getName()));
        final List<AnnotationInstance> transactionScopedAnnotations = compositeIndex.getAnnotations(DotName.createSimple(TransactionScoped.class.getName()));

        if (transactionalAnnotations.size() > 0 || transactionScopedAnnotations.size() > 0) {
            addJTSModuleDependencyToDeployment(unit);
        }
    }

    private void addJTSModuleDependencyToDeployment(DeploymentUnit unit) {

        final ModuleLoader moduleLoader = Module.getBootModuleLoader();
        final ModuleSpecification moduleSpec = unit.getAttachment(Attachments.MODULE_SPECIFICATION);
        moduleSpec.addSystemDependency(new ModuleDependency(moduleLoader, JTS_MODULE, false, false, true, false));
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }
}

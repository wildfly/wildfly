/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.structure;

import java.util.List;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.SubDeploymentMarker;
import org.jboss.as.server.deployment.module.ModuleRootMarker;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.vfs.VirtualFile;

/**
 * Processor that only runs for ear deployments where no application.xml is provided. It examines jars in the ear to determine
 * which are Jakarta Enterprise Beans sub-deployments.
 * <p/>
 * TODO: Move this to the Jakarta Enterprise Beans subsystem.
 *
 * @author Stuart Douglas
 */
public class EjbJarDeploymentProcessor implements DeploymentUnitProcessor {

    private static final DotName STATELESS = DotName.createSimple("jakarta.ejb.Stateless");
    private static final DotName STATEFUL = DotName.createSimple("jakarta.ejb.Stateful");
    private static final DotName MESSAGE_DRIVEN = DotName.createSimple("jakarta.ejb.MessageDriven");
    private static final DotName SINGLETON = DotName.createSimple("jakarta.ejb.Singleton");

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.DEPLOYMENT_ROOT);
        if (!DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            return;
        }
        //we don't check for the metadata attachment
        //cause this could come from a jboss-app.xml instead
        if(deploymentRoot.getRoot().getChild("META-INF/application.xml").exists()) {
            //if we have an application.xml we don't scan
            return;
        }
        // TODO: deal with application clients, we need the manifest information
        List<ResourceRoot> potentialSubDeployments = deploymentUnit.getAttachmentList(Attachments.RESOURCE_ROOTS);
        for (ResourceRoot resourceRoot : potentialSubDeployments) {
            if (ModuleRootMarker.isModuleRoot(resourceRoot)) {
                // module roots cannot be ejb jars
                continue;
            }
            VirtualFile ejbJarFile = resourceRoot.getRoot().getChild("META-INF/ejb-jar.xml");
            if (ejbJarFile.exists()) {
                SubDeploymentMarker.mark(resourceRoot);
                ModuleRootMarker.mark(resourceRoot);
            } else {
                final Index index = resourceRoot.getAttachment(Attachments.ANNOTATION_INDEX);
                if (index != null
                        && (!index.getAnnotations(STATEFUL).isEmpty() ||
                            !index.getAnnotations(STATELESS).isEmpty() ||
                            !index.getAnnotations(MESSAGE_DRIVEN).isEmpty() ||
                            !index.getAnnotations(SINGLETON).isEmpty())) {
                    // this is a Jakarta Enterprise Beans deployment
                    // TODO: we need to mark Jakarta Enterprise Beans sub deployments so the sub deployers know they are Jakarta
                    // Enterprise Beans deployments
                    SubDeploymentMarker.mark(resourceRoot);
                    ModuleRootMarker.mark(resourceRoot);
                }
            }
        }
    }
}

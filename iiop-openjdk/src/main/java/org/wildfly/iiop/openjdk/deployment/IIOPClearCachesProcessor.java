/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.iiop.openjdk.deployment;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.modules.Module;
import org.wildfly.iiop.openjdk.rmi.ExceptionAnalysis;
import org.wildfly.iiop.openjdk.rmi.InterfaceAnalysis;
import org.wildfly.iiop.openjdk.rmi.ValueAnalysis;

/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */

public class IIOPClearCachesProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
    }

    @Override
    public void undeploy(final DeploymentUnit context) {
        //clear data from the relevant caches
        Module module = context.getAttachment(Attachments.MODULE);
        if(module == null) {
            return;
        }
        ExceptionAnalysis.clearCache(module.getClassLoader());
        InterfaceAnalysis.clearCache(module.getClassLoader());
        ValueAnalysis.clearCache(module.getClassLoader());
    }
}

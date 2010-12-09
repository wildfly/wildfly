/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.deployment.naming;

import javax.naming.Context;

import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.as.deployment.unit.DeploymentPhaseContext;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * Deployment processor that deploys a naming context for the current module.
 *
 * @author John E. Bailey
 */
public class ModuleContextProcessor implements DeploymentUnitProcessor {

    /**
     * Add a ContextService for this module.
     *
     * @param phaseContext the deployment unit context
     * @throws DeploymentUnitProcessingException
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final ServiceTarget serviceTarget = phaseContext.getBatchBuilder();
        final ServiceName moduleContextServiceName = ContextNames.GLOBAL_CONTEXT_SERVICE_NAME.append(phaseContext.getName());
        final JndiName moduleContextJndiName = ContextNames.GLOBAL_CONTEXT_NAME.append(phaseContext.getName());
        final ContextService contextService = new ContextService(moduleContextJndiName);
        serviceTarget.addService(moduleContextServiceName, contextService)
            .addDependency(ContextNames.GLOBAL_CONTEXT_SERVICE_NAME, Context.class, contextService.getParentContextInjector())
            .install();

        phaseContext.putAttachment(ModuleContextConfig.ATTACHMENT_KEY, new ModuleContextConfig(moduleContextServiceName, moduleContextJndiName));
        // TODO: These names will need to change when application scoping becomes available.
    }
}

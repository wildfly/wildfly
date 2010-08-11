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

package org.jboss.as.deployment.module;

import org.jboss.as.deployment.AttachmentKey;
import org.jboss.as.deployment.DeploymentPhases;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;

/**
 * Deployment unit processor responsible for attaching the default deployment module loader to the context.
 * @author John E. Bailey
 */
public class DeploymentModuleLoaderProcessor implements DeploymentUnitProcessor {
    public static final long PRIORITY = DeploymentPhases.MODULARIZE.plus(101L);

    static final AttachmentKey<DeploymentModuleLoader> ATTACHMENT_KEY = new AttachmentKey<DeploymentModuleLoader>(DeploymentModuleLoader.class);

    private final DeploymentModuleLoader deploymentModuleLoader = new DeploymentModuleLoaderImpl();

    /**
     * If there isn't currently a deployment module loader, attach the default loader.
     * 
     * @param context the deployment unit context
     * @throws DeploymentUnitProcessingException
     */
    public void processDeployment(DeploymentUnitContext context) throws DeploymentUnitProcessingException {
        if(context.getAttachment(ATTACHMENT_KEY) == null)
            context.putAttachment(ATTACHMENT_KEY, deploymentModuleLoader);
    }
}

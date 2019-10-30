/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.deployment.processors;

import org.jboss.as.ejb3.subsystem.DefaultDistinctNameService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

/**
 * processor that sets the default distinct name for a deployment.
 *
 * @author Stuart Douglas
 */
public class EjbDefaultDistinctNameProcessor implements DeploymentUnitProcessor {

    private final DefaultDistinctNameService defaultDistinctNameService;

    public EjbDefaultDistinctNameProcessor(final DefaultDistinctNameService defaultDistinctNameService) {
        this.defaultDistinctNameService = defaultDistinctNameService;
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final String defaultDistinctName = defaultDistinctNameService.getDefaultDistinctName();
        if(defaultDistinctName != null) {
            phaseContext.getDeploymentUnit().putAttachment(org.jboss.as.ee.structure.Attachments.DISTINCT_NAME, defaultDistinctName);
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {

    }
}

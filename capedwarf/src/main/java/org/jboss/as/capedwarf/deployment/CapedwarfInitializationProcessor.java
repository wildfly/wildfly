/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.capedwarf.deployment;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.logging.Logger;
import org.jboss.vfs.VirtualFile;

/**
 * Recognize Capedwarf deployments.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class CapedwarfInitializationProcessor implements DeploymentUnitProcessor {

    private static final String APPENGINE_WEB_XML = "WEB-INF/appengine-web.xml";

    private final Logger log = Logger.getLogger(CapedwarfInitializationProcessor.class);

    /**
     * The relative order of this processor within the phase.
     * The current number is large enough for it to happen after all
     * the standard deployment unit processors that come with JBoss AS.
     */
    public static final int PRIORITY = 0x4000;

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit unit = phaseContext.getDeploymentUnit();

        if (unit.getParent() != null || DeploymentTypeMarker.isType(DeploymentType.WAR, unit) == false)
            return; // Skip non top and non web deployments

        if (hasAppEngineWebXml(unit)) {
            log.info("Found GAE / CapeDwarf deployment: " + unit);
            CapedwarfDeploymentMarker.mark(unit);
        }
    }

    protected boolean hasAppEngineWebXml(DeploymentUnit deploymentUnit) {
        ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        VirtualFile root = deploymentRoot.getRoot();
        VirtualFile xml = root.getChild(APPENGINE_WEB_XML);
        return xml.exists();
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

}

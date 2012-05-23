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

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.web.deployment.ScisMetaData;

import javax.servlet.ServletContainerInitializer;
import java.util.Set;

/**
 * Handle CapeDwarf JSF usage.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class CapedwarfJsfProcessor extends CapedwarfDeploymentUnitProcessor {

    private static final String FACES_INIT = "com.sun.faces.config.FacesInitializer";

    protected void doDeploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ScisMetaData scisMetaData = deploymentUnit.getAttachment(ScisMetaData.ATTACHMENT_KEY);
        if (scisMetaData != null) {
            ServletContainerInitializer key = null;
            for (ServletContainerInitializer sci : scisMetaData.getScis()) {
                if (FACES_INIT.equals(sci.getClass().getName())) {
                    key = sci;
                    break;
                }
            }
            if (key != null) {
                Set<Class<?>> classes = scisMetaData.getHandlesTypes().get(key);
                if (classes != null)
                    classes.add(Object.class); // hack
            }
        }
    }
}

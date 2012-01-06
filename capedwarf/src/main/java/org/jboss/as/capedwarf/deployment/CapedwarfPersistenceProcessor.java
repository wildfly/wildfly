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

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.module.ResourceRoot;

import java.io.IOException;
import java.util.List;

/**
 * Fix CapeDwarf persistence.xml / JPA usage - atm we use Hibernate.
 * DataNucleus support is on the roadmap.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public abstract class CapedwarfPersistenceProcessor extends CapedwarfDeploymentUnitProcessor {

    static final String DIALECT_PROPERTY_KEY = "hibernate.dialect";
    static final String DEFAULT_DIALECT = "org.hibernate.dialect.H2Dialect";

    @Override
    protected void doDeploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit unit = phaseContext.getDeploymentUnit();
        try {
            final ResourceRoot deploymentRoot = unit.getAttachment(Attachments.DEPLOYMENT_ROOT);
            modifyPersistenceInfo(unit, deploymentRoot);

            final List<ResourceRoot> resourceRoots = unit.getAttachment(Attachments.RESOURCE_ROOTS);
            for (ResourceRoot rr : resourceRoots) {
                modifyPersistenceInfo(unit, rr);
            }
        } catch (IOException e) {
            throw new DeploymentUnitProcessingException(e);
        }
    }

    protected abstract void modifyPersistenceInfo(DeploymentUnit unit, ResourceRoot resourceRoot) throws IOException;

}

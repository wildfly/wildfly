/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.webservices.deployers;

//import static org.jboss.as.webservices.util.WSAttachmentKeys.DEPLOYMENT_TYPE_KEY;
import static org.jboss.as.webservices.util.WSAttachmentKeys.WEBSERVICE_DEPLOYMENT_KEY;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.ejb3.deployment.EjbDeploymentMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.web.deployment.WarMetaData;
import org.jboss.as.webservices.util.ASHelper;
import org.jboss.jandex.Index;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.filter.PathFilters;
import org.jboss.wsf.spi.deployment.integration.WebServiceDeployment;

/**
 * A DUP that sets the dependencies required for using WS classes in WS deployments
 *
 * @author alessio.soldano@jboss.com
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @since 19-Jan-2011
 */
public final class WSDependenciesProcessor implements DeploymentUnitProcessor {

    private static final Logger LOGGER = Logger.getLogger(WSDependenciesProcessor.class);
    public static final ModuleIdentifier ASIL = ModuleIdentifier.create("org.jboss.as.webservices.server.integration");

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (!isWSDeployment(deploymentUnit)) {
            return;
        }

        final ModuleLoader moduleLoader = Module.getBootModuleLoader();
        final ModuleSpecification moduleSpec = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);

        moduleSpec.addDependency(applyCXFExtensionImportFilters(new ModuleDependency(moduleLoader, ASIL, false, true, true)));
    }

    private static ModuleDependency applyCXFExtensionImportFilters(final ModuleDependency dep) {
        // TODO: investigate how to do it via module.xml
        dep.addImportFilter(PathFilters.match("META-INF/cxf"), true);
        dep.addImportFilter(PathFilters.match("META-INF"), true);
        return dep;
    }

    /**
     * Determines whether the provided deployment unit is a WS endpoint deployment or not;
     * currently finds JSE endpoints only and relies upon endpoints declared in web.xml only
     * (merged jbossweb metadata is not available yet at this phase)
     * TODO: to be improved (jaxrpc deployments are skipped, ejb3 enpoints are skipped, web3 servlets are skipped)
     *
     * @param unit
     * @return
     */
    private boolean isJaxwsJseDeployment(final DeploymentUnit unit) {
        final boolean isWarDeployment = DeploymentTypeMarker.isType(DeploymentType.WAR, unit);
        if (isWarDeployment) {
            final Index index = ASHelper.getRootAnnotationIndex(unit);
            final WarMetaData warMetaData = ASHelper.getOptionalAttachment(unit, WarMetaData.ATTACHMENT_KEY);
            if (warMetaData != null && warMetaData.getWebMetaData() != null) {
                return (ASHelper.selectWebServiceServlets(index, warMetaData.getWebMetaData().getServlets(), true).size() > 0);
            }
        }

        return false;
    }

    private boolean isJaxwsEjbDeployment(final DeploymentUnit unit) {
        final boolean isEjbDeployment = EjbDeploymentMarker.isEjbDeployment(unit);
        if (isEjbDeployment) {
            WSEJBAdapterDeployer.internalDeploy(unit); // TODO: refactor this ugly hack
            WebServiceDeployment wsDeployment = ASHelper.getRequiredAttachment(unit, WEBSERVICE_DEPLOYMENT_KEY);
            return wsDeployment.getServiceEndpoints().size() > 0;
        }

        return false;
    }

    private boolean isWSDeployment(final DeploymentUnit unit) {
        if (isJaxwsJseDeployment(unit)) {
            LOGGER.trace("Detected JAXWS JSE deployment");
            //unit.putAttachment(DEPLOYMENT_TYPE_KEY, org.jboss.wsf.spi.deployment.Deployment.DeploymentType.JAXWS_JSE); // TODO: moved to WSTypeDeploymentProcessor
            return true;
        }
        else if (isJaxwsEjbDeployment(unit)) {
            LOGGER.trace("Detected JAXWS EJB3 deployment");
            //unit.putAttachment(DEPLOYMENT_TYPE_KEY, org.jboss.wsf.spi.deployment.Deployment.DeploymentType.JAXWS_EJB3); // TODO: moved to WSTypeDeploymentProcessor
            return true;
        }

        return false;
    }

    public void undeploy(final DeploymentUnit context) {
    }

}

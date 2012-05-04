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

package org.jboss.as.connector.deployers.ds;

import org.jboss.as.connector.deployers.datasource.DataSourceDefinitionAnnotationParser;
import org.jboss.as.connector.deployers.datasource.DataSourceDefinitionDeploymentDescriptorParser;
import org.jboss.as.connector.deployers.ds.processors.DsXmlDeploymentInstallProcessor;
import org.jboss.as.connector.deployers.ds.processors.DsXmlDeploymentParsingProcessor;
import org.jboss.as.connector.subsystems.datasources.DataSourcesExtension;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceTarget;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Service activator which installs the various service required for datasource
 * deployments.
 *
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public class DsDeploymentActivator {

    public Collection<ServiceController<?>> activateServices(final ServiceTarget serviceTarget,
                                                             final ServiceListener<Object>... listeners) {

        final Collection<ServiceController<?>> controllers = new ArrayList<ServiceController<?>>(1);

        return controllers;
    }

    public void activateProcessors(final DeploymentProcessorTarget updateContext) {
        updateContext.addDeploymentProcessor(DataSourcesExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_DSXML_DEPLOYMENT, new DsXmlDeploymentParsingProcessor());
        updateContext.addDeploymentProcessor(DataSourcesExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_DATA_SOURCE_DEFINITION_ANNOTATION, new DataSourceDefinitionAnnotationParser());
        updateContext.addDeploymentProcessor(DataSourcesExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_DATASOURCE_REF, new DataSourceDefinitionDeploymentDescriptorParser());
        updateContext.addDeploymentProcessor(DataSourcesExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_DSXML_DEPLOYMENT, new DsXmlDeploymentInstallProcessor());
    }
}

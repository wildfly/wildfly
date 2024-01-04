/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.deployers.ds;

import org.jboss.as.connector.deployers.datasource.DataSourceDefinitionAnnotationProcessor;
import org.jboss.as.connector.deployers.datasource.DataSourceDefinitionDescriptorProcessor;
import org.jboss.as.connector.deployers.ds.processors.DsXmlDeploymentInstallProcessor;
import org.jboss.as.connector.deployers.ds.processors.DsXmlDeploymentParsingProcessor;
import org.jboss.as.connector.deployers.ds.processors.JdbcDriverDeploymentProcessor;
import org.jboss.as.connector.subsystems.datasources.DataSourcesExtension;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;

/**
 * Service activator which installs the various service required for datasource
 * deployments.
 *
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public class DsDeploymentActivator {

    public void activateProcessors(final DeploymentProcessorTarget updateContext) {
        updateContext.addDeploymentProcessor(DataSourcesExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_DSXML_DEPLOYMENT, new DsXmlDeploymentParsingProcessor());
        updateContext.addDeploymentProcessor(DataSourcesExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_RESOURCE_DEF_ANNOTATION_DATA_SOURCE, new DataSourceDefinitionAnnotationProcessor());
        updateContext.addDeploymentProcessor(DataSourcesExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_JDBC_DRIVER, new JdbcDriverDeploymentProcessor());
        updateContext.addDeploymentProcessor(DataSourcesExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_RESOURCE_DEF_XML_DATA_SOURCE, new DataSourceDefinitionDescriptorProcessor());
        updateContext.addDeploymentProcessor(DataSourcesExtension.SUBSYSTEM_NAME, Phase.FIRST_MODULE_USE, Phase.FIRST_MODULE_USE_DSXML_DEPLOYMENT, new DsXmlDeploymentInstallProcessor());
    }

}

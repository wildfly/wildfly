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

package org.jboss.as.web;

import org.jboss.as.deployment.Phase;
import org.jboss.as.model.BootUpdateContext;
import org.jboss.as.web.deployment.JBossWebParsingDeploymentProcessor;
import org.jboss.as.web.deployment.ServletContainerInitializerDeploymentProcessor;
import org.jboss.as.web.deployment.TldParsingDeploymentProcessor;
import org.jboss.as.web.deployment.WarAnnotationDeploymentProcessor;
import org.jboss.as.web.deployment.WarAnnotationIndexProcessor;
import org.jboss.as.web.deployment.WarClassloadingDependencyProcessor;
import org.jboss.as.web.deployment.WarDeploymentInitializingProcessor;
import org.jboss.as.web.deployment.WarDeploymentProcessor;
import org.jboss.as.web.deployment.WarMetaDataProcessor;
import org.jboss.as.web.deployment.WarModuleConfigProcessor;
import org.jboss.as.web.deployment.WarStructureDeploymentProcessor;
import org.jboss.as.web.deployment.WebFragmentParsingDeploymentProcessor;
import org.jboss.as.web.deployment.WebParsingDeploymentProcessor;

/**
 * @author Emanuel Muckenhuber
 */
class WebDeploymentActivator {

    static void activate(final String defaultHost, final SharedWebMetaDataBuilder sharedWebBuilder, final SharedTldsMetaDataBuilder sharedTldsBuilder, final BootUpdateContext updateContext) {
        // Web specific deployment processors ....
        updateContext.addDeploymentProcessor(INIT_ME, new WarDeploymentInitializingProcessor(), Phase.WAR_DEPLOYMENT_INITIALIZING_PROCESSOR);
        updateContext.addDeploymentProcessor(INIT_ME, new WarStructureDeploymentProcessor(sharedWebBuilder.create(), sharedTldsBuilder.create()), Phase.WAR_STRUCTURE_DEPLOYMENT_PROCESSOR);
        updateContext.addDeploymentProcessor(INIT_ME, new WarAnnotationIndexProcessor(), Phase.WAR_ANNOTATION_INDEX_PROCESSOR);
        updateContext.addDeploymentProcessor(INIT_ME, new WarModuleConfigProcessor(), Phase.WAR_MODULE_CONFIG_PROCESSOR);
        updateContext.addDeploymentProcessor(INIT_ME, new WebParsingDeploymentProcessor(), Phase.WEB_PARSING_DEPLOYMENT_PROCESSOR);
        updateContext.addDeploymentProcessor(INIT_ME, new WebFragmentParsingDeploymentProcessor(), Phase.WEB_FRAGMENT_PARSING_DEPLOYMENT_PROCESSOR);
        updateContext.addDeploymentProcessor(INIT_ME, new JBossWebParsingDeploymentProcessor(), Phase.JBOSS_WEB_PARSING_DEPLOYMENT_PROCESSOR);
        updateContext.addDeploymentProcessor(INIT_ME, new TldParsingDeploymentProcessor(), Phase.TLD_PARSING_DEPLOYMENT_PROCESSOR);
        updateContext.addDeploymentProcessor(INIT_ME, new WarClassloadingDependencyProcessor(), Phase.WAR_CLASSLOADING_DEPENDENCY_PROCESSOR);
        updateContext.addDeploymentProcessor(INIT_ME, new WarAnnotationDeploymentProcessor(), Phase.WAR_ANNOTATION_DEPLOYMENT_PROCESSOR);
        updateContext.addDeploymentProcessor(INIT_ME, new ServletContainerInitializerDeploymentProcessor(), Phase.SERVLET_CONTAINER_INITIALIZER_DEPLOYMENT_PROCESSOR);
        updateContext.addDeploymentProcessor(INIT_ME, new WarMetaDataProcessor(), Phase.WAR_META_DATA_PROCESSOR);
        updateContext.addDeploymentProcessor(INIT_ME, new WarDeploymentProcessor(defaultHost), Phase.WAR_DEPLOYMENT_PROCESSOR);
    }
}

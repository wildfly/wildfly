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

package org.jboss.as.osgi.deployment;

import org.jboss.as.deployment.Phase;
import org.jboss.as.deployment.module.OSGiManifestAttachmentProcessor;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.as.model.BootUpdateContext;
import org.jboss.msc.service.ServiceContainer;

/**
 * Installs the various {@link DeploymentUnitProcessor}s required for OSGi deployments.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 07-Oct-2010
 */
public class OSGiDeploymentActivator {
    /**
     * Activate the services required for service deployments.
     */
    public void activate(final BootUpdateContext updateContext) {
        ServiceContainer serviceContainer = updateContext.getServiceContainer();
        updateContext.addDeploymentProcessor(INIT_ME, new OSGiManifestAttachmentProcessor(), Phase.OSGI_MANIFEST_ATTACHMENT_PROCESSOR);
        updateContext.addDeploymentProcessor(INIT_ME, new BundleInfoAttachmentProcessor(serviceContainer), Phase.OSGI_BUNDLE_INFO_ATTACHMENT_PROCESSOR);
        updateContext.addDeploymentProcessor(INIT_ME, new OSGiAttachmentsDeploymentProcessor(serviceContainer), Phase.OSGI_ATTACHMENTS_DEPLOYMENT_PROCESSOR);
    }
}

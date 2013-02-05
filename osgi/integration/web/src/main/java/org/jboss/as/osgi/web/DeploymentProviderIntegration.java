/*
 * #%L
 * JBossOSGi Framework
 * %%
 * Copyright (C) 2010 - 2012 JBoss by Red Hat
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package org.jboss.as.osgi.web;

import java.util.jar.Manifest;

import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.deployment.deployer.DeploymentFactory;
import org.jboss.osgi.framework.spi.DeploymentProvider;
import org.jboss.osgi.framework.spi.DeploymentProviderPlugin;
import org.jboss.osgi.framework.spi.StorageState;
import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.metadata.OSGiMetaDataBuilder;
import org.jboss.osgi.vfs.VirtualFile;
import org.osgi.framework.BundleException;

/**
 * A integration point for 'webbundle' locations.
 *
 * @author thomas.diesler@jboss.com
 * @since 30-Nov-2012
 */
public class DeploymentProviderIntegration extends DeploymentProviderPlugin {

    @Override
    protected DeploymentProvider createServiceValue(StartContext startContext) throws StartException {
        DeploymentProvider defaultHandler = super.createServiceValue(startContext);
        return new DeploymentProviderImpl(defaultHandler);
    }

    static class DeploymentProviderImpl implements DeploymentProvider {

        private final DeploymentProvider defaultHandler;

        DeploymentProviderImpl(DeploymentProvider defaultHandler) {
            this.defaultHandler = defaultHandler;
        }

        @Override
        public Deployment createDeployment(StorageState storageState) throws BundleException {
            return defaultHandler.createDeployment(storageState);
        }

        @Override
        public OSGiMetaData createOSGiMetaData(Deployment deployment) throws BundleException {
            return defaultHandler.createOSGiMetaData(deployment);
        }

        @Override
        public Deployment createDeployment(String location, VirtualFile rootFile) throws BundleException {

            if (!location.startsWith(WebExtension.WEBBUNDLE_PREFIX))
                return defaultHandler.createDeployment(location, rootFile);

            Manifest manifest = WebBundleURIParser.parse(location);
            OSGiMetaData metadata = OSGiMetaDataBuilder.load(manifest);
            String symbolicName = metadata.getBundleSymbolicName();

            Deployment dep = DeploymentFactory.createDeployment(rootFile, location, symbolicName, null);
            dep.addAttachment(Manifest.class, manifest);
            dep.addAttachment(OSGiMetaData.class, metadata);

            return dep;
        }
    }
}

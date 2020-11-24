/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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
package org.jboss.as.connector.deployers.ds.processors;

import org.jboss.as.connector._drivermanager.DriverManagerAdapter;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.AbstractResourceLoader;
import org.jboss.modules.ClassSpec;
import org.jboss.modules.ResourceLoaderSpec;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 * <p>
 * https://issues.redhat.com/browse/WFLY-14114
 * <p>
 * This is a hack that allows us to get access to {@link java.sql.Driver} registered by the driver code. Those objects are created by the
 * driver code and registered in {@link java.sql.DriverManager}. Driver objects are not guaranteed to be deregistered which leads to leaks.
 * {@link java.sql.DriverManager} allows for obtaining the list of drivers, and deregistering a driver but only in a give classloading context.
 * As a result, connector module can not neither list or deregister drivers from a deployed driver module.
 * <p>
 * To work this around, this hack modifies driver's module by injecting {@link DriverManagerAdapter} class to it. Because @{@link DriverManagerAdapter}
 * is loaded by driver module it allows to obtain and deregister the drivers.
 */

public class DriverManagerAdapterProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
            final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
            final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
            final DriverAdapterResourceLoader resourceLoader = new DriverAdapterResourceLoader();
            moduleSpecification.addResourceLoader(ResourceLoaderSpec.createResourceLoaderSpec(resourceLoader));
    }

    static class DriverAdapterResourceLoader extends AbstractResourceLoader {
        @Override
        public ClassSpec getClassSpec(final String fileName) throws IOException {
            InputStream is = this.getClass().getClassLoader().getResourceAsStream(fileName);
            if (is == null) {
                return null;
            }
            final byte[] bytes = readAllBytesFromStream(is);
            final ClassSpec spec = new ClassSpec();
            spec.setBytes(bytes);
            return spec;
        }

        @Override
        public Collection<String> getPaths() {
            return Collections.singletonList(DriverManagerAdapter.class.getPackage().getName().replace('.','/'));
        }

        private static byte[] readAllBytesFromStream(final InputStream is) throws IOException {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                final byte[] buffer = new byte[1024];
                int read = 0;
                while ((read = is.read(buffer)) != -1) {
                    bos.write(buffer, 0, read);
                }
                return bos.toByteArray();
            } finally {
                safeClose(is);
                safeClose(bos);
            }
        }

        private static void safeClose(final Closeable c) {
            if (c != null) {
                try {
                    c.close();
                } catch (Throwable ignored) {}
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {

    }
}

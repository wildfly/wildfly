/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
}

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

package org.jboss.as.arquillian.osgi.service;

import org.jboss.arquillian.protocol.jmx.JMXTestRunner;
import org.jboss.arquillian.testenricher.osgi.BundleContextAssociation;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.osgi.framework.BundleManagerService;
import org.jboss.osgi.metadata.OSGiMetaDataBuilder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

/**
 * Service responsible for creating and managing the life-cycle of the Arquillian service.
 *
 * @author Thomas.Diesler@jboss.com
 * @author Kabir Khan
 * @since 17-Nov-2010
 */
public class ArquillianService extends org.jboss.as.arquillian.service.ArquillianService<ArquillianConfig> implements Service<ArquillianService> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("arquillian", "testrunner");

    private static final Logger log = Logger.getLogger("org.jboss.as.arquillian");

    public static void addService(final ServiceTarget serviceTarget) {
        ArquillianService service = new ArquillianService();
        ServiceBuilder<ArquillianService> serviceBuilder = serviceTarget.addService(ArquillianService.SERVICE_NAME, service);
        service.build(serviceBuilder);
        serviceBuilder.install();
    }

    @Override
    protected ArquillianConfig createArquillianConfig(DeploymentUnit depUnit) {
        return ArquillianConfigBuilder.processDeployment(this, depUnit);
    }

    @Override
    protected JMXTestRunner.TestClassLoader createTestClassLoader() {
        return new ExtendedTestClassLoader();
    }

    // expose to package
    @Override
    protected ServiceContainer getServiceContainer() {
        return super.getServiceContainer();
    }

    @Override
    public synchronized ArquillianService getValue() throws IllegalStateException {
        return this;
    }

    void registerArquillianServiceWithOSGi(BundleManagerService bundleManager) {
        ModuleClassLoader classLoader = ((ModuleClassLoader) ArquillianService.class.getClassLoader());
        Module module = classLoader.getModule();
        if (bundleManager.getBundle(module.getIdentifier()) == null) {
            OSGiMetaDataBuilder builder = OSGiMetaDataBuilder.createBuilder("arquillian-service");
            builder.addExportPackages("org.jboss.arquillian.container.test.api", "org.jboss.arquillian.junit");
            builder.addExportPackages("org.jboss.arquillian.osgi", "org.jboss.arquillian.test.api");
            builder.addExportPackages("org.jboss.shrinkwrap.api", "org.jboss.shrinkwrap.api.asset", "org.jboss.shrinkwrap.api.spec");
            builder.addExportPackages("org.junit", "org.junit.runner");
            try {
                log.infof("Register arquillian service with OSGi layer");
                bundleManager.registerModule(getServiceTarget(), module, builder.getOSGiMetaData());
            } catch (BundleException ex) {
                log.errorf(ex, "Cannot register arquillian service with OSGi layer");
            }
        }
    }

    @Override
    protected void startingToUp(ServiceBuilder<ArquillianConfig> builder, ArquillianConfig arqConfig) {
        FrameworkActivationProcessor.process(builder, arqConfig);
    }

    class ExtendedTestClassLoader implements JMXTestRunner.TestClassLoader {

        @Override
        public Class<?> loadTestClass(final String className) throws ClassNotFoundException {

            final ArquillianConfig arqConfig = getArquillianConfig(className, -1);
            if (arqConfig == null)
                throw new ClassNotFoundException("No Arquillian config found for: " + className);

            // Make the BundleContext available to the {@link OSGiTestEnricher}
            BundleContext bundleContext = arqConfig.getBundleContext();
            BundleContextAssociation.setBundleContext(bundleContext);

            return arqConfig.loadClass(className);
        }
    }
}

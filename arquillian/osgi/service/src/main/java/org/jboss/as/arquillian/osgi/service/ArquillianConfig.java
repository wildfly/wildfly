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

import java.util.Collection;
import java.util.Set;

import org.jboss.arquillian.testenricher.osgi.BundleAssociation;
import org.jboss.as.osgi.deployment.OSGiDeploymentAttachment;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.framework.BundleManagerService;
import org.jboss.osgi.framework.Services;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * The ArquillianConfig represents an Arquillian deployment.
 *
 * @author Thomas.Diesler@jboss.com
 */
class ArquillianConfig extends org.jboss.as.arquillian.service.ArquillianConfig<ArquillianConfig> {

    static final AttachmentKey<ArquillianConfig> KEY = AttachmentKey.create(ArquillianConfig.class);

    private final Collection<String> testClasses;

    private final InjectedValue<BundleManagerService> injectedBundleManager = new InjectedValue<BundleManagerService>();
    private final InjectedValue<BundleContext> injectedBundleContext = new InjectedValue<BundleContext>();

    ArquillianConfig(ArquillianService arqService, DeploymentUnit depUnit, Set<String> testClasses) {
        super(arqService, depUnit, testClasses);
        this.testClasses = super.getTestClasses();
    }

    ServiceBuilder<ArquillianConfig> buildService(ServiceTarget serviceTarget, ServiceController<?> depController) {
        ServiceBuilder<ArquillianConfig> builder = serviceTarget.addService(getServiceName(), this);
        builder.addDependency(depController.getName());
        return builder;
    }

    void addFrameworkDependency(ServiceBuilder<ArquillianConfig> builder) {
        builder.addDependency(Services.BUNDLE_MANAGER, BundleManagerService.class, injectedBundleManager);
        builder.addDependency(Services.SYSTEM_CONTEXT, BundleContext.class, injectedBundleContext);
        builder.addDependency(Services.FRAMEWORK_ACTIVATOR);
    }

    @Override
    protected ArquillianService getArquillianService() {
        return (ArquillianService) super.getArquillianService();
    }

    BundleContext getBundleContext() {
        return injectedBundleContext.getOptionalValue();
    }

    // expose to package
    @Override
    protected DeploymentUnit getDeploymentUnit() {
        return super.getDeploymentUnit();
    }

    // expose to package
    @Override
    protected ServiceName getServiceName() {
        return super.getServiceName();
    }

    // expose to package
    @Override
    protected Collection<String> getTestClasses() {
        return testClasses;
    }

    Class<?> loadClass(String className) throws ClassNotFoundException {
        final DeploymentUnit depUnit = getDeploymentUnit();

        if (testClasses.contains(className) == false)
            throw new ClassNotFoundException("Class '" + className + "' not found in: " + testClasses);

        Module module = depUnit.getAttachment(Attachments.MODULE);
        Deployment osgidep = OSGiDeploymentAttachment.getDeployment(depUnit);
        if (module == null && osgidep == null)
            throw new IllegalStateException("Cannot determine deployment type: " + depUnit);
        if (module != null && osgidep != null)
            throw new IllegalStateException("Found MODULE attachment for Bundle deployment: " + depUnit);

        Class<?> testClass;
        if (osgidep != null) {
            Bundle bundle = osgidep.getAttachment(Bundle.class);
            testClass = bundle.loadClass(className);
            BundleAssociation.setBundle(bundle);
        } else {
            testClass = module.getClassLoader().loadClass(className);
        }

        associate();

        return testClass;
    }

    @Override
    public synchronized void start(StartContext context) throws StartException {
        super.start(context);
        BundleManagerService bundleManager = injectedBundleManager.getOptionalValue();
        if (bundleManager != null) {
            getArquillianService().registerArquillianServiceWithOSGi(bundleManager);
        }
    }

    @Override
    public synchronized ArquillianConfig getValue() {
        return this;
    }
}

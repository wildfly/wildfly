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
package org.jboss.as.arquillian.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jboss.arquillian.testenricher.msc.ServiceContainerAssociation;
import org.jboss.arquillian.testenricher.msc.ServiceTargetAssociation;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.modules.Module;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * The ArquillianConfig represents an Arquillian deployment.
 *
 * @author Thomas.Diesler@jboss.com
 */
class ArquillianConfig implements Service<ArquillianConfig> {

    static final AttachmentKey<ArquillianConfig> KEY = AttachmentKey.create(ArquillianConfig.class);

    private final ArquillianService arqService;
    private final DeploymentUnit depUnit;
    private final ServiceName serviceName;
    private final List<String> testClasses = new ArrayList<String>();

    /*
    private final InjectedValue<BundleManagerService> injectedBundleManager = new InjectedValue<BundleManagerService>();
    private final InjectedValue<BundleContext> injectedBundleContext = new InjectedValue<BundleContext>();
    */
    private ServiceContainer serviceContainer;
    private ServiceTarget serviceTarget;

    static ServiceName getServiceName(DeploymentUnit depUnit) {
        return ServiceName.JBOSS.append("arquillian", "config", depUnit.getName());
    }

    ArquillianConfig(ArquillianService arqService, DeploymentUnit depUnit, Set<String> testClasses) {
        this.arqService = arqService;
        this.depUnit = depUnit;
        this.serviceName = getServiceName(depUnit);
        this.testClasses.addAll(testClasses);
    }

    ServiceBuilder<ArquillianConfig> buildService(ServiceTarget serviceTarget, ServiceController<?> depController) {
        ServiceBuilder<ArquillianConfig> builder = serviceTarget.addService(getServiceName(), this);
        builder.addDependency(depController.getName());
        return builder;
    }

    /*
    void addFrameworkDependency(ServiceBuilder<ArquillianConfig> builder) {
        builder.addDependency(Services.BUNDLE_MANAGER, BundleManagerService.class, injectedBundleManager);
        builder.addDependency(Services.SYSTEM_CONTEXT, BundleContext.class, injectedBundleContext);
        builder.addDependency(Services.FRAMEWORK_ACTIVATOR);
    }
    */

    ServiceName getServiceName() {
        return serviceName;
    }

    DeploymentUnit getDeploymentUnit() {
        return depUnit;
    }

    List<String> getTestClasses() {
        return Collections.unmodifiableList(testClasses);
    }

    /*
    BundleContext getBundleContext() {
        return injectedBundleContext.getOptionalValue();
    }
    */

    Class<?> loadClass(String className) throws ClassNotFoundException {

        if (testClasses.contains(className) == false)
            throw new ClassNotFoundException("Class '" + className + "' not found in: " + testClasses);

        Module module = depUnit.getAttachment(Attachments.MODULE);
        /*
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
        */
        if (module == null)
            throw new IllegalStateException("Cannot determine deployment type: " + depUnit);
        final Class<?> testClass = module.getClassLoader().loadClass(className);

        // Always make the MSC artefacts available
        ServiceTargetAssociation.setServiceTarget(serviceTarget);
        ServiceContainerAssociation.setServiceContainer(serviceContainer);

        return testClass;
    }

    @Override
    public synchronized void start(StartContext context) throws StartException {
        serviceContainer = context.getController().getServiceContainer();
        serviceTarget = context.getChildTarget();
        arqService.registerArquillianConfig(this);

        /*
        BundleManagerService bundleManager = injectedBundleManager.getOptionalValue();
        if (bundleManager != null) {
            arqService.registerArquillianServiceWithOSGi(bundleManager);
        }
        */
    }

    @Override
    public synchronized void stop(StopContext context) {
        context.getController().setMode(Mode.REMOVE);
        arqService.unregisterArquillianConfig(this);
    }

    @Override
    public synchronized ArquillianConfig getValue() {
        return this;
    }

    @Override
    public String toString() {
        String uname = depUnit.getName();
        String sname = serviceName.getCanonicalName();
        return "ArquillianConfig[service=" + sname + ",unit=" + uname + ",tests=" + testClasses + "]";
    }
}

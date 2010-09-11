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

package org.jboss.as.osgi;

import java.util.List;

import org.jboss.as.deployment.chain.DeploymentChain;
import org.jboss.as.osgi.OSGiSubsystemSupport.BatchedWork;
import org.jboss.as.osgi.OSGiSubsystemSupport.TestModuleLoader;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.osgi.framework.bundle.BundleManager;
import org.jboss.osgi.testing.OSGiTestHelper;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Abstract OSGi subsystem test.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 11-Sep-2010
 */
public abstract class AbstractOSGiSubsystemTest {

    protected abstract OSGiSubsystemSupport getSubsystemSupport();

    public ServiceContainer getServiceContainer() {
        return getSubsystemSupport().getServiceContainer();
    }

    protected ModuleLoader getClassifyingModuleLoader() {
        return getSubsystemSupport().getClassifyingModuleLoader();
    }

    protected TestModuleLoader getTestModuleLoader() {
        return getSubsystemSupport().getTestModuleLoader();
    }

    protected Module loadModule(ModuleIdentifier identifier) throws ModuleLoadException {
        return getSubsystemSupport().loadModule(identifier);
    }

    public DeploymentChain getDeploymentChain() {
        return getSubsystemSupport().getDeploymentChain();
    }

    protected void setupServices(final BatchBuilder batchBuilder) throws Exception {
        getSubsystemSupport().setupServices(batchBuilder);
    }

    protected BundleManager getBundleManager() {
        return getSubsystemSupport().getBundleManager();
    }

    protected BundleContext getSystemContext() {
        return getSubsystemSupport().getSystemContext();
    }

    protected Bundle executeDeploy(final JavaArchive archive) throws Exception {
        return getSubsystemSupport().executeDeploy(archive);
    }

    protected void executeUndeploy(final JavaArchive archive) throws Exception {
        getSubsystemSupport().executeUndeploy(archive);
    }

    protected List<ServiceName> runWithLatchedBatch(final BatchedWork work) throws Exception {
        return getSubsystemSupport().runWithLatchedBatch(work);
    }

    protected void assertServiceUp(ServiceName prefix) {
        getSubsystemSupport().assertServiceUp(prefix);
    }

    protected void assertServiceDown(ServiceName prefix) {
        getSubsystemSupport().assertServiceDown(prefix);
    }

    protected void assertBundleState(int expState, int wasState) {
        new OSGiTestHelper().assertBundleState(expState, wasState);
    }

    public void assertLoadClass(ModuleIdentifier identifier, String className) throws Exception
    {
        getSubsystemSupport().assertLoadClass(identifier, className);
    }

    public void assertLoadClass(ModuleIdentifier identifier, String className, ModuleIdentifier exporterId) throws Exception
    {
        getSubsystemSupport().assertLoadClass(identifier, className, exporterId);
    }

    public void assertLoadClassFails(ModuleIdentifier identifier, String className) throws Exception
    {
        getSubsystemSupport().assertLoadClassFails(identifier, className);
    }

    public Class<?> loadClass(ModuleIdentifier identifier, String className) throws Exception
    {
        return getSubsystemSupport().loadClass(identifier, className);
    }

    protected Class<?> assertLoadClass(Bundle bundle, String className) {
        return new OSGiTestHelper().assertLoadClass(bundle, className);
    }

    protected void assertLoadClassFail(Bundle bundle, String className) {
        new OSGiTestHelper().assertLoadClassFail(bundle, className);
    }

    protected void assertLoadClass(Bundle bundle, String className, Bundle exporter) {
        new OSGiTestHelper().assertLoadClass(bundle, className, exporter);
    }
}

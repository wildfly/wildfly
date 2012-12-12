/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.osgi.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.embedded.StandaloneServer;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.osgi.framework.Services;
import org.jboss.osgi.framework.spi.FutureServiceValue;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.launch.Framework;

/**
 * An proxy to the underlying {@link Framework}.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 12-Dec-2012
 */
final class FrameworkProxy implements Framework {

    private final StandaloneServer server;
    private int state = Bundle.INSTALLED;
    private BundleContext bundleContext;

    FrameworkProxy(StandaloneServer server) {
        this.server = server;
    }

    @Override
    public long getBundleId() {
        return 0;
    }

    @Override
    public String getSymbolicName() {
        return Constants.SYSTEM_BUNDLE_SYMBOLICNAME;
    }

    @Override
    public String getLocation() {
        return Constants.SYSTEM_BUNDLE_LOCATION;
    }

    @Override
    public Version getVersion() {
        // [TODO] framework version
        return Version.emptyVersion;
    }

    @Override
    public BundleContext getBundleContext() {
        return bundleContext;
    }

    @Override
    public int getState() {
        return bundleContext != null ? bundleContext.getBundle().getState() : state;
    }

    /**
     * Initialize this Framework.
     *
     * After calling this method, this Framework must:
     *
     * - Be in the Bundle.STARTING state.
     * - Have a valid Bundle Context.
     * - Be at start level 0.
     * - Have event handling enabled.
     * - Have reified Bundle objects for all installed bundles.
     * - Have registered any framework services (e.g. PackageAdmin, ConditionalPermissionAdmin, StartLevel)
     *
     * This Framework will not actually be started until start is called.
     *
     * This method does nothing if called when this Framework is in the Bundle.STARTING, Bundle.ACTIVE or Bundle.STOPPING
     * states.
     */
    @Override
    @SuppressWarnings("unchecked")
    public void init() throws BundleException {

        // This method does nothing if called when this Framework is in the STARTING, ACTIVE or STOPPING state
        int state = getState();
        if (state == Bundle.STARTING || state == Bundle.ACTIVE || state == Bundle.STOPPING)
            return;

        // Start the server
        try {
            server.start();
        } catch (Throwable th) {
            throw new BundleException("Cannot start the server", th);
        }

        // Activate the OSGi subsystem
        try {
            ModelControllerClient client = server.getModelControllerClient();
            ModelNode op = createOpNode("subsystem=osgi", "activate");
            executeOperation(client, op, true);
        } catch (Throwable th) {
            throw new BundleException("Cannot activate the osgi subsystem", th);
        }

        try {
            // The above implies that the subsystem is up when the operation is executed successfully
            // We still use {@link FutureServiceValue} to work around a rare race condition that causes to op to return prematurely
            ServiceController<BundleContext> controller = (ServiceController<BundleContext>) server.getService(Services.FRAMEWORK_INIT);
            FutureServiceValue<BundleContext> future = new FutureServiceValue<BundleContext>(controller);
            bundleContext = future.get(30, TimeUnit.SECONDS);
        } catch (Throwable th) {
            throw new BundleException("Cannot get bundle context", th);
        }
    }

    @Override
    public void start() throws BundleException {
        start(0);
    }

    /**
     * Start this Framework.
     *
     * The following steps are taken to start this Framework:
     *
     * - If this Framework is not in the {@link #STARTING} state, {@link #init()} is called
     * - All installed bundles must be started
     * - The start level of this Framework is moved to the FRAMEWORK_BEGINNING_STARTLEVEL
     *
     * Any exceptions that occur during bundle starting must be wrapped in a {@link BundleException} and then published as a
     * framework event of type {@link FrameworkEvent#ERROR}
     *
     * - This Framework's state is set to {@link #ACTIVE}.
     * - A framework event of type {@link FrameworkEvent#STARTED} is fired
     */
    @Override
    public void start(int options) throws BundleException {

        int state = getState();
        if (state != Bundle.STARTING)
            init();
    }

    @Override
    public void stop() throws BundleException {
        stop(0);
    }

    /**
     * Stop this Framework.
     *
     * The method returns immediately to the caller after initiating the following steps to be taken on another thread.
     *
     * 1. This Framework's state is set to Bundle.STOPPING.
     * 2. All installed bundles must be stopped without changing each bundle's persistent autostart setting.
     * 3. Unregister all services registered by this Framework.
     * 4. Event handling is disabled.
     * 5. This Framework's state is set to Bundle.RESOLVED.
     * 6. All resources held by this Framework are released. This includes threads, bundle class loaders, open files, etc.
     * 7. Notify all threads that are waiting at waitForStop that the stop operation has completed.
     *
     * After being stopped, this Framework may be discarded, initialized or started.
     */
    @Override
    public void stop(int options) throws BundleException {
        // Stop the server
        try {
            server.stop();
        } catch (Throwable th) {
            throw new BundleException("Cannot stop the server", th);
        } finally {
            bundleContext = null;
            state = Bundle.RESOLVED;
        }
    }

    /**
     * Wait until this Framework has completely stopped.
     *
     * The stop and update methods on a Framework performs an asynchronous stop of the Framework. This method can be used to
     * wait until the asynchronous stop of this Framework has completed. This method will only wait if called when this
     * Framework is in the Bundle.STARTING, Bundle.ACTIVE, or Bundle.STOPPING states. Otherwise it will return immediately.
     *
     * A Framework Event is returned to indicate why this Framework has stopped.
     */
    @Override
    public FrameworkEvent waitForStop(long timeout) throws InterruptedException {
        return new FrameworkEvent(FrameworkEvent.STOPPED, this, null);
    }

    @Override
    public void update(InputStream input) throws BundleException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void uninstall() throws BundleException {
        throw new UnsupportedOperationException();
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Dictionary getHeaders() {
        assertValidBundleContext();
        return bundleContext.getBundle().getHeaders();
    }

    @Override
    public ServiceReference[] getRegisteredServices() {
        assertValidBundleContext();
        return bundleContext.getBundle().getRegisteredServices();
    }

    @Override
    public ServiceReference[] getServicesInUse() {
        assertValidBundleContext();
        return bundleContext.getBundle().getServicesInUse();
    }

    @Override
    public boolean hasPermission(Object permission) {
        assertValidBundleContext();
        return bundleContext.getBundle().hasPermission(permission);
    }

    @Override
    public URL getResource(String name) {
        assertValidBundleContext();
        return bundleContext.getBundle().getResource(name);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Dictionary getHeaders(String locale) {
        assertValidBundleContext();
        return bundleContext.getBundle().getHeaders(locale);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        assertValidBundleContext();
        return bundleContext.getBundle().loadClass(name);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Enumeration getResources(String name) throws IOException {
        assertValidBundleContext();
        return bundleContext.getBundle().getResources(name);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Enumeration getEntryPaths(String path) {
        assertValidBundleContext();
        return bundleContext.getBundle().getEntryPaths(path);
    }

    @Override
    public URL getEntry(String path) {
        assertValidBundleContext();
        return bundleContext.getBundle().getEntry(path);
    }

    @Override
    public long getLastModified() {
        assertValidBundleContext();
        return bundleContext.getBundle().getLastModified();
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Enumeration findEntries(String path, String filePattern, boolean recurse) {
        assertValidBundleContext();
        return bundleContext.getBundle().findEntries(path, filePattern, recurse);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Map getSignerCertificates(int signersType) {
        assertValidBundleContext();
        return bundleContext.getBundle().getSignerCertificates(signersType);
    }

    private void assertValidBundleContext() {
        if (bundleContext == null)
            throw new IllegalStateException("Framework bundle context not available");
    }

    private static ModelNode createOpNode(String address, String operation) {
        ModelNode op = new ModelNode();
        ModelNode list = op.get("address").setEmptyList();
        if (address != null) {
            String[] pathSegments = address.split("/");
            for (String segment : pathSegments) {
                String[] elements = segment.split("=");
                list.add(elements[0], elements[1]);
            }
        }
        op.get("operation").set(operation);
        return op;
    }

    private static ModelNode executeOperation(final ModelControllerClient client, ModelNode op, boolean unwrapResult) throws Exception {
        ModelNode ret = client.execute(op);
        if (!unwrapResult) return ret;
        if (!"success".equals(ret.get("outcome").asString())) {
            throw new IllegalStateException("Management operation failed: " + ret.get("failure-description"));
        }
        return ret.get("result");
    }
}

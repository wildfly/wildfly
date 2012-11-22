/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.osgi.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.launch.Framework;

/**
 * Implements the standard OSGi Framework launch API.
 *
 * @author David Bosschaert
 */
public class FrameworkWrapper implements Framework {
    private final EmbeddedOSGiFrameworkLauncher launcher;
    private final Bundle systemBundle;
    private final AtomicInteger frameworkState = new AtomicInteger(Bundle.INSTALLED);

    public FrameworkWrapper(EmbeddedOSGiFrameworkLauncher launcher, Bundle systemBundle) {
        this.launcher = launcher;
        this.systemBundle = systemBundle;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Enumeration findEntries(String path, String filePattern, boolean recurse) {
        return systemBundle.findEntries(path, filePattern, recurse);
    }

    @Override
    public BundleContext getBundleContext() {
        return systemBundle.getBundleContext();
    }

    @Override
    public URL getEntry(String path) {
        return systemBundle.getEntry(path);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Enumeration getEntryPaths(String path) {
        return systemBundle.getEntryPaths(path);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Dictionary getHeaders() {
        return systemBundle.getHeaders();
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Dictionary getHeaders(String locale) {
        return systemBundle.getHeaders(locale);
    }

    @Override
    public long getLastModified() {
        return systemBundle.getLastModified();
    }

    @Override
    public ServiceReference[] getRegisteredServices() {
        return systemBundle.getRegisteredServices();
    }

    @Override
    public URL getResource(String name) {
        return systemBundle.getResource(name);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Enumeration getResources(String name) throws IOException {
        return systemBundle.getResources(name);
    }

    @Override
    public ServiceReference[] getServicesInUse() {
        return systemBundle.getServicesInUse();
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Map getSignerCertificates(int signersType) {
        return systemBundle.getSignerCertificates(signersType);
    }

    @Override
    public int getState() {
        return frameworkState.get();
    }

    @Override
    public Version getVersion() {
        return systemBundle.getVersion();
    }

    @Override
    public boolean hasPermission(Object permission) {
        return systemBundle.hasPermission(permission);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Class loadClass(String name) throws ClassNotFoundException {
        return systemBundle.loadClass(name);
    }

    @Override
    public long getBundleId() {
        return 0;
    }

    @Override
    public String getLocation() {
        return systemBundle.getLocation();
    }

    @Override
    public String getSymbolicName() {
        return systemBundle.getSymbolicName();
    }

    @Override
    public void init() throws BundleException {
        frameworkState.set(Bundle.STARTING);

        int state = systemBundle.getState();
        if (state == Bundle.STARTING || state == Bundle.ACTIVE || state == Bundle.STOPPING)
            return;

        awaitFrameworkInit();
    }

    private void awaitFrameworkInit() {
        launcher.getService(30000, "jbosgi", "framework", "INIT");
    }

    @Override
    public void start() throws BundleException {
        start(0);
    }

    @Override
    public void start(int options) throws BundleException {
        if (frameworkState.get() != Bundle.STARTING)
            init();

        awaitFrameworkActive();
        frameworkState.set(Bundle.ACTIVE);
    }

    private void awaitFrameworkActive() {
        launcher.getService(30000, "jbosgi", "framework", "ACTIVE");
    }

    @Override
    public void stop() throws BundleException {
        stop(0);
    }

    @Override
    public void stop(int options) throws BundleException {
        frameworkState.set(Bundle.STOPPING);
    }

    @Override
    public void uninstall() throws BundleException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update() throws BundleException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(InputStream in) throws BundleException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FrameworkEvent waitForStop(long timeout) throws InterruptedException {
        frameworkState.set(Bundle.STOPPING);

        try {
            launcher.stop();
            Thread.sleep(5000);
        } catch (Throwable e) {
        }
        frameworkState.set(Bundle.RESOLVED);

        return new FrameworkEvent(FrameworkEvent.STOPPED, this, null);
    }
}

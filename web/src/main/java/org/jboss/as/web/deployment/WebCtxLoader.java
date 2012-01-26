/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.web.deployment;

import java.beans.PropertyChangeListener;

import org.apache.catalina.Container;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;

/**
 * @author Emanuel Muckenhuber
 */
public class WebCtxLoader implements Lifecycle, Loader {

    /** The container. */
    private Container container;

    /** The deployment classloader. */
    private ClassLoader classloader;

    public WebCtxLoader(final ClassLoader classloader) {
        this.classloader = classloader;
    }

    public void addPropertyChangeListener(PropertyChangeListener arg0) {
        // FIXME addPropertyChangeListener
    }

    public void addRepository(String arg0) {
        // FIXME addRepository
    }

    public void backgroundProcess() {
        // FIXME backgroudProcess
    }

    public ClassLoader getClassLoader() {
        return classloader;
    }

    public Container getContainer() {
        return container;
    }

    public String getInfo() {
        return "";
    }

    public void removePropertyChangeListener(PropertyChangeListener arg0) {
        // FIXME removePropertyChangeListener
    }

    public void setContainer(Container container) {
        this.container = container;
    }

    public void addLifecycleListener(LifecycleListener arg0) {
        // FIXME addLifecycleListener
    }

    public LifecycleListener[] findLifecycleListeners() {
        return new LifecycleListener[0];
    }

    public void removeLifecycleListener(LifecycleListener arg0) {
        // FIXME removeLifecycleListener
    }

    public void start() throws LifecycleException {
        if (this.classloader == null) {
            throw new LifecycleException("null classloader");
        }
    }

    public void stop() throws LifecycleException {
        this.container = null;
    }

}

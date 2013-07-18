/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow;

import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ServletContainer;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Central Undertow 'Container' HTTP listeners will make this container accessible whilst deployers will add content.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ServletContainerService implements Service<ServletContainerService> {

    private final boolean developmentMode;
    private volatile ServletContainer servletContainer;
    @Deprecated
    private final Map<String, Integer> secureListeners = new ConcurrentHashMap<>(1);

    public ServletContainerService(boolean developmentMode) {
        this.developmentMode = developmentMode;
    }

    static String getDeployedContextPath(DeploymentInfo deploymentInfo) {
        return "".equals(deploymentInfo.getContextPath()) ? "/" : deploymentInfo.getContextPath();
    }

    public void start(StartContext context) throws StartException {

        servletContainer = ServletContainer.Factory.newInstance();
    }

    public void stop(StopContext context) {

    }

    public ServletContainerService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public ServletContainer getServletContainer() {
        return servletContainer;
    }

    public boolean isDevelopmentMode() {
        return developmentMode;
    }

    public Integer lookupSecurePort(final String listenerName) {
        Integer response = null;
        response = secureListeners.get(listenerName);
        if (response == null) {
            while (response == null && secureListeners.isEmpty() == false) {
                try {
                    response = secureListeners.values().iterator().next();
                } catch (ConcurrentModificationException cme) {
                    // Ignored - The chance of happening is so small but do not wish to add
                    // additional synchronisation. If listeners are being added and removed
                    // to a server under load then behaviour could not be expected to be consistent.
                }
            }
        }

        if (response == null) {
            throw new IllegalStateException("No secure listeners defined.");
        }

        return response;

    }

    public void registerSecurePort(final String listenerName, final Integer port) {
        secureListeners.put(listenerName, port);
    }

    public void unregisterSecurePort(final String name) {
        secureListeners.remove(name);
    }

}

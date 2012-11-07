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

package org.jboss.as.server.deploymentoverlay.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Service that aggregates all available deployment overrides
 *
 * @author Stuart Douglas
 */
public class DeploymentOverlayIndexService implements Service<DeploymentOverlayIndexService> {

    private final List<DeploymentOverlayLinkService> services = new ArrayList<DeploymentOverlayLinkService>();

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("deploymentOverlayIndexService");

    @Override
    public synchronized void start(final StartContext context) throws StartException {
    }

    @Override
    public synchronized void stop(final StopContext context) {
        services.clear();
    }

    public synchronized void addService(final DeploymentOverlayLinkService service) {
        services.add(service);
    }

    public synchronized void removeService(final DeploymentOverlayLinkService service) {
        services.remove(service);
    }

    /**
     * Returns all the deployment overlays that should be applied to a deployment, with the highest priority
     * first.
     *
     * @param deploymentName The deployment name
     * @return
     */
    public synchronized List<DeploymentOverlayService> getOverrides(final String deploymentName) {

        final List<DeploymentOverlayLinkService> matched = new ArrayList<DeploymentOverlayLinkService>();
        for (final DeploymentOverlayLinkService service : services) {
            if (service.isRegex()) {
                if (service.getPattern().matcher(deploymentName).matches()) {
                    matched.add(service);
                }
            } else if (service.getDeployment().equals(deploymentName)) {
                matched.add(service);
            }
        }
        Collections.sort(matched, new Comparator<DeploymentOverlayLinkService>() {
            @Override
            public int compare(final DeploymentOverlayLinkService o1, final DeploymentOverlayLinkService o2) {
                int res = o1.getPriority().ordinal() - o2.getPriority().ordinal();
                if (res != 0) {
                    return res;
                }
                if (o2.isRegex() && !o1.isRegex()) {
                    return -1;
                } else if (o1.isRegex() && !o2.isRegex()) {
                    return 1;
                }
                return 0;
            }
        });

        final List<DeploymentOverlayService> ret = new ArrayList<DeploymentOverlayService>();
        for (final DeploymentOverlayLinkService i : matched) {
            ret.add(i.getDeploymentOverlayServiceInjectedValue().getValue());
        }
        return ret;
    }

    private boolean wildcardMatch(final String wildcard, final String deploymentName) {
        if (wildcard.startsWith("*")) {
            return deploymentName.endsWith(wildcard.substring(1));
        } else if (wildcard.endsWith("*")) {
            return deploymentName.startsWith(wildcard.substring(0, wildcard.length() - 1));
        }
        return false;
    }

    @Override
    public DeploymentOverlayIndexService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }
}

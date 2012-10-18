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

import java.util.regex.Pattern;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Stuart Douglas
 */
public class DeploymentOverlayLinkService implements Service<DeploymentOverlayLinkService> {

    public static final ServiceName SERVICE_NAME = DeploymentOverlayIndexService.SERVICE_NAME.append("deploymentOverlayLinkService");

    private static String wildcardToJavaRegexp(String expr) {
        if(expr == null) {
            throw new IllegalArgumentException("expr is null");
        }
        String regex = expr.replaceAll("([(){}\\[\\].+^$])", "\\\\$1"); // escape regex characters
        regex = regex.replaceAll("\\*", ".*"); // replace * with .*
        regex = regex.replaceAll("\\?", "."); // replace ? with .
        return regex;
    }

    private final InjectedValue<DeploymentOverlayIndexService> deploymentOverlayIndexServiceInjectedValue = new InjectedValue<DeploymentOverlayIndexService>();
    private final InjectedValue<DeploymentOverlayService> deploymentOverlayServiceInjectedValue = new InjectedValue<DeploymentOverlayService>();
    private final String deployment;
    private final DeploymentOverlayPriority priority;
    private final Pattern pattern;
    private final boolean regex;

    public DeploymentOverlayLinkService(final String deployment, final boolean regex, final DeploymentOverlayPriority priority) {
        this.deployment = deployment;
        this.priority = priority;
        this.regex = regex;
        if (regex) {
            this.pattern = Pattern.compile(wildcardToJavaRegexp(deployment));
        } else {
            this.pattern = null;
        }
    }

    @Override
    public void start(final StartContext context) throws StartException {
        deploymentOverlayIndexServiceInjectedValue.getValue().addService(this);
    }

    @Override
    public void stop(final StopContext context) {
        deploymentOverlayIndexServiceInjectedValue.getValue().removeService(this);
    }

    @Override
    public DeploymentOverlayLinkService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public InjectedValue<DeploymentOverlayIndexService> getDeploymentOverlayIndexServiceInjectedValue() {
        return deploymentOverlayIndexServiceInjectedValue;
    }

    public InjectedValue<DeploymentOverlayService> getDeploymentOverlayServiceInjectedValue() {
        return deploymentOverlayServiceInjectedValue;
    }

    public DeploymentOverlayPriority getPriority() {
        return priority;
    }

    public String getDeployment() {
        return deployment;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public boolean isRegex() {
        return regex;
    }
}

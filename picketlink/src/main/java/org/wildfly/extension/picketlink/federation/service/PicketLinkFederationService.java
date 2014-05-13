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
package org.wildfly.extension.picketlink.federation.service;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.service.Service;
import org.wildfly.extension.picketlink.federation.metrics.PicketLinkSubsystemMetrics;

/**
 * <p> Interface for PicketLink specific services. </p>
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public interface PicketLinkFederationService<T> extends Service<T> {

    /**
     * <p> Configures a {@link org.jboss.as.server.deployment.DeploymentUnit} as a PicketLink deployment. For example: Identity Providers and Service Providers. </p>
     *
     * @param deploymentUnit
     */
    void configure(DeploymentUnit deploymentUnit);

    /**
     * <p> Returns a {@link PicketLinkSubsystemMetrics}. </p>
     *
     * @return
     */
    PicketLinkSubsystemMetrics getMetrics();
}

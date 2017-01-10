/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.remote;

import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.server.suspend.SuspendController;
import org.jboss.ejb.server.Association;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * The EJB server association service.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class AssociationService implements Service<Association> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb", "association");

    private final InjectedValue<DeploymentRepository> deploymentRepositoryInjector = new InjectedValue<>();
    private final InjectedValue<RegistryCollector> registryCollectorInjector = new InjectedValue<>();
    private final InjectedValue<SuspendController> suspendControllerInjector = new InjectedValue<>();

    private Association value;

    public AssociationService() {
    }

    public void start(final StartContext context) throws StartException {
        // todo clusterRegistryCollector
        // todo suspendController
        value = new AssociationImpl(deploymentRepositoryInjector.getValue());
    }

    public void stop(final StopContext context) {
        value = null;
    }

    public Association getValue() throws IllegalStateException, IllegalArgumentException {
        final Association value = this.value;
        if (value == null) throw new IllegalStateException();
        return value;
    }

    public InjectedValue<DeploymentRepository> getDeploymentRepositoryInjector() {
        return deploymentRepositoryInjector;
    }

    public InjectedValue<RegistryCollector> getRegistryCollectorInjector() {
        return registryCollectorInjector;
    }

    public InjectedValue<SuspendController> getSuspendControllerInjector() {
        return suspendControllerInjector;
    }
}


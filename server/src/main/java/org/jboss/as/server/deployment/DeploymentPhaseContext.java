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

package org.jboss.as.server.deployment;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;

/**
 * The deployment unit processor context.  Maintains state pertaining to the current cycle
 * of deployment/undeployment.  This context object will be discarded when processing is
 * complete; data which must persist for the life of the deployment should be attached to
 * the {@link DeploymentUnit}.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface DeploymentPhaseContext extends Attachable {

    /**
     * Get the service name of the current deployment phase.
     *
     * @return the deployment phase service name
     */
    ServiceName getPhaseServiceName();

    /**
     * Get the service target into which this phase should install services.  <b>Please note</b> that
     * services added via this context do <b>not</b> have any implicit dependencies by default; any root-level
     * deployment services that you add should depend on the service name of the current phase, acquired via
     * the {@link #getPhaseServiceName()} method.
     *
     * @return the service target
     */
    ServiceTarget getServiceTarget();

    /**
     * Get the service registry for the container, which may be used to look up services.
     *
     * @return the service registry
     */
    ServiceRegistry getServiceRegistry();

    /**
     * Get the persistent deployment unit context for this deployment unit.
     *
     * @return the deployment unit context
     */
    DeploymentUnit getDeploymentUnit();

    /**
     * Get the phase that this processor applies to.
     *
     * @return the phase
     */
    Phase getPhase();

    /**
     * Adds a dependency on the service to the next phase service. The service value will be make available as an attachment
     * under the {@link DeploymentPhaseContext} for the phase.
     * <p/>
     * If the attachment represents an {@link AttachmentList} type then the value is added to the attachment list.
     *
     * @param <T> the type of the injected value
     * @param serviceName The service name to add to {@link Attachments#NEXT_PHASE_DEPS}
     * @param attachmentKey The AttachmentKey to attach the service result under.
     * @throws IllegalStateException If this is the last phase
     */
    <T> void addDependency(ServiceName serviceName, AttachmentKey<T> attachmentKey);

    /**
     * Adds a dependency on the service to the next phase service.
     *
     * @param <T> the type of the injected value
     * @param serviceName the service name to add to {@link Attachments#NEXT_PHASE_DEPS}
     * @param type the type to inject
     * @param injector the injector into which the dependency value is injected
     * @throws IllegalStateException If this is the last phase
     */
    <T> void addDependency(ServiceName serviceName, Class<T> type, Injector<T> injector);

    /**
     * Adds a dependency on the service to the next phase service. The service value will be make available as an attachment to
     * the {@link DeploymentUnit}. This attachment will be removed when the phase service for the next phase stops.
     * <p/>
     * If the attachment represents an {@link AttachmentList} type then the value is added to the attachment list.
     *
     * @param <T> The type of the injected value
     * @param serviceName The service name to add to {@link Attachments#NEXT_PHASE_DEPS}
     * @param attachmentKey The AttachmentKey to attach the service result under.
     * @throws IllegalStateException If this is the last phase
     */
    <T> void addDeploymentDependency(ServiceName serviceName, AttachmentKey<T> attachmentKey);

}

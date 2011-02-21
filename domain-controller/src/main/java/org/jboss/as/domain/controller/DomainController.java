/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.controller;

import org.jboss.as.controller.ModelController;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

/**
 * @author Emanuel Muckenhuber
 */
public interface DomainController extends ModelController {

    /**
     * {@link ServiceName} under which a DomainController instance should be registered
     * with the service container of a Host Controller that is acting as the domain controller.
     */
    ServiceName SERVICE_NAME = ServiceName.JBOSS.append("domain", "controller");

    /**
     * Registers a Host Controller with this domain controller.
     *
     * @param hostControllerClient client the domain controller can use to communicate with the Host Controller.
     *
     * @return a copy of the domain level model
     */
    ModelNode addClient(final HostControllerClient hostControllerClient);

    /**
     * Deregisters a previously registered Host Controller.
     *
     * @param id the name of the previously
     *           registered Host Controller
     */
    void removeClient(final String id);

    /**
     * Get the underlying model.
     *
     * @return the model
     */
    ModelNode getDomainModel();

    /**
     * Get the operations needed to create the given profile.
     *
     * @param profileName the name of the profile
     *
     * @return the operations
     */
    ModelNode getProfileOperations(String profileName);
}

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

package org.jboss.as.host.controller;

import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.controller.FileRepository;
import org.jboss.as.model.DomainModel;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 *
 * @author John Bailey
 */
public class NewLocalDomainControllerConnection implements DomainControllerConnection, Service<DomainControllerConnection> {
    private final DomainController domainController;
    private final NewLocalDomainControllerClient client;
    private final FileRepository fileRepository;

    /**
     * Create an instance.
     *
     * @param hostController The local host controller
     * @param domainController The local domain controller
     * @param fileRepository The local file repository
     */
    public NewLocalDomainControllerConnection(final NewHostController hostController, final DomainController domainController, final FileRepository fileRepository) {
        this.domainController = domainController;
        client = new NewLocalDomainControllerClient(hostController);
        this.fileRepository = fileRepository;
    }

    public void start(StartContext context) throws StartException {
    }

    public void stop(StopContext context) {
    }

    public DomainControllerConnection getValue() throws IllegalStateException {
        return this;
    }

    /** {@inheritDoc} */
    public DomainModel register() {
        domainController.addClient(client);
        return domainController.getDomainModel();
    }

    /** {@inheritDoc} */
    public void unregister() {
        domainController.removeClient(client.getId());
    }

    /** {@inheritDoc} */
    public FileRepository getRemoteFileRepository() {
        return fileRepository;
    }
}

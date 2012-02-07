/**
 *
 */
package org.jboss.as.host.controller;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.repository.HostFileRepository;
import org.jboss.msc.service.ServiceName;

import java.io.IOException;

/**
 * Client for interacting with the master {@link DomainController} on a remote host.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface MasterDomainControllerClient extends ModelControllerClient {

    /** Standard service name to use for a service that returns a MasterDomainControllerClient */
    ServiceName SERVICE_NAME = ServiceName.JBOSS.append("domain", "controller", "connection");

    /**
     * Register with the remote domain controller
     *
     * @throws IOException if there was a problem talking to the remote host
     */
    void register() throws IOException;

    /**
     * Unregister with the remote domain controller.
     */
    void unregister();

    /**
     * Gets a {@link HostFileRepository} capable of retrieving files from the
     * master domain controller.
     *
     * @return the file repository
     */
    HostFileRepository getRemoteFileRepository();
}

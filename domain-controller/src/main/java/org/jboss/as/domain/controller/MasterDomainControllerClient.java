/**
 *
 */
package org.jboss.as.domain.controller;

import java.net.InetAddress;

import org.jboss.as.controller.ModelController;
import org.jboss.msc.service.ServiceName;

/**
 * Client for interacting with the master {@link DomainController} on a remote host.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface MasterDomainControllerClient extends ModelController {

    /** Standard service name to use for a service that returns a MasterDomainControllerClient */
    ServiceName SERVICE_NAME = ServiceName.JBOSS.append("domain", "controller", "connection");

    /**
     * Register with the remote domain controller
     *
     * @param host the name of this host
     * @param inetAddress the address on which this host is listening for management requests
     * @param mgmtPort the port on which this host is listening for management requests
     * @param domainController the slave domain controller on this host
     * @throws IllegalStateException if there was a problem talking to the remote host
     */
    void register(String hostName, InetAddress inetAddress, int mgmtPort, DomainControllerSlave domainController);

    /**
     * Unregister with the remote domain controller.
     */
    void unregister();

    /**
     * Gets a {@link FileRepository} capable of retrieving files from the
     * master domain controller.
     *
     * @return the file repository
     */
    FileRepository getRemoteFileRepository();
}

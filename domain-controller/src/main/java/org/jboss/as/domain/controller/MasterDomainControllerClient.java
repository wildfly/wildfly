/**
 *
 */
package org.jboss.as.domain.controller;

import org.jboss.as.controller.ModelController;
import org.jboss.msc.service.ServiceName;

/**
 * TODO add class javadoc for MasterDomainControllerClient
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 *
 */
public interface MasterDomainControllerClient extends ModelController {

    ServiceName SERVICE_NAME = ServiceName.JBOSS.append("domain", "controller", "connection");

    void register(String hostName, DomainControllerSlave domainController);

    void unregister();

    FileRepository getRemoteFileRepository();
}

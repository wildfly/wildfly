package org.jboss.as.test.clustering.cluster.ejb2.remote.bean;

import jakarta.ejb.EJBHome;

/**
 * The remote home interface for the Heartbeat Enterprise Beans 2.x bean
 *
 * @author Richard Achmatowicz
 */
public interface HeartbeatRemoteHome extends EJBHome {
    HeartbeatRemote create() throws java.rmi.RemoteException, jakarta.ejb.CreateException;
}

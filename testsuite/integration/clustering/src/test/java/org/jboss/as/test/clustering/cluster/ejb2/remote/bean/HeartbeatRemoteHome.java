package org.jboss.as.test.clustering.cluster.ejb2.remote.bean;

import javax.ejb.EJBHome;

/**
 * The remote home interface for the Heartbeat EJB 2.x bean
 *
 * @author Richard Achmatowicz
 */
public interface HeartbeatRemoteHome extends EJBHome {
    HeartbeatRemote create() throws java.rmi.RemoteException, javax.ejb.CreateException;
}

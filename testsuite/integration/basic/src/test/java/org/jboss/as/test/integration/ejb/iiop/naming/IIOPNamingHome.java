package org.jboss.as.test.integration.ejb.iiop.naming;

import jakarta.ejb.EJBHome;
import java.rmi.RemoteException;

/**
 * @author Stuart Douglas
 */
public interface IIOPNamingHome extends EJBHome {

    IIOPRemote create() throws RemoteException;

}

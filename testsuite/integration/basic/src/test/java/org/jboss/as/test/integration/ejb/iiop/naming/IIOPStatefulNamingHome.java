package org.jboss.as.test.integration.ejb.iiop.naming;

import java.rmi.RemoteException;

import javax.ejb.EJBHome;

/**
 * @author Stuart Douglas
 */
public interface IIOPStatefulNamingHome extends EJBHome {

    IIOPStatefulRemote create(int start) throws RemoteException;

}

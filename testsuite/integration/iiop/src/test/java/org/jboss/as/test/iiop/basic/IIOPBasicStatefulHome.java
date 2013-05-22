package org.jboss.as.test.iiop.basic;

import java.rmi.RemoteException;

import javax.ejb.EJBHome;

/**
 * @author Stuart Douglas
 */
public interface IIOPBasicStatefulHome extends EJBHome {

    public IIOPBasicStatefulRemote create(int state) throws RemoteException;

}

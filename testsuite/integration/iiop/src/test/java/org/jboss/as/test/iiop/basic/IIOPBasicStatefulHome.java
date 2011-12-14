package org.jboss.as.test.iiop.basic;

import javax.ejb.EJBHome;
import java.rmi.RemoteException;

/**
 * @author Stuart Douglas
 */
public interface IIOPBasicStatefulHome extends EJBHome {

    public IIOPBasicStatefulRemote create(int state) throws RemoteException;

}

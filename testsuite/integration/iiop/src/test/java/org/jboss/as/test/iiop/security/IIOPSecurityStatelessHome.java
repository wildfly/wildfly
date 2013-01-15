package org.jboss.as.test.iiop.security;

import javax.ejb.EJBHome;
import java.rmi.RemoteException;

import org.jboss.as.test.iiop.basic.IIOPBasicStatefulRemote;

/**
 * @author Stuart Douglas
 */
public interface IIOPSecurityStatelessHome extends EJBHome {

    public IIOPSecurityStatelessRemote create() throws RemoteException;

}

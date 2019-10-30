package org.jboss.as.test.iiop.security;

import java.rmi.RemoteException;
import javax.ejb.Stateless;

/**
 * @author Stuart Douglas
 */
@Stateless
public class ClientEjb {

    private IIOPSecurityStatelessHome statelessHome;


    public String testSuccess() throws RemoteException {
        IIOPSecurityStatelessRemote ejb = statelessHome.create();
        return ejb.role1();
    }

    public String testFailure() throws RemoteException {
        IIOPSecurityStatelessRemote ejb = statelessHome.create();
        return ejb.role2();
    }
}

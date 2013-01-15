package org.jboss.as.test.iiop.security;

import java.rmi.RemoteException;

import javax.ejb.EJBMetaData;
import javax.ejb.Handle;
import javax.ejb.HomeHandle;
import javax.ejb.Stateless;
import javax.rmi.PortableRemoteObject;

import junit.framework.Assert;
import org.jboss.as.test.iiop.basic.IIOPBasicHome;
import org.jboss.as.test.iiop.basic.IIOPBasicRemote;
import org.jboss.as.test.iiop.basic.IIOPBasicStatefulHome;
import org.jboss.as.test.iiop.basic.IIOPBasicStatefulRemote;
import org.jboss.ejb.iiop.HandleImplIIOP;

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

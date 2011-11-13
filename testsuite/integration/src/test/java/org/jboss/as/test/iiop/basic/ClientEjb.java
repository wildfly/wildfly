package org.jboss.as.test.iiop.basic;

import java.rmi.RemoteException;

import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * @author Stuart Douglas
 */
@Stateless
public class ClientEjb {

    @EJB(lookup = "corbaname:iiop:localhost:3628#server/IIOPBasicBean")
    private IIOPBasicHome home;

    public String getRemoteMessage() throws RemoteException {
        return home.create().hello();
    }


}

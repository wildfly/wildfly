package org.jboss.as.test.integration.ejb.iiop.naming;

import jakarta.ejb.EJBObject;
import java.rmi.RemoteException;

/**
 * @author Stuart Douglas
 */
public interface IIOPRemote extends EJBObject {

    String hello() throws RemoteException;
}

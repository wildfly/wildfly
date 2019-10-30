package org.jboss.as.test.integration.ejb.iiop.naming;

import javax.ejb.EJBObject;
import java.rmi.RemoteException;

/**
 * @author Stuart Douglas
 */
public interface IIOPRemote extends EJBObject {

    String hello() throws RemoteException;
}

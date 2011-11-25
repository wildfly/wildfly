package org.jboss.as.test.integration.ejb.iiop.naming;

import java.rmi.RemoteException;

import javax.ejb.EJBObject;

/**
 * @author Stuart Douglas
 */
public interface IIOPStatefulRemote extends EJBObject {

    int increment() throws RemoteException;
}

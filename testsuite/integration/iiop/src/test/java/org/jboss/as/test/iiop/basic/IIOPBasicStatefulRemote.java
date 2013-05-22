package org.jboss.as.test.iiop.basic;

import java.rmi.RemoteException;

import javax.ejb.EJBObject;

/**
 * @author Stuart Douglas
 */
public interface IIOPBasicStatefulRemote extends EJBObject {

    int state() throws RemoteException;
}

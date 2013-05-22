package org.jboss.as.test.iiop.security;

import java.rmi.RemoteException;

import javax.ejb.EJBObject;

/**
 * @author Stuart Douglas
 */
public interface IIOPSecurityStatelessRemote extends EJBObject {

    String role1() throws RemoteException;

    String role2() throws RemoteException;

}

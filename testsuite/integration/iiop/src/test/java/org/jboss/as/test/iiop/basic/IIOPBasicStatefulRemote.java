package org.jboss.as.test.iiop.basic;

import javax.ejb.EJBObject;
import java.rmi.RemoteException;

/**
 * @author Stuart Douglas
 */
public interface IIOPBasicStatefulRemote extends EJBObject {

    int state() throws RemoteException;
}

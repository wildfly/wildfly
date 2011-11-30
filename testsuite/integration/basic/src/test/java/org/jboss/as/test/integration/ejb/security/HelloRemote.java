package org.jboss.as.test.integration.ejb.security;

import java.rmi.RemoteException;
import javax.ejb.EJBObject;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public interface HelloRemote extends EJBObject {
    String sayHello(String name) throws RemoteException;
}

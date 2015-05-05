package org.jboss.as.test.integration.ejb.entity.exceptions;

import javax.ejb.EJBObject;
import java.rmi.RemoteException;

public interface ExceptionsRemoteInterface extends EJBObject {

    void throwRuntimeException() throws RemoteException;

    void test() throws RemoteException;
}

package org.jboss.as.test.integration.ejb.entity.exceptions;

import javax.ejb.CreateException;
import javax.ejb.EJBHome;
import javax.ejb.FinderException;
import java.rmi.RemoteException;

public interface ExceptionsRemoteHome extends EJBHome {

    ExceptionsRemoteInterface create(String key) throws CreateException, RemoteException;

    ExceptionsRemoteInterface findByPrimaryKey(String key) throws FinderException, RemoteException;
}

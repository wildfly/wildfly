package org.jboss.as.test.integration.ejb.entity.cmp.postcreate;


import java.rmi.RemoteException;
import javax.ejb.CreateException;
import javax.ejb.EJBHome;
import javax.ejb.FinderException;

public interface BeanHome extends EJBHome {
    public Bean create(String id, String name, int value, ADVC a1, BDVC b1, int flag) throws RemoteException, CreateException;

    public Bean findByPrimaryKey(String id) throws RemoteException, FinderException;
}

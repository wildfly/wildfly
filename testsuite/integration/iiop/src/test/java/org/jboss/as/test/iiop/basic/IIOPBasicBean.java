package org.jboss.as.test.iiop.basic;

import java.rmi.RemoteException;

import javax.annotation.Resource;
import javax.ejb.RemoteHome;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

/**
 * @author Stuart Douglas
 */
@RemoteHome(IIOPBasicHome.class)
@Stateless
public class IIOPBasicBean {

    @Resource
    private SessionContext sessionContext;


    public String hello() {
        return "hello";
    }

    public HandleWrapper wrappedHandle() {
        try {
            return new HandleWrapper(sessionContext.getEJBObject().getHandle());
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

}

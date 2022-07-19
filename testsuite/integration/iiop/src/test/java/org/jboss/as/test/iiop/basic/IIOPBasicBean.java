package org.jboss.as.test.iiop.basic;

import jakarta.annotation.Resource;
import jakarta.ejb.RemoteHome;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import java.rmi.RemoteException;

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

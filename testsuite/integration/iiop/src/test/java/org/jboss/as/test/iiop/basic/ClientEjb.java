package org.jboss.as.test.iiop.basic;

import java.rmi.RemoteException;

import javax.ejb.EJB;
import javax.ejb.EJBMetaData;
import javax.ejb.Handle;
import javax.ejb.HomeHandle;
import javax.ejb.Stateless;
import javax.rmi.PortableRemoteObject;

import junit.framework.Assert;

/**
 * @author Stuart Douglas
 */
@Stateless
public class ClientEjb {

    @EJB(lookup = "corbaname:iiop:localhost:3628#server/IIOPBasicBean")
    private IIOPBasicHome home;

    public String getRemoteMessage() throws RemoteException {
        return home.create().hello();
    }

    public String getRemoteViaHomeHandleMessage() throws RemoteException {
        final HomeHandle handle = home.getHomeHandle();
        final IIOPBasicHome newHome = (IIOPBasicHome) PortableRemoteObject.narrow(handle.getEJBHome(), IIOPBasicHome.class);
        final IIOPBasicRemote object = newHome.create();
        return object.hello();
    }
    public String getRemoteViaHandleMessage() throws RemoteException {

        final IIOPBasicRemote object = home.create();
        final Handle handle = object.getHandle();
        final IIOPBasicRemote newObject = (IIOPBasicRemote) PortableRemoteObject.narrow(handle.getEJBObject(), IIOPBasicRemote.class);
        return newObject.hello();
    }
    public String getRemoteMessageViaEjbMetadata() throws RemoteException {
        final EJBMetaData metadata = home.getEJBMetaData();
        final IIOPBasicHome newHome = (IIOPBasicHome) PortableRemoteObject.narrow(metadata.getEJBHome(), IIOPBasicHome.class);
        final IIOPBasicRemote object = newHome.create();
        Assert.assertEquals(IIOPBasicHome.class, metadata.getHomeInterfaceClass());
        Assert.assertEquals(IIOPBasicRemote.class, metadata.getRemoteInterfaceClass());
        return object.hello();
    }

}

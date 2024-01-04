/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.iiop.basic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.rmi.RemoteException;

import jakarta.ejb.EJBMetaData;
import jakarta.ejb.Handle;
import jakarta.ejb.HomeHandle;
import jakarta.ejb.Stateless;
import javax.rmi.PortableRemoteObject;

import org.junit.Assert;
import org.jboss.ejb.iiop.HandleImplIIOP;

/**
 * @author Stuart Douglas
 */
@Stateless
public class ClientEjb {

    private IIOPBasicHome home;

    private IIOPBasicStatefulHome statefulHome;

    public String getRemoteMessage() throws RemoteException {
        return home.create().hello();
    }

    public String getRemoteViaHomeHandleMessage() throws RemoteException {
        final HomeHandle handle = home.getHomeHandle();
        final IIOPBasicHome newHome = (IIOPBasicHome) PortableRemoteObject.narrow(handle.getEJBHome(), IIOPBasicHome.class);
        final IIOPBasicRemote object = newHome.create();
        return object.hello();
    }


    public String getRemoteViaHandleMessage() throws RemoteException, IOException, ClassNotFoundException {

        final IIOPBasicRemote object = home.create();
        final Handle handle = serializeAndDeserialize(object.getHandle());
        final IIOPBasicRemote newObject = (IIOPBasicRemote) PortableRemoteObject.narrow(handle.getEJBObject(), IIOPBasicRemote.class);
        return newObject.hello();
    }

    public String getRemoteViaWrappedHandle() throws RemoteException, IOException, ClassNotFoundException {

        final IIOPBasicRemote object = home.create();
        final Handle handle = serializeAndDeserialize(object.wrappedHandle().getHandle());
        Assert.assertEquals(HandleImplIIOP.class, handle.getClass());
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

    public void testIsIdentical() throws RemoteException {
        final IIOPBasicStatefulRemote b1 = statefulHome.create(10);
        final IIOPBasicStatefulRemote b2 = statefulHome.create(20);
        Assert.assertTrue(b1.isIdentical(b1));
        Assert.assertFalse(b1.isIdentical(b2));

        final IIOPBasicRemote s1 = home.create();
        final IIOPBasicRemote s2 = home.create();
        Assert.assertTrue(s1.isIdentical(s1));
        Assert.assertTrue(s1.isIdentical(s2));
    }

    private static <T extends Serializable> T serializeAndDeserialize(T o) throws IOException, ClassNotFoundException {
        //serialize
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(o);
        oos.close();

        //.. and deserialize hande
        ByteArrayInputStream fileInputStream
                = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream objectInputStream
                = new ObjectInputStream(fileInputStream);
        T result = (T) objectInputStream.readObject();
        objectInputStream.close();
        return result;
    }
}

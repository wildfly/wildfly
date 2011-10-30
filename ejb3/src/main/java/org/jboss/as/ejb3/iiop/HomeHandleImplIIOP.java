/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.ejb3.iiop;

import org.jboss.as.ejb3.iiop.handle.HandleDelegateImpl;
import org.jboss.as.jacorb.service.CorbaORBService;

import javax.ejb.EJBHome;
import javax.ejb.HomeHandle;
import javax.ejb.spi.HandleDelegate;
import javax.rmi.CORBA.Stub;
import javax.rmi.PortableRemoteObject;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.RemoteException;

/**
 * A CORBA-based EJB home handle implementation.
 *
 * @author <a href="mailto:rickard.oberg@telkel.com">Rickard Öberg</a>.
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @author <a href="mailto:reverbel@ime.usp.br">Francisco Reverbel</a>
 * @author adrian@jboss.org
 * @version $Revision: 65794 $
 */
public class HomeHandleImplIIOP implements HomeHandle {
    /**
     * @since 4.2.0
     */
    static final long serialVersionUID = 8652819695513956661L;

    /**
     * This handle encapsulates an stringfied CORBA reference for an
     * <code>EJBHome</code>.
     */
    private String ior;

    /**
     * The stub class
     */
    private transient Class<?> stubClass = EJBHome.class;

    /**
     * Constructs a <code>HomeHandleImplIIOP</code>.
     *
     * @param ior An stringfied CORBA reference for an <code>EJBHome</code>.
     */
    public HomeHandleImplIIOP(final String ior) {
        this.ior = ior;
    }

    /**
     * Constructs a <tt>HomeHandleImplIIOP</tt>.
     *
     * @param home An <code>EJBHome</code>.
     */
    public HomeHandleImplIIOP(final EJBHome home) {
        this((org.omg.CORBA.Object) home);
    }

    /**
     * Constructs a <tt>HomeHandleImplIIOP</tt>.
     *
     * @param home A CORBA reference for an <code>EJBHome</code>.
     */
    public HomeHandleImplIIOP(final org.omg.CORBA.Object home) {
        this.ior = CorbaORBService.getCurrent().object_to_string(home);
        this.stubClass = home.getClass();
    }

    /**
     * Obtains the <code>EJBHome</code> represented by this home handle.
     *
     * @return a reference to an <code>EJBHome</code>.
     * @throws java.rmi.RemoteException
     */
    public EJBHome getEJBHome() throws RemoteException {
        try {
            final org.omg.CORBA.Object obj = CorbaORBService.getCurrent().string_to_object(ior);
            return narrow(obj);
        } catch (RemoteException e) {
            throw e;
        } catch (Exception e) {
            throw new RemoteException("Could not get EJBHome from HomeHandle", e);
        }
    }

    private EJBHome narrow(org.omg.CORBA.Object obj) throws ClassCastException, RemoteException {
        // Null object - this should probably throw some kind of error?
        if (obj == null)
            return null;

        // Already the correct type
        if (stubClass.isAssignableFrom(obj.getClass()))
            return (EJBHome) obj;

        // Backwards compatibility - almost certainly wrong!
        if (stubClass == EJBHome.class)
            return (EJBHome) PortableRemoteObject.narrow(obj, EJBHome.class);

        // Create the stub from the stub class
        try {
            final Stub stub = (Stub) stubClass.newInstance();
            stub._set_delegate(((org.omg.CORBA.portable.ObjectImpl) obj)._get_delegate());
            return (EJBHome) stub;
        } catch (Exception e) {
            throw new RemoteException("Error creating stub", e);
        }
    }

    private void writeObject(final ObjectOutputStream oostream) throws IOException {
        final HandleDelegate delegate = HandleDelegateImpl.getDelegate();
        delegate.writeEJBHome(getEJBHome(), oostream);
    }

    private void readObject(final ObjectInputStream oistream) throws IOException, ClassNotFoundException {
        final HandleDelegate delegate = HandleDelegateImpl.getDelegate();
        final EJBHome obj = delegate.readEJBHome(oistream);
        this.ior = CorbaORBService.getCurrent().object_to_string((org.omg.CORBA.Object) obj);
        this.stubClass = obj.getClass();
    }
}

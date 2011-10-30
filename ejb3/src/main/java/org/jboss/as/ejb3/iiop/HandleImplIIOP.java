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

import javax.ejb.EJBObject;
import javax.ejb.Handle;
import javax.ejb.spi.HandleDelegate;
import javax.rmi.CORBA.Stub;
import javax.rmi.PortableRemoteObject;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.rmi.RemoteException;

/**
 * A CORBA-based EJBObject handle implementation.
 *
 * @author <a href="mailto:rickard.oberg@telkel.com">Rickard Öberg</a>.
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @author <a href="mailto:reverbel@ime.usp.br">Francisco Reverbel</a>
 * @version $Revision: 65011 $
 */
public class HandleImplIIOP implements Handle, Serializable {
    /**
     * @since 4.0.0
     */
    static final long serialVersionUID = -501170775289648475L;

    /**
     * This handle encapsulates an stringfied CORBA reference for an
     * <code>EJBObject</code>.
     */
    private String ior;

    /**
     * The stub class
     */
    private transient Class<?> stubClass = EJBObject.class;

    /**
     * Constructs an <code>HandleImplIIOP</code>.
     *
     * @param ior An stringfied CORBA reference for an <code>EJBObject</code>.
     */
    public HandleImplIIOP(String ior) {
        this.ior = ior;
    }

    /**
     * Constructs an <tt>HandleImplIIOP</tt>.
     *
     * @param obj An <code>EJBObject</code>.
     */
    public HandleImplIIOP(EJBObject obj) {
        this((org.omg.CORBA.Object) obj);
    }

    /**
     * Constructs an <tt>HandleImplIIOP</tt>.
     *
     * @param obj A CORBA reference for an <code>EJBObject</code>.
     */
    public HandleImplIIOP(org.omg.CORBA.Object obj) {
        this.ior = CorbaORBService.getCurrent().object_to_string(obj);
        this.stubClass = obj.getClass();
    }

    // Public --------------------------------------------------------

    // Handle implementation -----------------------------------------

    /**
     * Obtains the <code>EJBObject</code> represented by this handle.
     *
     * @return a reference to an <code>EJBObject</code>.
     * @throws java.rmi.RemoteException
     */
    public EJBObject getEJBObject() throws RemoteException {
        try {
            org.omg.CORBA.Object obj = CorbaORBService.getCurrent().string_to_object(ior);
            return narrow(obj);
        } catch (RemoteException e) {
            throw e;
        } catch (Exception e) {
            throw new RemoteException("Could not get EJBObject from Handle", e);
        }
    }

    private EJBObject narrow(org.omg.CORBA.Object obj) throws ClassCastException, RemoteException {
        // Null object - this should probably throw some kind of error?
        if (obj == null)
            return null;

        // Already the correct type
        if (stubClass.isAssignableFrom(obj.getClass()))
            return (EJBObject) obj;

        // Backwards compatibility - almost certainly wrong!
        if (stubClass == EJBObject.class)
            return (EJBObject) PortableRemoteObject.narrow(obj, EJBObject.class);

        // Create the stub from the stub class
        try {
            Stub stub = (Stub) stubClass.newInstance();
            stub._set_delegate(((org.omg.CORBA.portable.ObjectImpl) obj)._get_delegate());
            return (EJBObject) stub;
        } catch (Exception e) {
            throw new RemoteException("Error creating stub", e);
        }
    }

    private void writeObject(ObjectOutputStream oostream) throws IOException {
        HandleDelegate delegate = HandleDelegateImpl.getDelegate();
        delegate.writeEJBObject(getEJBObject(), oostream);
    }

    private void readObject(ObjectInputStream oistream) throws IOException, ClassNotFoundException {
        HandleDelegate delegate = HandleDelegateImpl.getDelegate();
        EJBObject obj = delegate.readEJBObject(oistream);
        this.ior = CorbaORBService.getCurrent().object_to_string((org.omg.CORBA.Object) obj);
        this.stubClass = obj.getClass();
    }
}

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.ejb3.iiop.handle;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.ejb.EJBHome;
import javax.ejb.EJBObject;
import javax.ejb.spi.HandleDelegate;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.CORBA.Stub;
import javax.rmi.PortableRemoteObject;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.omg.CORBA.BAD_OPERATION;
import org.omg.CORBA.ORB;
import org.omg.CORBA.portable.ObjectImpl;

/**
 * <P>Implementation of the javax.ejb.spi.HandleDelegate interface</P>
 * <p/>
 * <P>The HandleDelegate interface is implemented by the EJB container.
 * It is used by portable implementations of javax.ejb.Handle and
 * javax.ejb.HomeHandle. It is not used by EJB components or by client components.
 * It provides methods to serialize and deserialize EJBObject and EJBHome
 * references to streams.</P>
 * <p/>
 * <P>The HandleDelegate object is obtained by JNDI lookup at the reserved name
 * "java:comp/HandleDelegate".</P>
 *
 * @author Dimitris.Andreadis@jboss.org
 * @author adrian@jboss.com
 */
public class HandleDelegateImpl implements HandleDelegate {

    public HandleDelegateImpl(final ClassLoader classLoader) {
        proxy = SerializationHackProxy.proxy(classLoader);
    }

    private final SerializationHackProxy proxy;

    public void writeEJBObject(final EJBObject ejbObject, final ObjectOutputStream oostream)
            throws IOException {
        oostream.writeObject(ejbObject);
    }

    public EJBObject readEJBObject(final ObjectInputStream oistream)
            throws IOException, ClassNotFoundException {
        final Object ejbObject = proxy.read(oistream);
        reconnect(ejbObject);
        return (EJBObject) PortableRemoteObject.narrow(ejbObject, EJBObject.class);
    }

    public void writeEJBHome(final EJBHome ejbHome, final ObjectOutputStream oostream)
            throws IOException {
        oostream.writeObject(ejbHome);
    }

    public EJBHome readEJBHome(final ObjectInputStream oistream)
            throws IOException, ClassNotFoundException {
        final Object ejbHome = proxy.read(oistream);
        reconnect(ejbHome);
        return (EJBHome) PortableRemoteObject.narrow(ejbHome, EJBHome.class);
    }

    protected void reconnect(Object object) throws IOException {
        if (object instanceof ObjectImpl) {
            try {
                // Check we are still connected
                ObjectImpl objectImpl = (ObjectImpl) object;
                objectImpl._get_delegate();
            } catch (BAD_OPERATION e) {
                try {
                    // Reconnect
                    final Stub stub = (Stub) object;
                    final ORB orb = (ORB) new InitialContext().lookup("java:comp/ORB");
                    stub.connect(orb);
                } catch (NamingException ne) {
                    throw EjbLogger.ROOT_LOGGER.failedToLookupORB();
                }
            }
        } else {
            throw EjbLogger.ROOT_LOGGER.notAnObjectImpl(object.getClass());
        }
    }

    public static HandleDelegate getDelegate() {
        try {
            final InitialContext ctx = new InitialContext();
            return (HandleDelegate) ctx.lookup("java:comp/HandleDelegate");
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }
}

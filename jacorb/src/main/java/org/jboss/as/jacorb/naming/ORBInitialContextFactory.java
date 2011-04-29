/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jacorb.naming;

import org.jboss.as.naming.InitialContextFactory;
import org.omg.CORBA.ORB;

import javax.naming.Context;
import javax.naming.NamingException;
import java.util.Hashtable;

/**
 * <p>
 * An {@code org.jboss.as.naming.InitialContextFactory} that includes the ORB in the environment.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
@SuppressWarnings("unchecked")
public class ORBInitialContextFactory extends InitialContextFactory {

    public static final String ORB_INSTANCE = "java.naming.corba.orb";

    /**
     * The orb to include in the naming environment - must be serializable.
     */
    private static ORB orb;

    /**
     * <p>
     * Obtains the serializable ORB that is included in the naming environment.
     * </p>
     *
     * @return a reference to the {@code ORB}.
     */
    public static ORB getORB() {
        return orb;
    }

    /**
     * <p>
     * Sets the ORB that will be included in the naming environment whenever an {@code InitialContext} is created.
     * </p>
     *
     * @param orb a reference to the {@code ORB} that will be included in the naming environment.
     */
    public static void setORB(ORB orb) {
        if (orb == null)
            ORBInitialContextFactory.orb = null;
        else
            ORBInitialContextFactory.orb = new SerializableORB(orb);
    }

    @Override
    public Context getInitialContext(Hashtable env) throws NamingException {
        if (orb != null && !env.containsKey(ORB_INSTANCE))
            env.put(ORB_INSTANCE, orb);
        return super.getInitialContext(env);
    }
}
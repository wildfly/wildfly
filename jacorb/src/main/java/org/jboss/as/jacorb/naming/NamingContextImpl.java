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

import org.jboss.logging.Logger;
import org.omg.CosNaming.NamingContext;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.PortableServer.POA;

/**
 * <p>
 * This subclass of {@code org.jacorb.naming.NamingContextImpl} overrides the method {@code new_context()} because its
 * implementation in {@code org.jacorb.naming.NamingContextImpl} is not suitable for our in-VM naming server. The
 * superclass implementation of {@code new_context()} assumes that naming context states are* persistently stored and
 * requires a servant activator that reads context states from persistent storage.
 * </p>
 *
 * @author <a href="mailto:reverbel@ime.usp.br">Francisco Reverbel</a>
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class NamingContextImpl extends org.jacorb.naming.NamingContextImpl {

    private POA poa;
    private int childCount = 0;
    private static final Logger logger = Logger.getLogger("org.jboss.as.jacorb");

    public NamingContextImpl(POA poa) {
        this.poa = poa;
    }

    @Override
    public NamingContext new_context() {
        try {
            NamingContextImpl newContextImpl = new NamingContextImpl(poa);
            byte[] oid = (new String(poa.servant_to_id(this)) + "/ctx" + (++childCount)).getBytes();
            poa.activate_object_with_id(oid, newContextImpl);
            return NamingContextExtHelper.narrow(poa.create_reference_with_id(oid,
                    "IDL:omg.org/CosNaming/NamingContextExt:1.0"));
        } catch (Exception e) {
            logger.error("Cannot create CORBA naming context", e);
            return null;
        }
    }
}

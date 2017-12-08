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
package org.jboss.as.ejb3.iiop;

import org.omg.PortableServer.POA;

/**
 * Interface of a CORBA reference factory. Such a factory encapsulates a POA
 * and provides reference creation methods.
 *
 * @author <a href="mailto:reverbel@ime.usp.br">Francisco Reverbel</a>
 */
public interface ReferenceFactory {

    /**
     * Creates a reference with a null id in its "reference data" and
     * with object type information given by the <code>interfId</code>
     * parameter.
     */
    org.omg.CORBA.Object createReference(String inferfId) throws Exception;

    /**
     * Creates a reference with the specified <code>id</code> in its
     * "reference data" and with object type information given by the
     * <code>interfId</code> parameter.
     */
    org.omg.CORBA.Object createReferenceWithId(byte[] id, String interfId)
            throws Exception;

    /**
     * Returns a reference to the POA encapsulated by this
     * <code>ReferenceFactory</code>.
     */
    POA getPOA();

}

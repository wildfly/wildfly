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

import org.omg.CORBA.Policy;
import org.omg.PortableServer.Servant;

/**
 * Interface of a registry for CORBA servants.
 *
 * @author <a href="mailto:reverbel@ime.usp.br">Francisco Reverbel</a>
 * @version $Revision: 81018 $
 */
public interface ServantRegistry {

    /**
     * Binds <code>name</code> to <code>servant</code>, with the given
     * <code>policies</code>. Returns a <code>ReferenceFactory</code>
     * that should be used to create CORBA references to the object(s)
     * implemented by <code>servant</code>. A CORBA reference created by this
     * factory will contain <code>name</code> as the servant id embedded in the
     * reference. If the servant implements more than one CORBA object,
     * references for such objects should be created by the
     * <code>ReferenceFactory</code> method
     * <code>createReferenceWithId()</code>, which takes an <code>id</code>
     * parameter to distinguish among the objects implemented by the same
     * servant. Otherwise (if the servant implements a single CORBA object)
     * the method <code>createReference()</code> should be used.
     */
    ReferenceFactory bind(String name, Servant servant, Policy[] policies) throws Exception;

    /**
     * Binds <code>name</code> to <code>servant</code>. Returns a
     * <code>ReferenceFactory</code> that should be used to create CORBA
     * references to the object(s) implemented by <code>servant</code>.
     * For the usage of this <code>ReferenceFactory</code>, see method
     * above.
     */
    ReferenceFactory bind(String name, Servant servant) throws Exception;

    /**
     * Unbinds the servant bound to <code>name</code>.
     */
    void unbind(String name)  throws Exception;

}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

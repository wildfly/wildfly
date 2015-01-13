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
package org.wildfly.iiop.openjdk.rmi.ir;

import org.omg.CORBA.Any;
import org.omg.CORBA.ContainedOperations;
import org.omg.CORBA.ContainedPackage.Description;
import org.omg.CORBA.DefinitionKind;
import org.omg.CORBA.ExceptionDef;
import org.omg.CORBA.ExceptionDescription;
import org.omg.CORBA.ExceptionDescriptionHelper;
import org.omg.CORBA.IDLType;
import org.omg.CORBA.IDLTypeHelper;
import org.omg.CORBA.IRObject;
import org.omg.CORBA.OperationDef;
import org.omg.CORBA.OperationDefOperations;
import org.omg.CORBA.OperationDefPOATie;
import org.omg.CORBA.OperationDescription;
import org.omg.CORBA.OperationDescriptionHelper;
import org.omg.CORBA.OperationMode;
import org.omg.CORBA.ParameterDescription;
import org.omg.CORBA.TypeCode;
import org.wildfly.iiop.openjdk.logging.IIOPLogger;

/**
 * OperationDef IR object.
 *
 * @author <a href="mailto:osh@sparre.dk">Ole Husgaard</a>
 * @version $Revision: 81018 $
 */
class OperationDefImpl
        extends ContainedImpl
        implements OperationDefOperations {
    // Constants -----------------------------------------------------

    // Attributes ----------------------------------------------------

    // Static --------------------------------------------------------

    // Constructors --------------------------------------------------

    OperationDefImpl(String id, String name, String version,
                     LocalContainer defined_in,
                     TypeCode typeCode,
                     ParameterDescription[] params,
                     ExceptionDef[] exceptions,
                     RepositoryImpl repository) {
        super(id, name, version, defined_in,
                DefinitionKind.dk_Operation, repository);

        this.typeCode = typeCode;
        this.params = params;
        this.exceptions = exceptions;
    }

    // Public --------------------------------------------------------


    // LocalIRObject implementation ---------------------------------

    public IRObject getReference() {
        if (ref == null) {
            ref = org.omg.CORBA.OperationDefHelper.narrow(
                    servantToReference(new OperationDefPOATie(this)));
        }
        return ref;
    }

    public void allDone()
            throws IRConstructionException {
        // Get my return type definition: It should have been created now.
        type_def = IDLTypeImpl.getIDLType(typeCode, repository);

        // resolve parameter type definitions
        for (int i = 0; i < params.length; ++i) {
            LocalIDLType lit = IDLTypeImpl.getIDLType(params[i].type, repository);
            if (lit == null)
                throw new RuntimeException();
            params[i].type_def = IDLTypeHelper.narrow(lit.getReference());
            if (params[i].type_def == null)
                throw new RuntimeException();
        }

        getReference();
    }


    // OperationDefOperations implementation ----------------------------

    public TypeCode result() {
        return typeCode;
    }

    public IDLType result_def() {
        return IDLTypeHelper.narrow(type_def.getReference());
    }

    public void result_def(IDLType arg) {
        throw IIOPLogger.ROOT_LOGGER.cannotChangeRMIIIOPMapping();
    }

    public ParameterDescription[] params() {
        return params;
    }

    public void params(ParameterDescription[] arg) {
        throw IIOPLogger.ROOT_LOGGER.cannotChangeRMIIIOPMapping();
    }

    public OperationMode mode() {
        // RMI/IIOP never map to oneway operations.
        return OperationMode.OP_NORMAL;
    }

    public void mode(OperationMode arg) {
        throw IIOPLogger.ROOT_LOGGER.cannotChangeRMIIIOPMapping();
    }

    public String[] contexts() {
        // TODO
        return new String[0];
    }

    public void contexts(String[] arg) {
        throw IIOPLogger.ROOT_LOGGER.cannotChangeRMIIIOPMapping();
    }

    public ExceptionDef[] exceptions() {
        return exceptions;
    }

    public void exceptions(ExceptionDef[] arg) {
        throw IIOPLogger.ROOT_LOGGER.cannotChangeRMIIIOPMapping();
    }

    // ContainedImpl implementation ----------------------------------

    public Description describe() {
        String defined_in_id = "IR";

        if (defined_in instanceof ContainedOperations)
            defined_in_id = ((ContainedOperations) defined_in).id();

        ExceptionDescription[] exds;
        exds = new ExceptionDescription[exceptions.length];
        for (int i = 0; i < exceptions.length; ++i) {
            Description d = exceptions[i].describe();
            exds[i] = ExceptionDescriptionHelper.extract(d.value);
        }

        OperationDescription od;
        od = new OperationDescription(name, id, defined_in_id, version, typeCode,
                mode(), contexts(), params(), exds);

        Any any = getORB().create_any();

        OperationDescriptionHelper.insert(any, od);

        return new Description(DefinitionKind.dk_Operation, any);
    }


    // Package protected ---------------------------------------------

    // Private -------------------------------------------------------

    /**
     * My CORBA reference.
     */
    private OperationDef ref = null;

    /**
     * My result TypeCode.
     */
    private TypeCode typeCode;

    /**
     * My result type definition.
     */
    private LocalIDLType type_def;

    /**
     * My parameter description.
     */
    private ParameterDescription[] params;

    /**
     * My exceptions.
     */
    private ExceptionDef[] exceptions;

}


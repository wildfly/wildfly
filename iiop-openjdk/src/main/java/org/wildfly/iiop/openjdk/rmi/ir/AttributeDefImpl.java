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
import org.omg.CORBA.AttributeDef;
import org.omg.CORBA.AttributeDefOperations;
import org.omg.CORBA.AttributeDefPOATie;
import org.omg.CORBA.AttributeDescription;
import org.omg.CORBA.AttributeDescriptionHelper;
import org.omg.CORBA.AttributeMode;
import org.omg.CORBA.ContainedOperations;
import org.omg.CORBA.ContainedPackage.Description;
import org.omg.CORBA.DefinitionKind;
import org.omg.CORBA.IDLType;
import org.omg.CORBA.IDLTypeHelper;
import org.omg.CORBA.IRObject;
import org.omg.CORBA.TypeCode;
import org.wildfly.iiop.openjdk.logging.IIOPLogger;

/**
 * Attribute IR object.
 *
 * @author <a href="mailto:osh@sparre.dk">Ole Husgaard</a>
 * @version $Revision: 81018 $
 */
public class AttributeDefImpl
        extends ContainedImpl
        implements AttributeDefOperations {
    // Constants -----------------------------------------------------

    // Attributes ----------------------------------------------------

    // Static --------------------------------------------------------

    // Constructors --------------------------------------------------

    AttributeDefImpl(String id, String name, String version,
                     AttributeMode mode, TypeCode typeCode,
                     LocalContainer defined_in, RepositoryImpl repository) {
        super(id, name, version, defined_in,
                DefinitionKind.dk_Attribute, repository);

        this.mode = mode;
        this.typeCode = typeCode;
    }

    // Public --------------------------------------------------------


    // LocalIRObject implementation ---------------------------------

    public IRObject getReference() {
        if (ref == null) {
            ref = org.omg.CORBA.AttributeDefHelper.narrow(
                    servantToReference(new AttributeDefPOATie(this)));
        }
        return ref;
    }

    public void allDone()
            throws IRConstructionException {
        // Get my type definition: It should have been created now.
        type_def = IDLTypeImpl.getIDLType(typeCode, repository);

        getReference();
    }


    // AttributeDefOperations implementation ----------------------------

    public TypeCode type() {
        return typeCode;
    }

    public IDLType type_def() {
        return IDLTypeHelper.narrow(type_def.getReference());
    }

    public void type_def(IDLType arg) {
        throw IIOPLogger.ROOT_LOGGER.cannotChangeRMIIIOPMapping();
    }

    public AttributeMode mode() {
        return mode;
    }

    public void mode(AttributeMode arg) {
        throw IIOPLogger.ROOT_LOGGER.cannotChangeRMIIIOPMapping();
    }


    // ContainedImpl implementation ----------------------------------

    public Description describe() {
        String defined_in_id = "IR";

        if (defined_in instanceof ContainedOperations)
            defined_in_id = ((ContainedOperations) defined_in).id();

        AttributeDescription d =
                new AttributeDescription(name, id, defined_in_id, version,
                        typeCode, mode);

        Any any = getORB().create_any();

        AttributeDescriptionHelper.insert(any, d);

        return new Description(DefinitionKind.dk_Attribute, any);
    }

    // Y overrides ---------------------------------------------------

    // Package protected ---------------------------------------------

    // Protected -----------------------------------------------------

    // Private -------------------------------------------------------

    /**
     * My CORBA reference.
     */
    private AttributeDef ref = null;


    /**
     * My mode.
     */
    private AttributeMode mode;

    /**
     * My TypeCode.
     */
    private TypeCode typeCode;

    /**
     * My type definition.
     */
    private LocalIDLType type_def;


    // Inner classes -------------------------------------------------
}


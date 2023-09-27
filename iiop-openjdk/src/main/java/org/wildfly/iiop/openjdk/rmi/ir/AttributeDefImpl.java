/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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


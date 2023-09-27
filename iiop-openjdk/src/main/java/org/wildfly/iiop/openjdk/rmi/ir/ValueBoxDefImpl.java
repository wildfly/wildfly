/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.iiop.openjdk.rmi.ir;

import org.omg.CORBA.DefinitionKind;
import org.omg.CORBA.IDLType;
import org.omg.CORBA.IDLTypeHelper;
import org.omg.CORBA.IRObject;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.TypeCodePackage.BadKind;
import org.omg.CORBA.ValueBoxDef;
import org.omg.CORBA.ValueBoxDefOperations;
import org.omg.CORBA.ValueBoxDefPOATie;
import org.wildfly.iiop.openjdk.logging.IIOPLogger;

/**
 * ValueBoxDef IR object.
 *
 * @author <a href="mailto:osh@sparre.dk">Ole Husgaard</a>
 * @version $Revision: 81018 $
 */
class ValueBoxDefImpl extends TypedefDefImpl implements ValueBoxDefOperations {

    ValueBoxDefImpl(String id, String name, String version,
                    LocalContainer defined_in,
                    TypeCode typeCode, RepositoryImpl repository) {
        super(id, name, version, defined_in, typeCode,
                DefinitionKind.dk_ValueBox, repository);
    }

    public IRObject getReference() {
        if (ref == null) {
            ref = org.omg.CORBA.ValueBoxDefHelper.narrow(
                    servantToReference(new ValueBoxDefPOATie(this)));
        }
        return ref;
    }

    public void allDone()
            throws IRConstructionException {
        // Get my original type definition: It should have been created now.
        try {
            original_type_def = IDLTypeImpl.getIDLType(type().content_type(),
                    repository);
        } catch (BadKind ex) {
            throw IIOPLogger.ROOT_LOGGER.badKindForTypeCode(type().kind().value());
        }

        getReference();
    }

    public IDLType original_type_def() {
        return IDLTypeHelper.narrow(original_type_def.getReference());
    }

    public void original_type_def(IDLType arg) {
        throw IIOPLogger.ROOT_LOGGER.cannotChangeRMIIIOPMapping();
    }

    /**
     * My CORBA reference.
     */
    private ValueBoxDef ref = null;

    /**
     * My original IDL type.
     */
    private LocalIDLType original_type_def;
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.iiop.openjdk.rmi.ir;

import org.omg.CORBA.TypeCode;
import org.omg.CORBA.TypeCodePackage.BadKind;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.DefinitionKind;
import org.wildfly.iiop.openjdk.logging.IIOPLogger;

/**
 * IDLType IR object.
 *
 * @author <a href="mailto:osh@sparre.dk">Ole Husgaard</a>
 * @version $Revision: 81018 $
 */
abstract class IDLTypeImpl
        extends IRObjectImpl
        implements LocalIDLType {
    // Constants -----------------------------------------------------

    // Attributes ----------------------------------------------------

    // Static --------------------------------------------------------

    // Constructors --------------------------------------------------

    IDLTypeImpl(TypeCode typeCode, DefinitionKind def_kind,
                RepositoryImpl repository) {
        super(def_kind, repository);

        this.typeCode = typeCode;
    }

    // Public --------------------------------------------------------

    // IDLTypeOperations implementation ---------------------------------

    public TypeCode type() {
        return typeCode;
    }

    // Package protected ---------------------------------------------

    /**
     * Return the LocalIDLType for the given TypeCode.
     */
    static LocalIDLType getIDLType(TypeCode typeCode, RepositoryImpl repository) {
        TCKind tcKind = typeCode.kind();

        if (PrimitiveDefImpl.isPrimitiveTCKind(tcKind))
            return new PrimitiveDefImpl(typeCode, repository);

        if (tcKind == TCKind.tk_sequence)
            return repository.getSequenceImpl(typeCode);

        if (tcKind == TCKind.tk_value || tcKind == TCKind.tk_value_box ||
                tcKind == TCKind.tk_alias || tcKind == TCKind.tk_struct ||
                tcKind == TCKind.tk_union || tcKind == TCKind.tk_enum ||
                tcKind == TCKind.tk_objref) {
            try {
                return (LocalIDLType) repository._lookup_id(typeCode.id());
            } catch (BadKind ex) {
                throw IIOPLogger.ROOT_LOGGER.badKindForTypeCode(tcKind.value());
            }
        }

        throw IIOPLogger.ROOT_LOGGER.badKindForTypeCode(tcKind.value());
    }

    // Protected -----------------------------------------------------

    /**
     * Return the POA object ID of this IR object.
     * We delegate to the IR to get a serial number ID.
     */
    protected byte[] getObjectId() {
        return repository.getNextObjectId();
    }

    // Private -------------------------------------------------------

    /**
     * My TypeCode.
     */
    private TypeCode typeCode;

}


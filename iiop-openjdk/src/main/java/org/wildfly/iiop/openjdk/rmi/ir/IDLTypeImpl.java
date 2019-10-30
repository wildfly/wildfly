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


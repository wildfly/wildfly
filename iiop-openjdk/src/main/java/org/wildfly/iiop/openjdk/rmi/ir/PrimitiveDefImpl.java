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
import org.omg.CORBA.TCKind;
import org.omg.CORBA.IRObject;
import org.omg.CORBA.DefinitionKind;
import org.omg.CORBA.PrimitiveKind;
import org.omg.CORBA.PrimitiveDef;
import org.omg.CORBA.PrimitiveDefOperations;
import org.omg.CORBA.PrimitiveDefPOATie;

import java.util.Map;
import java.util.HashMap;

/**
 * PrimitiveDef IR object.
 *
 * @author <a href="mailto:osh@sparre.dk">Ole Husgaard</a>
 * @version $Revision: 81018 $
 */
class PrimitiveDefImpl
        extends IDLTypeImpl
        implements PrimitiveDefOperations, LocalIDLType {
    // Constants -----------------------------------------------------

    // Attributes ----------------------------------------------------

    // Static --------------------------------------------------------

    // Constructors --------------------------------------------------

    PrimitiveDefImpl(TypeCode typeCode,
                     RepositoryImpl repository) {
        super(typeCode, DefinitionKind.dk_Primitive, repository);
    }

    // Public --------------------------------------------------------


    // LocalIRObject implementation ---------------------------------

    public IRObject getReference() {
        if (ref == null) {
            ref = org.omg.CORBA.PrimitiveDefHelper.narrow(
                    servantToReference(new PrimitiveDefPOATie(this)));
        }
        return ref;
    }


    // PrimitiveDefOperations implementation ----------------------------

    public PrimitiveKind kind() {
        return (PrimitiveKind) primitiveTCKindMap.get(type().kind());
    }

    // Package protected ---------------------------------------------

    static boolean isPrimitiveTCKind(TCKind tcKind) {
        return primitiveTCKindMap.containsKey(tcKind);
    }

    // Private -------------------------------------------------------

    /**
     * My CORBA reference.
     */
    private PrimitiveDef ref = null;

    /**
     * Maps TCKind to PrimitiveKind.
     */
    private static Map primitiveTCKindMap;

    static {
        // Create and initialize the map
        primitiveTCKindMap = new HashMap();
        primitiveTCKindMap.put(TCKind.tk_null, PrimitiveKind.pk_null);
        primitiveTCKindMap.put(TCKind.tk_void, PrimitiveKind.pk_void);
        primitiveTCKindMap.put(TCKind.tk_short, PrimitiveKind.pk_short);
        primitiveTCKindMap.put(TCKind.tk_long, PrimitiveKind.pk_long);
        primitiveTCKindMap.put(TCKind.tk_ushort, PrimitiveKind.pk_ushort);
        primitiveTCKindMap.put(TCKind.tk_ulong, PrimitiveKind.pk_ulong);
        primitiveTCKindMap.put(TCKind.tk_float, PrimitiveKind.pk_float);
        primitiveTCKindMap.put(TCKind.tk_double, PrimitiveKind.pk_double);
        primitiveTCKindMap.put(TCKind.tk_boolean, PrimitiveKind.pk_boolean);
        primitiveTCKindMap.put(TCKind.tk_char, PrimitiveKind.pk_char);
        primitiveTCKindMap.put(TCKind.tk_octet, PrimitiveKind.pk_octet);
        primitiveTCKindMap.put(TCKind.tk_any, PrimitiveKind.pk_any);
        primitiveTCKindMap.put(TCKind.tk_TypeCode, PrimitiveKind.pk_TypeCode);
        primitiveTCKindMap.put(TCKind.tk_Principal, PrimitiveKind.pk_Principal);
        primitiveTCKindMap.put(TCKind.tk_objref, PrimitiveKind.pk_objref);
        primitiveTCKindMap.put(TCKind.tk_string, PrimitiveKind.pk_string);
        primitiveTCKindMap.put(TCKind.tk_longlong, PrimitiveKind.pk_longlong);
        primitiveTCKindMap.put(TCKind.tk_ulonglong, PrimitiveKind.pk_ulonglong);
        primitiveTCKindMap.put(TCKind.tk_longdouble, PrimitiveKind.pk_longdouble);
        primitiveTCKindMap.put(TCKind.tk_wchar, PrimitiveKind.pk_wchar);
        primitiveTCKindMap.put(TCKind.tk_wstring, PrimitiveKind.pk_wstring);
    }

}


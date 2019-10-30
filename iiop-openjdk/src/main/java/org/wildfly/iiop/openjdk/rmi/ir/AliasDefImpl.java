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

import org.omg.CORBA.AliasDef;
import org.omg.CORBA.AliasDefOperations;
import org.omg.CORBA.AliasDefPOATie;
import org.omg.CORBA.DefinitionKind;
import org.omg.CORBA.IDLType;
import org.omg.CORBA.IDLTypeHelper;
import org.omg.CORBA.IRObject;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.TypeCodePackage.BadKind;
import org.wildfly.iiop.openjdk.logging.IIOPLogger;

/**
 * AliasDef IR object.
 *
 * @author <a href="mailto:osh@sparre.dk">Ole Husgaard</a>
 * @version $Revision: 81018 $
 */
class AliasDefImpl extends TypedefDefImpl implements AliasDefOperations {

    AliasDefImpl(String id, String name, String version,
                 LocalContainer defined_in,
                 TypeCode typeCode, RepositoryImpl repository) {
        super(id, name, version, defined_in, typeCode,
                DefinitionKind.dk_Alias, repository);
    }

    public IRObject getReference() {
        if (ref == null) {
            ref = org.omg.CORBA.AliasDefHelper.narrow(
                    servantToReference(new AliasDefPOATie(this)));
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
    private AliasDef ref = null;

    /**
     * My original IDL type.
     */
    private LocalIDLType original_type_def;
}

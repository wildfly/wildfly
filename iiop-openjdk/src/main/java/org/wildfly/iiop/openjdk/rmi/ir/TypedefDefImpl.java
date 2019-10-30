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
import org.omg.CORBA.DefinitionKind;
import org.omg.CORBA.TypedefDefOperations;
import org.omg.CORBA.Any;
import org.omg.CORBA.TypeDescription;
import org.omg.CORBA.TypeDescriptionHelper;
import org.omg.CORBA.ContainedOperations;
import org.omg.CORBA.ContainedPackage.Description;

/**
 * TypedefDef IR object.
 *
 * @author <a href="mailto:osh@sparre.dk">Ole Husgaard</a>
 * @version $Revision: 81018 $
 */
abstract class TypedefDefImpl
        extends ContainedImpl
        implements TypedefDefOperations, LocalContainedIDLType {

    TypedefDefImpl(String id, String name, String version,
                   LocalContainer defined_in, TypeCode typeCode,
                   DefinitionKind def_kind, RepositoryImpl repository) {
        super(id, name, version, defined_in,
                def_kind, repository);

        this.typeCode = typeCode;
    }

    // Public --------------------------------------------------------

    // ContainedImpl implementation ----------------------------------

    public Description describe() {
        String defined_in_id = "IR";

        if (defined_in instanceof ContainedOperations)
            defined_in_id = ((ContainedOperations) defined_in).id();

        TypeDescription td = new TypeDescription(name, id, defined_in_id,
                version, typeCode);

        Any any = getORB().create_any();

        TypeDescriptionHelper.insert(any, td);

        return new Description(DefinitionKind.dk_Typedef, any);
    }

    // IDLTypeOperations implementation -------------------------------

    public TypeCode type() {
        return typeCode;
    }

    // Package protected ---------------------------------------------

    // Private -------------------------------------------------------

    /**
     * My TypeCode.
     */
    private TypeCode typeCode;
}

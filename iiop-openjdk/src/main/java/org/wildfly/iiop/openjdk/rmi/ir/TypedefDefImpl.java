/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

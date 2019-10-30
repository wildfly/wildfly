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
import org.omg.CORBA.IDLType;
import org.omg.CORBA.IDLTypeHelper;
import org.omg.CORBA.IRObject;
import org.omg.CORBA.PRIVATE_MEMBER;
import org.omg.CORBA.PUBLIC_MEMBER;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.ValueMember;
import org.omg.CORBA.ValueMemberDef;
import org.omg.CORBA.ValueMemberDefOperations;
import org.omg.CORBA.ValueMemberDefPOATie;
import org.omg.CORBA.ValueMemberHelper;
import org.wildfly.iiop.openjdk.logging.IIOPLogger;

/**
 * ValueMemberDef IR object.
 *
 * @author <a href="mailto:osh@sparre.dk">Ole Husgaard</a>
 * @version $Revision: 81018 $
 */
class ValueMemberDefImpl extends ContainedImpl implements ValueMemberDefOperations {

    ValueMemberDefImpl(String id, String name, String version,
                       TypeCode typeCode, boolean publicMember,
                       LocalContainer defined_in, RepositoryImpl repository) {
        super(id, name, version, defined_in,
                DefinitionKind.dk_ValueMember, repository);

        this.typeCode = typeCode;
        this.publicMember = publicMember;
    }

    public IRObject getReference() {
        if (ref == null) {
            ref = org.omg.CORBA.ValueMemberDefHelper.narrow(
                    servantToReference(new ValueMemberDefPOATie(this)));
        }
        return ref;
    }

    public void allDone()
            throws IRConstructionException {
        // Get my type definition: It should have been created now.
        type_def = IDLTypeImpl.getIDLType(typeCode, repository);

        getReference();
    }

    public TypeCode type() {
        return typeCode;
    }

    public IDLType type_def() {
        return IDLTypeHelper.narrow(type_def.getReference());
    }

    public void type_def(IDLType arg) {
        throw IIOPLogger.ROOT_LOGGER.cannotChangeRMIIIOPMapping();
    }

    public short access() {
        return (publicMember) ? PUBLIC_MEMBER.value : PRIVATE_MEMBER.value;
    }

    public void access(short arg) {
        throw IIOPLogger.ROOT_LOGGER.cannotChangeRMIIIOPMapping();
    }

    public Description describe() {
        String defined_in_id = "IR";

        if (defined_in instanceof ContainedOperations)
            defined_in_id = ((ContainedOperations) defined_in).id();

        ValueMember d =
                new ValueMember(name, id, defined_in_id, version,
                        typeCode, type_def(), access());

        Any any = getORB().create_any();

        ValueMemberHelper.insert(any, d);

        return new Description(DefinitionKind.dk_ValueMember, any);
    }

    /**
     * My CORBA reference.
     */
    private ValueMemberDef ref = null;

    /**
     * My TypeCode.
     */
    private TypeCode typeCode;

    /**
     * My type definition.
     */
    private LocalIDLType type_def;

    /**
     * Flags that this member is public.
     */
    private boolean publicMember;
}

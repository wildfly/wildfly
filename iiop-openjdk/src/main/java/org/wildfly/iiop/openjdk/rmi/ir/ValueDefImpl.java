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
import org.omg.CORBA.Any;
import org.omg.CORBA.AttributeDef;
import org.omg.CORBA.AttributeDescription;
import org.omg.CORBA.AttributeMode;
import org.omg.CORBA.ConstantDef;
import org.omg.CORBA.Contained;
import org.omg.CORBA.ContainedPackage.Description;
import org.omg.CORBA.DefinitionKind;
import org.omg.CORBA.EnumDef;
import org.omg.CORBA.ExceptionDef;
import org.omg.CORBA.IDLType;
import org.omg.CORBA.IRObject;
import org.omg.CORBA.Initializer;
import org.omg.CORBA.InterfaceDef;
import org.omg.CORBA.InterfaceDefHelper;
import org.omg.CORBA.ModuleDef;
import org.omg.CORBA.NativeDef;
import org.omg.CORBA.OperationDef;
import org.omg.CORBA.OperationDescription;
import org.omg.CORBA.OperationMode;
import org.omg.CORBA.ParameterDescription;
import org.omg.CORBA.StructDef;
import org.omg.CORBA.StructMember;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.TypeCodePackage.BadKind;
import org.omg.CORBA.UnionDef;
import org.omg.CORBA.UnionMember;
import org.omg.CORBA.VM_ABSTRACT;
import org.omg.CORBA.VM_CUSTOM;
import org.omg.CORBA.VM_NONE;
import org.omg.CORBA.ValueBoxDef;
import org.omg.CORBA.ValueDef;
import org.omg.CORBA.ValueDefHelper;
import org.omg.CORBA.ValueDefOperations;
import org.omg.CORBA.ValueDefPOATie;
import org.omg.CORBA.ValueDefPackage.FullValueDescription;
import org.omg.CORBA.ValueDescription;
import org.omg.CORBA.ValueDescriptionHelper;
import org.omg.CORBA.ValueMember;
import org.omg.CORBA.ValueMemberDef;
import org.wildfly.iiop.openjdk.logging.IIOPLogger;

/**
 * Interface IR object.
 *
 * @author <a href="mailto:osh@sparre.dk">Ole Husgaard</a>
 * @version $Revision: 81018 $
 */
class ValueDefImpl  extends ContainedImpl  implements ValueDefOperations, LocalContainer, LocalContainedIDLType {

    // Constructors --------------------------------------------------

    ValueDefImpl(String id, String name, String version,
                 LocalContainer defined_in,
                 boolean is_abstract, boolean is_custom,
                 String[] supported_interfaces,
                 String[] abstract_base_valuetypes,
                 TypeCode baseValueTypeCode,
                 RepositoryImpl repository) {
        super(id, name, version, defined_in,
                DefinitionKind.dk_Value, repository);

        this.is_abstract = is_abstract;
        this.is_custom = is_custom;
        this.supported_interfaces = supported_interfaces;
        this.abstract_base_valuetypes = abstract_base_valuetypes;
        this.baseValueTypeCode = baseValueTypeCode;
        this.delegate = new ContainerImplDelegate(this);
    }

    // Public --------------------------------------------------------

    // LocalContainer implementation ---------------------------------

    public LocalContained _lookup(String search_name) {
        return delegate._lookup(search_name);
    }

    public LocalContained[] _contents(DefinitionKind limit_type,
                                      boolean exclude_inherited) {
        return delegate._contents(limit_type, exclude_inherited);
    }

    public LocalContained[] _lookup_name(String search_name,
                                         int levels_to_search,
                                         DefinitionKind limit_type,
                                         boolean exclude_inherited) {
        return delegate._lookup_name(search_name, levels_to_search, limit_type,
                exclude_inherited);
    }

    public void add(String name, LocalContained contained)
            throws IRConstructionException {
        delegate.add(name, contained);
    }

    // LocalIRObject implementation ---------------------------------

    public IRObject getReference() {
        if (ref == null) {
            ref = org.omg.CORBA.ValueDefHelper.narrow(
                    servantToReference(new ValueDefPOATie(this)));
        }
        return ref;
    }

    public void allDone()
            throws IRConstructionException {
        getReference();
        delegate.allDone();

        if (baseValueTypeCode != null && baseValueTypeCode.kind() != TCKind.tk_null) {
            try {
                baseValue = baseValueTypeCode.id();
            } catch (BadKind ex) {
                throw IIOPLogger.ROOT_LOGGER.badKindForSuperValueType(id());
            }
            Contained c = repository.lookup_id(baseValue);
            base_value_ref = ValueDefHelper.narrow(c);
        } else
            baseValue = "IDL:omg.org/CORBA/ValueBase:1.0"; // TODO: is this right?

        // Resolve supported interfaces
        supported_interfaces_ref = new InterfaceDef[supported_interfaces.length];
        for (int i = 0; i < supported_interfaces.length; ++i) {
            InterfaceDef iDef = InterfaceDefHelper.narrow(
                    repository.lookup_id(supported_interfaces[i]));
            if (iDef == null)
                throw IIOPLogger.ROOT_LOGGER.errorResolvingRefToImplementedInterface(id(), supported_interfaces[i]);
            supported_interfaces_ref[i] = iDef;
        }

        // Resolve abstract base valuetypes
        abstract_base_valuetypes_ref =
                new ValueDef[abstract_base_valuetypes.length];
        for (int i = 0; i < abstract_base_valuetypes.length; ++i) {
            ValueDef vDef = ValueDefHelper.narrow(
                    repository.lookup_id(abstract_base_valuetypes[i]));
            if (vDef == null)
                throw IIOPLogger.ROOT_LOGGER.errorResolvingRefToAbstractValuetype(id(), abstract_base_valuetypes[i]);
            abstract_base_valuetypes_ref[i] = vDef;
        }
    }

    public void shutdown() {
        delegate.shutdown();
        super.shutdown();
    }


    // ContainerOperations implementation ----------------------------

    public Contained lookup(String search_name) {
        return delegate.lookup(search_name);
    }

    public Contained[] contents(DefinitionKind limit_type,
                                boolean exclude_inherited) {
        return delegate.contents(limit_type, exclude_inherited);
    }

    public Contained[] lookup_name(String search_name, int levels_to_search,
                                   DefinitionKind limit_type,
                                   boolean exclude_inherited) {
        return delegate.lookup_name(search_name, levels_to_search, limit_type,
                exclude_inherited);
    }

    public org.omg.CORBA.ContainerPackage.Description[]
    describe_contents(DefinitionKind limit_type,
                      boolean exclude_inherited,
                      int max_returned_objs) {
        return delegate.describe_contents(limit_type, exclude_inherited,
                max_returned_objs);
    }

    public ModuleDef create_module(String id, String name, String version) {
        return delegate.create_module(id, name, version);
    }

    public ConstantDef create_constant(String id, String name, String version,
                                       IDLType type, Any value) {
        return delegate.create_constant(id, name, version, type, value);
    }

    public StructDef create_struct(String id, String name, String version,
                                   StructMember[] members) {
        return delegate.create_struct(id, name, version, members);
    }

    public UnionDef create_union(String id, String name, String version,
                                 IDLType discriminator_type,
                                 UnionMember[] members) {
        return delegate.create_union(id, name, version, discriminator_type,
                members);
    }

    public EnumDef create_enum(String id, String name, String version,
                               String[] members) {
        return delegate.create_enum(id, name, version, members);
    }

    public AliasDef create_alias(String id, String name, String version,
                                 IDLType original_type) {
        return delegate.create_alias(id, name, version, original_type);
    }

    public InterfaceDef create_interface(String id, String name, String version,
                                         InterfaceDef[] base_interfaces,
                                         boolean is_abstract) {
        return delegate.create_interface(id, name, version,
                base_interfaces, is_abstract);
    }

    public ValueDef create_value(String id, String name, String version,
                                 boolean is_custom, boolean is_abstract,
                                 ValueDef base_value, boolean is_truncatable,
                                 ValueDef[] abstract_base_values,
                                 InterfaceDef[] supported_interfaces,
                                 Initializer[] initializers) {
        return delegate.create_value(id, name, version, is_custom, is_abstract,
                base_value, is_truncatable,
                abstract_base_values, supported_interfaces,
                initializers);
    }

    public ValueBoxDef create_value_box(String id, String name, String version,
                                        IDLType original_type_def) {
        return delegate.create_value_box(id, name, version, original_type_def);
    }

    public ExceptionDef create_exception(String id, String name, String version,
                                         StructMember[] members) {
        return delegate.create_exception(id, name, version, members);
    }

    public NativeDef create_native(String id, String name, String version) {
        return delegate.create_native(id, name, version);
    }


    // ValueDefOperations implementation -------------------------

    public InterfaceDef[] supported_interfaces() {
        return supported_interfaces_ref;
    }

    public void supported_interfaces(InterfaceDef[] arg) {
        throw IIOPLogger.ROOT_LOGGER.cannotChangeRMIIIOPMapping();
    }

    public Initializer[] initializers() {
        // We do not (currently) map constructors, as that is optional according
        // to the specification.
        return new Initializer[0];
    }

    public void initializers(Initializer[] arg) {
        throw IIOPLogger.ROOT_LOGGER.cannotChangeRMIIIOPMapping();
    }

    public ValueDef base_value() {
        return base_value_ref;
    }

    public void base_value(ValueDef arg) {
        throw IIOPLogger.ROOT_LOGGER.cannotChangeRMIIIOPMapping();
    }

    public ValueDef[] abstract_base_values() {
        return abstract_base_valuetypes_ref;
    }

    public void abstract_base_values(ValueDef[] arg) {
        throw IIOPLogger.ROOT_LOGGER.cannotChangeRMIIIOPMapping();
    }

    public boolean is_abstract() {
        return is_abstract;
    }

    public void is_abstract(boolean arg) {
        throw IIOPLogger.ROOT_LOGGER.cannotChangeRMIIIOPMapping();
    }

    public boolean is_custom() {
        return is_custom;
    }

    public void is_custom(boolean arg) {
        throw IIOPLogger.ROOT_LOGGER.cannotChangeRMIIIOPMapping();
    }

    public boolean is_truncatable() {
        return false;
    }

    public void is_truncatable(boolean arg) {
        throw IIOPLogger.ROOT_LOGGER.cannotChangeRMIIIOPMapping();
    }

    public boolean is_a(String id) {
        // TODO
        return id().equals(id);
    }

    public FullValueDescription describe_value() {
        if (fullValueDescription != null)
            return fullValueDescription;

        // Has to create the FullValueDescription

        // TODO
        OperationDescription[] operations = new OperationDescription[0];
        AttributeDescription[] attributes = new AttributeDescription[0];

        String defined_in_id = "IDL:Global:1.0";
        if (defined_in instanceof org.omg.CORBA.ContainedOperations)
            defined_in_id = ((org.omg.CORBA.ContainedOperations) defined_in).id();

        fullValueDescription = new FullValueDescription(name, id,
                is_abstract, is_custom,
                defined_in_id, version,
                operations, attributes,
                getValueMembers(),
                new Initializer[0], // TODO
                supported_interfaces,
                abstract_base_valuetypes,
                false,
                baseValue,
                typeCode);

        return fullValueDescription;
    }

    public ValueMemberDef create_value_member(String id, String name,
                                              String version, IDLType type,
                                              short access) {
        throw IIOPLogger.ROOT_LOGGER.cannotChangeRMIIIOPMapping();
    }

    public AttributeDef create_attribute(String id, String name, String version,
                                         IDLType type, AttributeMode mode) {
        throw IIOPLogger.ROOT_LOGGER.cannotChangeRMIIIOPMapping();
    }

    public OperationDef create_operation(String id, String name, String version,
                                         IDLType result, OperationMode mode,
                                         ParameterDescription[] params,
                                         ExceptionDef[] exceptions,
                                         String[] contexts) {
        throw IIOPLogger.ROOT_LOGGER.cannotChangeRMIIIOPMapping();
    }


    // IDLTypeOperations implementation ------------------------------

    public TypeCode type() {
        if (typeCode == null) {
            short modifier = VM_NONE.value;
            if (is_custom)
                modifier = VM_CUSTOM.value;
            else if (is_abstract)
                modifier = VM_ABSTRACT.value;

            typeCode = getORB().create_value_tc(id, name, modifier,
                    baseValueTypeCode,
                    getValueMembersForTypeCode());
        }
        return typeCode;
    }


    // ContainedImpl implementation ----------------------------------

    public Description describe() {
        String defined_in_id = "IR";

        if (defined_in instanceof org.omg.CORBA.ContainedOperations)
            defined_in_id = ((org.omg.CORBA.ContainedOperations) defined_in).id();

        ValueDescription md = new ValueDescription(name, id, is_abstract,
                is_custom,
                defined_in_id, version,
                supported_interfaces,
                abstract_base_valuetypes,
                false,
                baseValue);

        Any any = getORB().create_any();

        ValueDescriptionHelper.insert(any, md);

        return new Description(DefinitionKind.dk_Value, any);
    }

    // Y overrides ---------------------------------------------------

    // Package protected ---------------------------------------------

    // Protected -----------------------------------------------------

    // Private -------------------------------------------------------

    /**
     * My delegate for Container functionality.
     */
    private ContainerImplDelegate delegate;

    /**
     * My CORBA reference.
     */
    private ValueDef ref = null;

    /**
     * Flag that I am abstract.
     */
    private boolean is_abstract;

    /**
     * Flag that I use custom marshaling.
     */
    private boolean is_custom;

    /**
     * IDs of my implemented interfaces.
     */
    private String[] supported_interfaces;

    /**
     * CORBA references to my implemented interfaces.
     */
    private InterfaceDef[] supported_interfaces_ref;

    /**
     * IR ID of my base value (the class I extend from).
     */
    private String baseValue;

    /**
     * TypeCode of my base value (the class I extend from).
     */
    private TypeCode baseValueTypeCode;

    /**
     * CORBA reference to my base type.
     */
    private ValueDef base_value_ref;

    /**
     * IDs of my abstract base valuetypes.
     */
    private String[] abstract_base_valuetypes;

    /**
     * CORBA references to my abstract base valuetypes.
     */
    private ValueDef[] abstract_base_valuetypes_ref;

    /**
     * My cached TypeCode.
     */
    private TypeCode typeCode;

    /**
     * My Cached ValueMember[].
     */
    private ValueMember[] valueMembers;

    /**
     * My cached FullValueDescription.
     */
    private FullValueDescription fullValueDescription;


    /**
     * Create the valueMembers array, and return it.
     */
    private ValueMember[] getValueMembers() {
        if (valueMembers != null)
            return valueMembers;

        LocalContained[] c = _contents(DefinitionKind.dk_ValueMember, false);
        valueMembers = new ValueMember[c.length];
        for (int i = 0; i < c.length; ++i) {
            ValueMemberDefImpl vmdi = (ValueMemberDefImpl) c[i];

            valueMembers[i] = new ValueMember(vmdi.name(), vmdi.id(),
                    ((LocalContained) vmdi.defined_in).id(),
                    vmdi.version(),
                    vmdi.type(), vmdi.type_def(),
                    vmdi.access());
        }

        return valueMembers;
    }

    /**
     * Create a valueMembers array for TypeCode creation only, and return it.
     */
    private ValueMember[] getValueMembersForTypeCode() {
        LocalContained[] c = _contents(DefinitionKind.dk_ValueMember, false);
        ValueMember[] vms = new ValueMember[c.length];
        for (int i = 0; i < c.length; ++i) {
            ValueMemberDefImpl vmdi = (ValueMemberDefImpl) c[i];

            vms[i] = new ValueMember(vmdi.name(),
                    null, // ignore id
                    null, // ignore defined_in
                    null, // ignore version
                    vmdi.type(),
                    null, // ignore type_def
                    vmdi.access());
        }

        return vms;
    }

    // Inner classes -------------------------------------------------
}

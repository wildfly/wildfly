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

import org.omg.CORBA.ContainerOperations;
import org.omg.CORBA.Contained;
import org.omg.CORBA.ContainerPackage.Description;
import org.omg.CORBA.IDLType;
import org.omg.CORBA.DefinitionKind;
import org.omg.CORBA.StructMember;
import org.omg.CORBA.UnionMember;
import org.omg.CORBA.InterfaceDef;
import org.omg.CORBA.ConstantDef;
import org.omg.CORBA.EnumDef;
import org.omg.CORBA.ValueDef;
import org.omg.CORBA.ValueBoxDef;
import org.omg.CORBA.Initializer;
import org.omg.CORBA.StructDef;
import org.omg.CORBA.UnionDef;
import org.omg.CORBA.ModuleDef;
import org.omg.CORBA.NativeDef;
import org.omg.CORBA.AliasDef;
import org.omg.CORBA.Any;
import org.omg.CORBA.ExceptionDef;

/**
 * Abstract base class for container IR entities.
 *
 * @author <a href="mailto:osh@sparre.dk">Ole Husgaard</a>
 * @version $Revision: 81018 $
 */
abstract class ContainerImpl
        extends IRObjectImpl
        implements ContainerOperations, LocalContainer {
    // Constants -----------------------------------------------------

    // Attributes ----------------------------------------------------

    // Static --------------------------------------------------------

    // Constructors --------------------------------------------------

    ContainerImpl(DefinitionKind def_kind, RepositoryImpl repository) {
        super(def_kind, repository);

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

    public void allDone()
            throws IRConstructionException {
        getReference();
        delegate.allDone();
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

    public Description[] describe_contents(DefinitionKind limit_type,
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


    // Y overrides ---------------------------------------------------

    // Package protected ---------------------------------------------

    // Protected -----------------------------------------------------

    // Private -------------------------------------------------------

    /**
     * My delegate for Container functionality.
     */
    ContainerImplDelegate delegate;

    // Inner classes -------------------------------------------------
}


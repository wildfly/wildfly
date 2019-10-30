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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.omg.CORBA.AliasDef;
import org.omg.CORBA.ConstantDef;
import org.omg.CORBA.Contained;
import org.omg.CORBA.ContainedHelper;
import org.omg.CORBA.Container;
import org.omg.CORBA.ContainerOperations;
import org.omg.CORBA.ContainerPackage.Description;
import org.omg.CORBA.DefinitionKind;
import org.omg.CORBA.EnumDef;
import org.omg.CORBA.ExceptionDef;
import org.omg.CORBA.IDLType;
import org.omg.CORBA.Initializer;
import org.omg.CORBA.InterfaceDef;
import org.omg.CORBA.ModuleDef;
import org.omg.CORBA.NativeDef;
import org.omg.CORBA.StructDef;
import org.omg.CORBA.StructMember;
import org.omg.CORBA.UnionDef;
import org.omg.CORBA.UnionMember;
import org.omg.CORBA.ValueBoxDef;
import org.omg.CORBA.ValueDef;
import org.wildfly.iiop.openjdk.logging.IIOPLogger;

/**
 * Delegate for Container functionality.
 *
 * @author <a href="mailto:osh@sparre.dk">Ole Husgaard</a>
 * @version $Revision: 81018 $
 */
class ContainerImplDelegate
        implements ContainerOperations {
    // Constants -----------------------------------------------------

    // Attributes ----------------------------------------------------

    // Static --------------------------------------------------------

    // Constructors --------------------------------------------------

    /**
     * Create a new delegate.
     */
    ContainerImplDelegate(LocalContainer delegateFor) {
        this.delegateFor = delegateFor;
    }

    // Public --------------------------------------------------------

    // LocalContainer delegation implementation ----------------------

    public LocalContained _lookup(String search_name) {
        if (search_name.startsWith("::"))
            return delegateFor.getRepository()._lookup(search_name.substring(2));

        int idx = search_name.indexOf("::");
        if (idx > 0) {
            String first = search_name.substring(0, idx);
            Object o = contMap.get(first);

            if (o == null || !(o instanceof LocalContainer))
                return null;
            else {
                LocalContainer next = (LocalContainer) o;
                String rest = search_name.substring(idx + 2);

                return next._lookup(rest);
            }
        } else
            return (LocalContained) contMap.get(search_name);
    }

    public LocalContained[] _contents(DefinitionKind limit_type,
                                      boolean exclude_inherited) {
        int target = limit_type.value();
        Collection found;

        if (target == DefinitionKind._dk_all)
            found = cont;
        else {
            found = new ArrayList();
            for (int i = 0; i < cont.size(); ++i) {
                LocalContained val = (LocalContained) cont.get(i);

                if (target == val.def_kind().value()) {
                    if (!exclude_inherited || val.defined_in() == delegateFor)
                        found.add(val);
                }
            }
        }

        LocalContained[] res = new LocalContained[found.size()];
        res = (LocalContained[]) found.toArray(res);

        return res;
    }

    public LocalContained[] _lookup_name(String search_name,
                                         int levels_to_search,
                                         DefinitionKind limit_type,
                                         boolean exclude_inherited) {
        if (levels_to_search == 0)
            return null;

        if (levels_to_search == -1)
            ++levels_to_search; // One more level (recursively) == all levels

        Collection found = new ArrayList();
        LocalContained[] here = _contents(limit_type, exclude_inherited);

        for (int i = 0; i < here.length; ++i)
            if (here[i].name().equals(search_name))
                found.add(here[i]);

        if (levels_to_search >= 0) {
            // More levels to search, or unlimited depth search
            for (int i = 0; i < here.length; ++i) {
                if (here[i] instanceof Container) { // search here
                    LocalContainer container = (LocalContainer) here[i];

                    LocalContained[] c;
                    c = container._lookup_name(search_name, levels_to_search - 1,
                            limit_type, exclude_inherited);
                    if (c != null)
                        for (int j = 0; j < c.length; ++j)
                            found.add(c[j]);

                }
            }

        }

        LocalContained[] res = new LocalContained[found.size()];
        res = (LocalContained[]) found.toArray(res);

        return res;
    }

    public void shutdown() {
        for (int i = 0; i < cont.size(); ++i)
            ((LocalContained) cont.get(i)).shutdown();
    }

    // ContainerOperations implementation ----------------------------

    public Contained lookup(String search_name) {
        LocalContained c = _lookup(search_name);

        if (c == null)
            return null;
        else
            return ContainedHelper.narrow(c.getReference());
    }

    public Contained[] contents(DefinitionKind limit_type,
                                boolean exclude_inherited) {
        LocalContained[] c = _contents(limit_type, exclude_inherited);
        Contained[] res = new Contained[c.length];

        for (int i = 0; i < c.length; ++i)
            res[i] = ContainedHelper.narrow(c[i].getReference());

        return res;
    }

    public Contained[] lookup_name(String search_name, int levels_to_search,
                                   DefinitionKind limit_type,
                                   boolean exclude_inherited) {
        LocalContained[] c = _lookup_name(search_name, levels_to_search,
                limit_type, exclude_inherited);
        Contained[] res = new Contained[c.length];

        for (int i = 0; i < c.length; ++i)
            res[i] = ContainedHelper.narrow(c[i].getReference());

        return res;
    }

    public Description[] describe_contents(DefinitionKind limit_type,
                                           boolean exclude_inherited,
                                           int max_returned_objs) {
        Contained[] c = contents(limit_type, exclude_inherited);
        int returnSize;

        if (max_returned_objs != -1 && c.length > max_returned_objs)
            returnSize = max_returned_objs;
        else
            returnSize = c.length;

        Description[] ret = new Description[returnSize];

        for (int i = 0; i < returnSize; ++i) {
            org.omg.CORBA.ContainedPackage.Description d = c[i].describe();

            ret[i] = new Description(c[i], d.kind, d.value);
        }

        return ret;
    }

    public ModuleDef create_module(String id, String name, String version) {
        throw IIOPLogger.ROOT_LOGGER.cannotChangeRMIIIOPMapping();
    }

    public ConstantDef create_constant(String id, String name, String version,
                                       IDLType type, org.omg.CORBA.Any value) {
        throw IIOPLogger.ROOT_LOGGER.cannotChangeRMIIIOPMapping();
    }

    public StructDef create_struct(String id, String name, String version,
                                   StructMember[] members) {
        throw IIOPLogger.ROOT_LOGGER.cannotChangeRMIIIOPMapping();
    }

    public UnionDef create_union(String id, String name, String version,
                                 IDLType discriminator_type,
                                 UnionMember[] members) {
        throw IIOPLogger.ROOT_LOGGER.cannotChangeRMIIIOPMapping();
    }

    public EnumDef create_enum(String id, String name, String version,
                               String[] members) {
        throw IIOPLogger.ROOT_LOGGER.cannotChangeRMIIIOPMapping();
    }

    public AliasDef create_alias(String id, String name, String version,
                                 IDLType original_type) {
        throw IIOPLogger.ROOT_LOGGER.cannotChangeRMIIIOPMapping();
    }

    public InterfaceDef create_interface(String id, String name, String version,
                                         InterfaceDef[] base_interfaces,
                                         boolean is_abstract) {
        throw IIOPLogger.ROOT_LOGGER.cannotChangeRMIIIOPMapping();
    }

    public ValueDef create_value(String id, String name, String version,
                                 boolean is_custom, boolean is_abstract,
                                 ValueDef base_value, boolean is_truncatable,
                                 ValueDef[] abstract_base_values,
                                 InterfaceDef[] supported_interfaces,
                                 Initializer[] initializers) {
        throw IIOPLogger.ROOT_LOGGER.cannotChangeRMIIIOPMapping();
    }

    public ValueBoxDef create_value_box(String id, String name, String version,
                                        IDLType original_type_def) {
        throw IIOPLogger.ROOT_LOGGER.cannotChangeRMIIIOPMapping();
    }

    public ExceptionDef create_exception(String id, String name, String version,
                                         StructMember[] members) {
        throw IIOPLogger.ROOT_LOGGER.cannotChangeRMIIIOPMapping();
    }

    public NativeDef create_native(String id, String name, String version) {
        throw IIOPLogger.ROOT_LOGGER.cannotChangeRMIIIOPMapping();
    }


    // Dummy IRObjectOperations implementation -----------------------

    public DefinitionKind def_kind() {
        throw new UnsupportedOperationException();
    }

    public void destroy() {
        throw new UnsupportedOperationException();
    }


    // Package protected ---------------------------------------------

    void add(String name, LocalContained contained) throws IRConstructionException {
        if (contained.getRepository() != delegateFor.getRepository())
            throw IIOPLogger.ROOT_LOGGER.wrongInterfaceRepository();
        if (contMap.get(name) != null)
            throw IIOPLogger.ROOT_LOGGER.duplicateRepositoryName();
        cont.add(contained);
        contMap.put(name, contained);
    }

    /**
     * Finalize build process, and export.
     */
    void allDone() throws IRConstructionException {
        for (int i = 0; i < cont.size(); ++i) {
            LocalContained item = (LocalContained) cont.get(i);
            item.allDone();
        }
    }

    // Protected -----------------------------------------------------

    // Private -------------------------------------------------------

    /**
     * The contents of the Container.
     */
    private ArrayList cont = new ArrayList();

    /**
     * Maps names to the contents of the Container.
     */
    private Map contMap = new HashMap();

    /**
     * The Container I am a delegate for.
     */
    private LocalContainer delegateFor;


    // Inner classes -------------------------------------------------
}

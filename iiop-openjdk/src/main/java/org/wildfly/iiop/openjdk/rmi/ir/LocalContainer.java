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
import org.omg.CORBA.DefinitionKind;

/**
 * Interface of local containers.
 * Those who delegate the container implementation to the
 * ContainerImplDelegate should implement this interface.
 *
 * @author <a href="mailto:osh@sparre.dk">Ole Husgaard</a>
 * @version $Revision: 81018 $
 */
interface LocalContainer  extends ContainerOperations, LocalIRObject {
    /**
     * Same as org.omg.CORBA.Contained.lookup(),
     * but returns local objects instead.
     */
    LocalContained _lookup(String search_name);

    /**
     * Same as org.omg.CORBA.Contained.contents(),
     * but returns local objects instead.
     */
    LocalContained[] _contents(DefinitionKind limit_type,
                                      boolean exclude_inherited);

    /**
     * Same as org.omg.CORBA.Contained.lookup_name(),
     * but returns local objects instead.
     */
    LocalContained[] _lookup_name(String search_name,
                                         int levels_to_search,
                                         DefinitionKind limit_type,
                                         boolean exclude_inherited);

    /**
     * Add an entry to the delegating container.
     */
    void add(String name, LocalContained contained)
            throws IRConstructionException;
}


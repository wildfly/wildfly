/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.jboss.as.naming.subsystem;

import java.util.HashMap;
import java.util.Map;

import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
public enum BindingType {

    SIMPLE(NamingSubsystemModel.SIMPLE),
    OBJECT_FACTORY(NamingSubsystemModel.OBJECT_FACTORY),
    LOOKUP(NamingSubsystemModel.LOOKUP),
    EXTERNAL_CONTEXT(NamingSubsystemModel.EXTERNAL_CONTEXT),
    ;


    private static final Map<String, BindingType> MAP;

    static {
        final Map<String, BindingType> map = new HashMap<String, BindingType>();
        for (BindingType directoryGrouping : values()) {
            map.put(directoryGrouping.localName, directoryGrouping);
        }
        MAP = map;
    }

    public static BindingType forName(String localName) {
        if (localName == null) return null;
        final BindingType directoryGrouping = MAP.get(localName.toLowerCase());
        return directoryGrouping == null ? BindingType.valueOf(localName.toUpperCase()) : directoryGrouping;
    }

    private final String localName;

    BindingType(final String localName) {
        this.localName = localName;
    }

    @Override
    public String toString() {
        return localName;
    }

    /**
     * Converts the value of the directory grouping to a model node.
     *
     * @return a new model node for the value.
     */
    public ModelNode toModelNode() {
        return new ModelNode().set(toString());
    }


}

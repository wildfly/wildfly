/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.naming.subsystem;

import java.util.HashMap;
import java.util.Locale;
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
        final BindingType directoryGrouping = MAP.get(localName.toLowerCase(Locale.ENGLISH));
        return directoryGrouping == null ? BindingType.valueOf(localName.toUpperCase(Locale.ENGLISH)) : directoryGrouping;
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

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.keycloak.subsystem.adapter.extension;

import org.jboss.as.controller.ModelOnlyAddStepHandler;
import org.jboss.as.controller.ModelOnlyResourceDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines attributes and operations for the Realm
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2013 Red Hat Inc.
 */
public class RealmDefinition extends ModelOnlyResourceDefinition {

    public static final String TAG_NAME = "realm";


    protected static final List<SimpleAttributeDefinition> REALM_ONLY_ATTRIBUTES = new ArrayList<SimpleAttributeDefinition>();
    static {
    }

    protected static final List<SimpleAttributeDefinition> ALL_ATTRIBUTES = new ArrayList<SimpleAttributeDefinition>();
    static {
        ALL_ATTRIBUTES.addAll(REALM_ONLY_ATTRIBUTES);
        ALL_ATTRIBUTES.addAll(SharedAttributeDefinitons.ATTRIBUTES);
    }
    protected static final SimpleAttributeDefinition[] ALL_ATTRIBUTES_ARRAY = ALL_ATTRIBUTES.toArray(new SimpleAttributeDefinition[ALL_ATTRIBUTES.size()]);

    private static final Map<String, SimpleAttributeDefinition> DEFINITION_LOOKUP = new HashMap<String, SimpleAttributeDefinition>();
    static {
        for (SimpleAttributeDefinition def : ALL_ATTRIBUTES) {
            DEFINITION_LOOKUP.put(def.getXmlName(), def);
        }
    }

    public RealmDefinition() {
        super(PathElement.pathElement("realm"),
                KeycloakExtension.getResourceDescriptionResolver("realm"),
                new ModelOnlyAddStepHandler(ALL_ATTRIBUTES_ARRAY),
                ALL_ATTRIBUTES_ARRAY
                );
    }

    public static SimpleAttributeDefinition lookup(String name) {
        return DEFINITION_LOOKUP.get(name);
    }
}

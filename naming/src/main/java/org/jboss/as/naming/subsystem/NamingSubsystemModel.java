/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.naming.subsystem;

import org.jboss.as.controller.PathElement;

/**
 * User: jpai
 */
public interface NamingSubsystemModel {

    String BINDING = "binding";
    String BINDING_TYPE = "binding-type";

    String CACHE = "cache";
    String CLASS = "class";

    String EXTERNAL_CONTEXT = "external-context";

    String LOOKUP = "lookup";

    String OBJECT_FACTORY = "object-factory";
    String ENVIRONMENT = "environment";

    String MODULE = "module";

    String REBIND = "rebind";
    String REMOTE_NAMING = "remote-naming";

    String SIMPLE = "simple";
    String SERVICE = "service";

    String TYPE = "type";

    String VALUE = "value";

    PathElement BINDING_PATH = PathElement.pathElement(BINDING);
    PathElement REMOTE_NAMING_PATH = PathElement.pathElement(SERVICE, REMOTE_NAMING);


}

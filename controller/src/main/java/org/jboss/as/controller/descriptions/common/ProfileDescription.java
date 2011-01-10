/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.controller.descriptions.common;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_LENGTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NILLABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Model description for profile elements.
 *
 * @author Brian Stansberry
 */
public class ProfileDescription {

    private static final ModelNode PROFILE_NODE = new ModelNode();
    private static final ModelNode PROFILE_WITH_INCLUDES_NODE;

    private static final ModelNode PROFILE_ADD_OPERATION = new ModelNode();
    private static final ModelNode PROFILE_REMOVE_OPERATION = new ModelNode();

    private static final ModelNode PROFILE_INCLUDE_ADD_OPERATION = new ModelNode();
    private static final ModelNode PROFILE_INCLUDE_REMOVE_OPERATION = new ModelNode();

    static {
        //FIXME Load all descriptions from a resource file

        PROFILE_NODE.get(DESCRIPTION).set("A named set of subsystem configurations.");
        PROFILE_NODE.get(HEAD_COMMENT_ALLOWED).set(true);
        PROFILE_NODE.get(TAIL_COMMENT_ALLOWED).set(true);
        PROFILE_NODE.get(ATTRIBUTES, NAME, TYPE).set(ModelType.STRING);
        PROFILE_NODE.get(ATTRIBUTES, NAME, DESCRIPTION).set("The name of the profile");
        PROFILE_NODE.get(ATTRIBUTES, NAME, REQUIRED).set(true);
        PROFILE_NODE.get(ATTRIBUTES, NAME, MIN_LENGTH).set(1);

        PROFILE_ADD_OPERATION.get(OPERATION_NAME).set("add-profile");
        PROFILE_ADD_OPERATION.get(DESCRIPTION).set("Add a new 'profile' child");
        PROFILE_ADD_OPERATION.get(REQUEST_PROPERTIES).get(NAME).get(TYPE).set(ModelType.STRING);
        PROFILE_ADD_OPERATION.get(REQUEST_PROPERTIES).get(NAME).get(DESCRIPTION).set("The value of the profile's 'name' attribute");
        PROFILE_ADD_OPERATION.get(REQUEST_PROPERTIES).get(NAME).get(REQUIRED).set(true);
        PROFILE_ADD_OPERATION.get(REQUEST_PROPERTIES).get(NAME).get(MIN_LENGTH).set(1);
        PROFILE_ADD_OPERATION.get(REQUEST_PROPERTIES).get(NAME).get(NILLABLE).set(false);
        PROFILE_ADD_OPERATION.get(REPLY_PROPERTIES).setEmptyObject();

        PROFILE_REMOVE_OPERATION.get(OPERATION_NAME).set("remove-profile");
        PROFILE_REMOVE_OPERATION.get(DESCRIPTION).set("Remove a 'profile' child");
        PROFILE_REMOVE_OPERATION.get(REQUEST_PROPERTIES).get(NAME).get(TYPE).set(ModelType.STRING);
        PROFILE_REMOVE_OPERATION.get(REQUEST_PROPERTIES).get(NAME).get(DESCRIPTION).set("The value of the profile's 'name' attribute");
        PROFILE_REMOVE_OPERATION.get(REQUEST_PROPERTIES).get(NAME).get(REQUIRED).set(true);
        PROFILE_REMOVE_OPERATION.get(REQUEST_PROPERTIES).get(NAME).get(MIN_LENGTH).set(1);
        PROFILE_REMOVE_OPERATION.get(REQUEST_PROPERTIES).get(NAME).get(NILLABLE).set(false);
        PROFILE_REMOVE_OPERATION.get(REPLY_PROPERTIES).setEmptyObject();

        PROFILE_WITH_INCLUDES_NODE = PROFILE_NODE.clone();
        PROFILE_WITH_INCLUDES_NODE.get(ATTRIBUTES, INCLUDE, TYPE).set(ModelType.LIST);
        PROFILE_WITH_INCLUDES_NODE.get(ATTRIBUTES, INCLUDE, DESCRIPTION).set("The name of other profiles to include in this profile");
        PROFILE_WITH_INCLUDES_NODE.get(ATTRIBUTES, INCLUDE, REQUIRED).set(true);
        PROFILE_WITH_INCLUDES_NODE.get(ATTRIBUTES, INCLUDE, VALUE_TYPE).set(ModelType.STRING);

        PROFILE_INCLUDE_ADD_OPERATION.get(OPERATION_NAME).set("add-profile-include");
        PROFILE_INCLUDE_ADD_OPERATION.get(DESCRIPTION).set("Add a profile to the list of included profiles");
        PROFILE_INCLUDE_ADD_OPERATION.get(REQUEST_PROPERTIES).get(NAME).get(TYPE).set(ModelType.STRING);
        PROFILE_INCLUDE_ADD_OPERATION.get(REQUEST_PROPERTIES).get(NAME).get(DESCRIPTION).set("The name of the included profile");
        PROFILE_INCLUDE_ADD_OPERATION.get(REQUEST_PROPERTIES).get(NAME).get(REQUIRED).set(true);
        PROFILE_INCLUDE_ADD_OPERATION.get(REQUEST_PROPERTIES).get(NAME).get(MIN_LENGTH).set(1);
        PROFILE_INCLUDE_ADD_OPERATION.get(REQUEST_PROPERTIES).get(NAME).get(NILLABLE).set(false);
        PROFILE_INCLUDE_ADD_OPERATION.get(REPLY_PROPERTIES).setEmptyObject();

        PROFILE_INCLUDE_REMOVE_OPERATION.get(OPERATION_NAME).set("remove-profile-include");
        PROFILE_INCLUDE_REMOVE_OPERATION.get(DESCRIPTION).set("Remove a profile from the list of included profiles");
        PROFILE_INCLUDE_REMOVE_OPERATION.get(REQUEST_PROPERTIES).get(NAME).get(TYPE).set(ModelType.STRING);
        PROFILE_INCLUDE_REMOVE_OPERATION.get(REQUEST_PROPERTIES).get(NAME).get(DESCRIPTION).set("The name of the included profile");
        PROFILE_INCLUDE_REMOVE_OPERATION.get(REQUEST_PROPERTIES).get(NAME).get(REQUIRED).set(true);
        PROFILE_INCLUDE_REMOVE_OPERATION.get(REQUEST_PROPERTIES).get(NAME).get(MIN_LENGTH).set(1);
        PROFILE_INCLUDE_REMOVE_OPERATION.get(REQUEST_PROPERTIES).get(NAME).get(NILLABLE).set(false);
        PROFILE_INCLUDE_REMOVE_OPERATION.get(REPLY_PROPERTIES).setEmptyObject();
    }

    public static ModelNode getProfileDescription() {
        return PROFILE_NODE.clone();
    }

    public static ModelNode getProfileWithIncludesDescription() {
        return PROFILE_WITH_INCLUDES_NODE.clone();
    }

    public static ModelNode getProfileAddOperation() {
        return PROFILE_ADD_OPERATION.clone();
    }

    public static ModelNode getProfileRemoveOperation() {
        return PROFILE_REMOVE_OPERATION.clone();
    }

    public static ModelNode getProfileIncludeAddOperation() {
        return PROFILE_INCLUDE_ADD_OPERATION.clone();
    }

    public static ModelNode getProfileIncludeRemoveOperation() {
        return PROFILE_INCLUDE_REMOVE_OPERATION.clone();
    }
}

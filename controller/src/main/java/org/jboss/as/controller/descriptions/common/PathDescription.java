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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_LENGTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NILLABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Model description for path elements.
 *
 * @author Brian Stansberry
 */
public class PathDescription {

    private static final ModelNode PATH_NODE = new ModelNode();
    private static final ModelNode SPECIFIED_PATH_NODE;

    private static final ModelNode SET_NAMED_PATH_OPERATION = new ModelNode();
    private static final ModelNode SET_RELATIVE_TO_OPERATION = new ModelNode();
    private static final ModelNode SET_SPECIFIED_PATH_OPERATION;

    private static final ModelNode NAMED_PATH_ADD_OPERATION = new ModelNode();
    private static final ModelNode PATH_REMOVE_OPERATION = new ModelNode();

    private static final ModelNode SPECIFIED_PATH_ADD_OPERATION;

    private static final String RELATIVE_TO = "relative-to";

    static {
        //FIXME Load all descriptions from a resource file

        PATH_NODE.get(DESCRIPTION).set("A named filesystem path, but without a requirement to specify " +
                "the actual path. If no actual path is specified, acts as a " +
                "as a placeholder in the model (e.g. at the domain level) " +
                "until a fully specified path definition is applied at a " +
                "lower level (e.g. at the host level, where available addresses " +
                "are known.)");
        PATH_NODE.get(ATTRIBUTES, NAME).get(TYPE).set(ModelType.STRING);
        PATH_NODE.get(ATTRIBUTES, NAME).get(DESCRIPTION).set(
                "The name of the path. Cannot be one of the standard fixed paths " +
                "provided by the system: "+
                "\n" +
                "    jboss.home - the root directory of the JBoss AS distribution \n"+
                "    user.home - user's home directory \n"+
                "    user.dir - user's current working directory \n"+
                "    java.home - java installation directory\n"+
                "    jboss.server.base.dir - root directory for an individual server instance \n"+
                "\n" +
                "Note that the system provides other standard paths that can be "+
                "overridden by declaring them in the configuration file. See "+
                "the 'relative-to' attribute documentation for a complete "+
                "list of standard paths.");
        PATH_NODE.get(ATTRIBUTES, NAME).get(REQUIRED).set(true);
        PATH_NODE.get(ATTRIBUTES, PATH).get(TYPE).set(ModelType.STRING);
        PATH_NODE.get(ATTRIBUTES, PATH).get(DESCRIPTION).set(
                "The actual filesystem path. Treated as an absolute path, unless the " +
                "'relative-to' attribute is specified, in which case the value " +
                "is treated as relative to that path. " +
                "\n" +
                "If treated as an absolute path, the actual runtime pathname specified " +
                "by the value of this attribute will be determined as follows: " +
                "\n" +
                "If this value is already absolute, then the value is directly " +
                "used.  Otherwise the runtime pathname is resolved in a " +
                "system-dependent way.  On UNIX systems, a relative pathname is  " +
                "made absolute by resolving it against the current user directory. " +
                "On Microsoft Windows systems, a relative pathname is made absolute " +
                "by resolving it against the current directory of the drive named by the " +
                "pathname, if any; if not, it is resolved against the current user " +
                "directory.");
        PATH_NODE.get(ATTRIBUTES, PATH).get(REQUIRED).set(false);
        PATH_NODE.get(ATTRIBUTES, PATH).get(MIN_LENGTH).set(1);
        PATH_NODE.get(ATTRIBUTES, RELATIVE_TO).get(TYPE).set(ModelType.STRING);
        PATH_NODE.get(ATTRIBUTES, RELATIVE_TO).get(DESCRIPTION).set(
                "The name of another previously named path, or of one of the " +
                "standard paths provided by the system. If 'relative-to' is " +
                "provided, the value of the 'path' attribute is treated as " +
                "relative to the path specified by this attribute. The standard " +
                "paths provided by the system include:\n" +
                "\n" +
                "jboss.home - the root directory of the JBoss AS distribution\n"+
                "user.home - user's home directory\n"+
                "user.dir - user's current working directory\n"+
                "java.home - java installation directory\n"+
                "jboss.server.base.dir - root directory for an individual server instance\n"+
                "jboss.server.data.dir - directory the server will use for persistent data file storage\n"+
                "jboss.server.log.dir - directory the server will use for log file storage\n"+
                "jboss.server.tmp.dir - directory the server will use for temporary file storage\n"+
                "jboss.domain.servers.dir - directory under which a host controller will create the working area for individual server instances");
        PATH_NODE.get(ATTRIBUTES, RELATIVE_TO).get(REQUIRED).set(false);

        SPECIFIED_PATH_NODE = PATH_NODE.clone();
        SPECIFIED_PATH_NODE.get(ATTRIBUTES, PATH).get(REQUIRED).set(true);

        SET_NAMED_PATH_OPERATION.get(OPERATION_NAME).set("setPath");
        SET_NAMED_PATH_OPERATION.get(DESCRIPTION).set("Set the value of the 'path' attribute");
        SET_NAMED_PATH_OPERATION.get(REQUEST_PROPERTIES).get(PATH).get(TYPE).set(ModelType.STRING);
        SET_NAMED_PATH_OPERATION.get(REQUEST_PROPERTIES).get(PATH).get(DESCRIPTION).set("The new value of the 'path' attribute");
        SET_NAMED_PATH_OPERATION.get(REQUEST_PROPERTIES).get(PATH).get(REQUIRED).set(true);
        SET_NAMED_PATH_OPERATION.get(REQUEST_PROPERTIES).get(PATH).get(MIN_LENGTH).set(1);
        SET_NAMED_PATH_OPERATION.get(REQUEST_PROPERTIES).get(PATH).get(NILLABLE).set(true);
        SET_NAMED_PATH_OPERATION.get(REPLY_PROPERTIES).setEmptyObject();

        SET_RELATIVE_TO_OPERATION.get(OPERATION_NAME).set("setRelativeTo");
        SET_RELATIVE_TO_OPERATION.get(DESCRIPTION).set("Set the value of the 'relative-to' attribute");
        SET_RELATIVE_TO_OPERATION.get(REQUEST_PROPERTIES).get(RELATIVE_TO).get(TYPE).set(ModelType.STRING);
        SET_RELATIVE_TO_OPERATION.get(REQUEST_PROPERTIES).get(RELATIVE_TO).get(DESCRIPTION).set("The new value of the 'relative-to' attribute");
        SET_RELATIVE_TO_OPERATION.get(REQUEST_PROPERTIES).get(RELATIVE_TO).get(REQUIRED).set(true);
        SET_RELATIVE_TO_OPERATION.get(REQUEST_PROPERTIES).get(RELATIVE_TO).get(NILLABLE).set(true);
        SET_RELATIVE_TO_OPERATION.get(REPLY_PROPERTIES).setEmptyObject();

        SET_SPECIFIED_PATH_OPERATION = SET_NAMED_PATH_OPERATION.clone();
        SET_SPECIFIED_PATH_OPERATION.get(REQUEST_PROPERTIES).get(PATH).get(NILLABLE).set(false);

        NAMED_PATH_ADD_OPERATION.get(OPERATION_NAME).set("add-path");
        NAMED_PATH_ADD_OPERATION.get(DESCRIPTION).set("Add a new 'path' child");
        NAMED_PATH_ADD_OPERATION.get(REQUEST_PROPERTIES).get(NAME).get(TYPE).set(ModelType.STRING);
        NAMED_PATH_ADD_OPERATION.get(REQUEST_PROPERTIES).get(NAME).get(DESCRIPTION).set("The value of the path's 'name' attribute");
        NAMED_PATH_ADD_OPERATION.get(REQUEST_PROPERTIES).get(NAME).get(REQUIRED).set(true);
        NAMED_PATH_ADD_OPERATION.get(REQUEST_PROPERTIES).get(NAME).get(MIN_LENGTH).set(1);
        NAMED_PATH_ADD_OPERATION.get(REQUEST_PROPERTIES).get(NAME).get(NILLABLE).set(false);
        NAMED_PATH_ADD_OPERATION.get(REQUEST_PROPERTIES).get(PATH).get(TYPE).set(ModelType.STRING);
        NAMED_PATH_ADD_OPERATION.get(REQUEST_PROPERTIES).get(PATH).get(DESCRIPTION).set("The value of the path's 'path' attribute");
        NAMED_PATH_ADD_OPERATION.get(REQUEST_PROPERTIES).get(PATH).get(REQUIRED).set(false);
        NAMED_PATH_ADD_OPERATION.get(REQUEST_PROPERTIES).get(PATH).get(MIN_LENGTH).set(1);
        NAMED_PATH_ADD_OPERATION.get(REQUEST_PROPERTIES).get(PATH).get(NILLABLE).set(true);
        NAMED_PATH_ADD_OPERATION.get(REQUEST_PROPERTIES).get(RELATIVE_TO).get(TYPE).set(ModelType.STRING);
        NAMED_PATH_ADD_OPERATION.get(REQUEST_PROPERTIES).get(RELATIVE_TO).get(DESCRIPTION).set("The value of the path's 'relative-to' attribute");
        NAMED_PATH_ADD_OPERATION.get(REQUEST_PROPERTIES).get(RELATIVE_TO).get(REQUIRED).set(false);
        NAMED_PATH_ADD_OPERATION.get(REQUEST_PROPERTIES).get(RELATIVE_TO).get(NILLABLE).set(true);
        NAMED_PATH_ADD_OPERATION.get(REPLY_PROPERTIES).setEmptyObject();

        SPECIFIED_PATH_ADD_OPERATION = NAMED_PATH_ADD_OPERATION.clone();
        SPECIFIED_PATH_ADD_OPERATION.get(REQUEST_PROPERTIES, PATH, REQUIRED).set(true);
        SPECIFIED_PATH_ADD_OPERATION.get(REQUEST_PROPERTIES, PATH, NILLABLE).set(false);

        PATH_REMOVE_OPERATION.get(OPERATION_NAME).set("remove-path");
        PATH_REMOVE_OPERATION.get(DESCRIPTION).set("Remove a 'path' child");
        PATH_REMOVE_OPERATION.get(REQUEST_PROPERTIES).get(NAME).get(TYPE).set(ModelType.STRING);
        PATH_REMOVE_OPERATION.get(REQUEST_PROPERTIES).get(NAME).get(DESCRIPTION).set("The value of the path's 'name' attribute");
        PATH_REMOVE_OPERATION.get(REQUEST_PROPERTIES).get(NAME).get(REQUIRED).set(true);
        PATH_REMOVE_OPERATION.get(REQUEST_PROPERTIES).get(NAME).get(MIN_LENGTH).set(1);
        PATH_REMOVE_OPERATION.get(REQUEST_PROPERTIES).get(NAME).get(NILLABLE).set(false);
        PATH_REMOVE_OPERATION.get(REPLY_PROPERTIES).setEmptyObject();
    }

    public static ModelNode getNamedPathDescription() {
        return PATH_NODE.clone();
    }

    public static ModelNode getSpecifiedPathDescription() {
        return SPECIFIED_PATH_NODE.clone();
    }

    public static ModelNode getSetNamedPathOperation() {
        return SET_NAMED_PATH_OPERATION.clone();
    }

    public static ModelNode getSetSpecifiedPathOperation() {
        return SET_SPECIFIED_PATH_OPERATION.clone();
    }

    public static ModelNode getNamedPathAddOperation() {
        return NAMED_PATH_ADD_OPERATION.clone();
    }

    public static ModelNode getSpecifiedPathAddOperation() {
        return SPECIFIED_PATH_ADD_OPERATION.clone();
    }

    public static ModelNode getPathRemoveOperation() {
        return PATH_REMOVE_OPERATION.clone();
    }
}

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
 */package org.jboss.as.controller;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * TODO add class javadoc for DomainMetadata
 *
 * @author Brian Stansberry
 *
 */
public class CommonMetadata {

    public static final ModelNode NAMESPACE_PREFIX_ATTRIBUTE = new ModelNode();
    public static final ModelNode SCHEMA_LOCATION_ATTRIBUTE = new ModelNode();

    public static final ModelNode PATH_NODE = new ModelNode();
    public static final ModelNode SPECIFIED_PATH_NODE;

    public static final ModelNode PATH_ADD_OPERATION = new ModelNode();
    public static final ModelNode PATH_REMOVE_OPERATION = new ModelNode();

    static {
        NAMESPACE_PREFIX_ATTRIBUTE.get("name").set("namespaces");
        NAMESPACE_PREFIX_ATTRIBUTE.get("type").set(ModelType.OBJECT);
        NAMESPACE_PREFIX_ATTRIBUTE.get("value-type").set(ModelType.STRING);
        NAMESPACE_PREFIX_ATTRIBUTE.get("description").set("Map of namespaces used in the configuration XML document, where keys are namespace prefixes and values are schema URIs.");
        NAMESPACE_PREFIX_ATTRIBUTE.get("required").set(false);
        SCHEMA_LOCATION_ATTRIBUTE.get("name").set("schema-locations");
        SCHEMA_LOCATION_ATTRIBUTE.get("type").set(ModelType.OBJECT);
        SCHEMA_LOCATION_ATTRIBUTE.get("value-type").set(ModelType.STRING);
        SCHEMA_LOCATION_ATTRIBUTE.get("description").set("Map of locations of XML schemas used in the configuration XML document, where keys are schema URIs and values are locations where the schema can be found.");
        SCHEMA_LOCATION_ATTRIBUTE.get("required").set(false);

        PATH_NODE.get("description").set("A named filesystem path, but without a requirement to specify " +
                "the actual path. If no actual path is specified, acts as a " +
                "as a placeholder in the model (e.g. at the domain level) " +
                "until a fully specified path definition is applied at a " +
                "lower level (e.g. at the host level, where available addresses " +
                "are known.)");
        PATH_NODE.get("attributes").get(0).get("name").set("name");
        PATH_NODE.get("attributes").get(0).get("type").set(ModelType.STRING);
        PATH_NODE.get("attributes").get(0).get("description").set(
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
        PATH_NODE.get("attributes").get(0).get("required").set(true);
        PATH_NODE.get("attributes").get(1).get("name").set("path");
        PATH_NODE.get("attributes").get(1).get("type").set(ModelType.STRING);
        PATH_NODE.get("attributes").get(1).get("description").set(
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
        PATH_NODE.get("attributes").get(1).get("required").set(false);
        PATH_NODE.get("attributes").get(1).get("min-length").set(1);
        PATH_NODE.get("attributes").get(2).get("name").set("relative-to");
        PATH_NODE.get("attributes").get(2).get("type").set(ModelType.STRING);
        PATH_NODE.get("attributes").get(2).get("description").set(
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
        PATH_NODE.get("attributes").get(2).get("required").set(false);
        PATH_NODE.get("operations").get(0).get("operation-name").set("setPath");
        PATH_NODE.get("operations").get(0).get("description").set("Set the value of the 'path' attribute");
        PATH_NODE.get("operations").get(0).get("request-properties").get("path").get("type").set(ModelType.STRING);
        PATH_NODE.get("operations").get(0).get("request-properties").get("path").get("description").set("The new value of the 'path' attribute");
        PATH_NODE.get("operations").get(0).get("request-properties").get("path").get("required").set(true);
        PATH_NODE.get("operations").get(0).get("request-properties").get("path").get("min-length").set(1);
        PATH_NODE.get("operations").get(0).get("request-properties").get("path").get("nillable").set(true);
        PATH_NODE.get("operations").get(0).get("reply-properties").setEmptyObject();
        PATH_NODE.get("operations").get(1).get("operation-name").set("setRelativeTo");
        PATH_NODE.get("operations").get(1).get("description").set("Set the value of the 'relative-to' attribute");
        PATH_NODE.get("operations").get(1).get("request-properties").get("relative-to").get("type").set(ModelType.STRING);
        PATH_NODE.get("operations").get(1).get("request-properties").get("relative-to").get("description").set("The new value of the 'relative-to' attribute");
        PATH_NODE.get("operations").get(1).get("request-properties").get("relative-to").get("required").set(true);
        PATH_NODE.get("operations").get(1).get("request-properties").get("relative-to").get("nillable").set(true);
        PATH_NODE.get("operations").get(1).get("reply-properties").setEmptyObject();

        SPECIFIED_PATH_NODE = PATH_NODE.clone();
        SPECIFIED_PATH_NODE.get("attributes").get(1).get("required").set(true);
        SPECIFIED_PATH_NODE.get("operations").get(0).get("request-properties").get("path").get("nillable").set(false);

        PATH_ADD_OPERATION.get("operation-name").set("add-path");
        PATH_ADD_OPERATION.get("description").set("Add a new 'path' child");
        PATH_ADD_OPERATION.get("request-properties").get("name").get("type").set(ModelType.STRING);
        PATH_ADD_OPERATION.get("request-properties").get("name").get("description").set("The value of the path's 'name' attribute");
        PATH_ADD_OPERATION.get("request-properties").get("name").get("required").set(true);
        PATH_ADD_OPERATION.get("request-properties").get("name").get("min-length").set(1);
        PATH_ADD_OPERATION.get("request-properties").get("name").get("nillable").set(false);
        PATH_ADD_OPERATION.get("request-properties").get("path").get("type").set(ModelType.STRING);
        PATH_ADD_OPERATION.get("request-properties").get("path").get("description").set("The value of the path's 'path' attribute");
        PATH_ADD_OPERATION.get("request-properties").get("path").get("required").set(false);
        PATH_ADD_OPERATION.get("request-properties").get("path").get("min-length").set(1);
        PATH_ADD_OPERATION.get("request-properties").get("path").get("nillable").set(true);
        PATH_ADD_OPERATION.get("request-properties").get("relative-to").get("type").set(ModelType.STRING);
        PATH_ADD_OPERATION.get("request-properties").get("relative-to").get("description").set("The value of the path's 'relative-to' attribute");
        PATH_ADD_OPERATION.get("request-properties").get("relative-to").get("required").set(false);
        PATH_ADD_OPERATION.get("request-properties").get("relative-to").get("nillable").set(true);
        PATH_ADD_OPERATION.get("reply-properties").setEmptyObject();

        PATH_REMOVE_OPERATION.get("operation-name").set("remove-path");
        PATH_REMOVE_OPERATION.get("description").set("Remove a 'path' child");
        PATH_REMOVE_OPERATION.get("request-properties").get("name").get("type").set(ModelType.STRING);
        PATH_REMOVE_OPERATION.get("request-properties").get("name").get("description").set("The value of the path's 'name' attribute");
        PATH_REMOVE_OPERATION.get("request-properties").get("name").get("required").set(true);
        PATH_REMOVE_OPERATION.get("request-properties").get("name").get("min-length").set(1);
        PATH_REMOVE_OPERATION.get("request-properties").get("name").get("nillable").set(false);
        PATH_REMOVE_OPERATION.get("reply-properties").setEmptyObject();
    }

}

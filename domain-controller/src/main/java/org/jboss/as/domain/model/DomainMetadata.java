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
 */package org.jboss.as.domain.model;

import org.jboss.as.controller.CommonMetadata;
import org.jboss.dmr.ModelNode;

/**
 * TODO add class javadoc for DomainMetadata
 *
 * @author Brian Stansberry
 *
 */
public class DomainMetadata {

    public static final ModelNode ROOT = new ModelNode();

    static {
        ROOT.get("description").set("The root node of the domain-level management model. This node should be addressed via the key/value pair \"base => domain\"");
        ROOT.get("attributes").add(CommonMetadata.NAMESPACE_PREFIX_ATTRIBUTE);
        ROOT.get("attributes").add(CommonMetadata.SCHEMA_LOCATION_ATTRIBUTE);
        ROOT.get("children").get(0).get("name").set("path");
        ROOT.get("children").get(0).get("min-occurs").set(0);
        ROOT.get("children").get(0).get("max-occurs").set(Integer.MAX_VALUE);
        ROOT.get("children").get(0).get("metainfo").set(CommonMetadata.PATH_NODE);
        ROOT.get("children").get(1).get("name").set("profile");
        ROOT.get("children").get(2).get("name").set("interface");
        ROOT.get("children").get(3).get("name").set("socket-binding-group");
        ROOT.get("children").get(4).get("name").set("deployment");
        ROOT.get("children").get(5).get("name").set("server-group");
        ROOT.get("children").get(6).get("name").set("host");
        ROOT.get("children").get(7).get("name").set("server");
        ROOT.get("operations").add(CommonMetadata.PATH_ADD_OPERATION);
        ROOT.get("operations").add(CommonMetadata.PATH_REMOVE_OPERATION);

    }

    public static void main(final String[] args) {
        System.out.println(ROOT);
    }

}

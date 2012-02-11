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
package org.jboss.as.test.integration.management.util;

import java.util.LinkedList;
import java.util.List;
import org.jboss.dmr.ModelNode;

/**
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
public class ModelUtil {

    public static List<String> modelNodeAsStingList(ModelNode node) {
        List<String> ret = new LinkedList<String>();
        for (ModelNode n : node.asList()) ret.add(n.asString());
        return ret;
    }

    public static ModelNode createCompositeNode(ModelNode[] steps) {
        ModelNode comp = new ModelNode();
        comp.get("operation").set("composite");
        for (ModelNode step : steps) {
            comp.get("steps").add(step);
        }
        return comp;
    }

    public static ModelNode createOpNode(String address, String operation) {
        ModelNode op = new ModelNode();

        // set address
        ModelNode list = op.get("address").setEmptyList();
        if (address != null) {
            String[] pathSegments = address.split("/");
            for (String segment : pathSegments) {
                String[] elements = segment.split("=");
                list.add(elements[0], elements[1]);
            }
        }
        op.get("operation").set(operation);
        return op;
    }
}

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
package org.jboss.as.cli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 *
 * @author Alexey Loubyansky
 */
public class Util {

    public static boolean isSuccess(ModelNode operationResult) {
        if(operationResult != null) {
            ModelNode outcome = operationResult.get("outcome");
            return outcome != null && outcome.asString().equals("success");
        }
        return false;
    }

    public static List<String> getList(ModelNode operationResult) {
        if(!operationResult.hasDefined("result"))
            return Collections.emptyList();

        List<ModelNode> nodeList = operationResult.get("result").asList();
        if(nodeList.isEmpty())
            return Collections.emptyList();

        List<String> list = new ArrayList<String>(nodeList.size());
        for(ModelNode node : nodeList) {
            list.add(node.asString());
        }
        return list;
    }

    public static List<String> getRequestPropertyNames(ModelNode operationResult) {
        if(!operationResult.hasDefined("result"))
            return Collections.emptyList();

        ModelNode result = operationResult.get("result");
        if(!result.hasDefined("request-properties"))
            return Collections.emptyList();

        List<Property> nodeList = result.get("request-properties").asPropertyList();
        if(nodeList.isEmpty())
            return Collections.emptyList();

        List<String> list = new ArrayList<String>(nodeList.size());
        for(Property node : nodeList) {
            list.add(node.getName());
        }
        return list;
    }
}

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
package org.jboss.as.test.smoke.embedded.mgmt.datasource;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.junit.Assert;

public class DataSourceOperationTestUtil {

    static Map<String, ModelNode> getChildren(final ModelNode result) {
        Assert.assertTrue(result.isDefined());
        final Map<String, ModelNode> steps = new HashMap<String, ModelNode>();
        for (final Property property : result.asPropertyList()) {
            steps.put(property.getName(), property.getValue());
        }
        return steps;
    }

    static void testConnection(final String dsName, final ModelControllerClient client) throws Exception {
        final ModelNode address3 = new ModelNode();
        address3.add("subsystem", "datasources");
        address3.add("data-source", dsName);
        address3.protect();

        final ModelNode operation3 = new ModelNode();
        operation3.get(OP).set("test-connection-in-pool");
        operation3.get(OP_ADDR).set(address3);

        final ModelNode result3 = client.execute(operation3);
        Assert.assertEquals(SUCCESS, result3.get(OUTCOME).asString());
    }

    static void testConnectionXA(final String dsName, final ModelControllerClient client) throws Exception {
        final ModelNode address3 = new ModelNode();
        address3.add("subsystem", "datasources");
        address3.add("xa-data-source", dsName);
        address3.protect();

        final ModelNode operation3 = new ModelNode();
        operation3.get(OP).set("test-connection-in-pool");
        operation3.get(OP_ADDR).set(address3);

        final ModelNode result3 = client.execute(operation3);
        Assert.assertEquals(SUCCESS, result3.get(OUTCOME).asString());
    }
    static ModelNode findNodeWithProperty(List<ModelNode> newList,String propertyName,String setTo){
    	ModelNode toReturn=null;
    	for(ModelNode result : newList){
            final Map<String, ModelNode> parseChildren = getChildren(result);
            if (! parseChildren.isEmpty() && parseChildren.get(propertyName)!= null && parseChildren.get(propertyName).asString().equals(setTo)) {
                toReturn=result;break;
            }
        }
    	//Assert.assertNotNull("Not found "+propertyName+"=>"+setTo+" in:\n"+newList,toReturn);
    	return toReturn;
    }
}

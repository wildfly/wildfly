/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.management.api.expression;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESOLVE_EXPRESSIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;

import java.io.IOException;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.junit.Assert;

/**
 * Utility class used for manipulation with system properties via dmr api.
 *
 * @author <a href="ochaloup@jboss.com">Ondrej Chaloupka</a>
 */
public class Utils {
    private static final Logger log = Logger.getLogger(Utils.class);

    public static void setProperty(String name, String value, ModelControllerClient client) {
        ModelNode modelNode = createOpNode("system-property=" + name, ADD);
        modelNode.get(VALUE).set(value);
        ModelNode result = executeOp(modelNode, client);
        log.debugf("Added property %s, result: %s", name, result);
    }

    public static void removeProperty(String name, ModelControllerClient client) {
        ModelNode modelNode = createOpNode("system-property=" + name, REMOVE);
        ModelNode result = executeOp(modelNode, client);
        log.debugf("Removing property %s. Result: %s.", name, result);
    }

    public static void redefineProperty(String name, String value, ModelControllerClient client) {
        ModelNode modelNode = createOpNode("system-property=" + name, WRITE_ATTRIBUTE_OPERATION);
        modelNode.get(NAME).set(VALUE);
        modelNode.get(VALUE).set(value);
        ModelNode result = executeOp(modelNode, client);
        log.debugf("Redefine property %s to value %s. Result: %s.", name, value, result);
    }

    public static ModelNode executeOp(ModelNode op, ModelControllerClient client) {
        ModelNode modelNodeResult;
        try {
            modelNodeResult = client.execute(op);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        if (!SUCCESS.equals(modelNodeResult.get(OUTCOME).asString())) {
            throw new RuntimeException("Management operation: " + op + " was not successful. Result was: " + modelNodeResult);
        }
        return modelNodeResult;
    }

    public static String getProperty(String name, ModelControllerClient client) {
        ModelNode modelNode = createOpNode("system-property=" + name, READ_ATTRIBUTE_OPERATION);
        modelNode.get(NAME).set(VALUE);
        modelNode.get(RESOLVE_EXPRESSIONS).set(true);

        ModelNode result = executeOp(modelNode, client);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        ModelNode resolvedResult = result;//resolved by read operation already
        log.debugf("Resolved property %s with result: %s", name, resolvedResult);
        Assert.assertEquals(SUCCESS, resolvedResult.get(OUTCOME).asString());
        return resolvedResult.get("result").asString();
    }
}

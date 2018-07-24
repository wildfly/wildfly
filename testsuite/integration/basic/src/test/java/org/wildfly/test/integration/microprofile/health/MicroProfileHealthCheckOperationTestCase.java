/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.microprofile.health;

import static org.jboss.as.controller.operations.common.Util.getEmptyOperation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
public class MicroProfileHealthCheckOperationTestCase extends MicroProfileHealthTestBase{

    void checkGlobalOutcome(ManagementClient managementClient, boolean mustBeUP, String probeName) throws IOException {
        final ModelNode address = new ModelNode();
        address.add("subsystem", "microprofile-health-smallrye");
        ModelNode checkOp = getEmptyOperation("check", address);

        ModelNode response = managementClient.getControllerClient().execute(checkOp);

        final String opOutcome = response.get("outcome").asString();
        assertEquals("success", opOutcome);

        ModelNode result = response.get("result");
        assertEquals(mustBeUP? "UP" : "DOWN", result.get("outcome").asString());

        if (probeName != null) {
            for (ModelNode check : result.get("checks").asList()) {
                if (probeName.equals(check.get("name").asString())) {
                    // probe name found
                    // global outcome is driven by this probe state
                    assertEquals(mustBeUP? "UP" : "DOWN", check.get("state").asString());
                    return;
                }
            }
            fail("Probe named " + probeName + " not found in " + result);
        }


    }
}

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.manual.microprofile.health;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.dmr.ModelNode;

import java.io.IOException;
import java.net.ConnectException;

import static org.eclipse.microprofile.health.HealthCheckResponse.Status.DOWN;
import static org.eclipse.microprofile.health.HealthCheckResponse.Status.UP;
import static org.jboss.as.controller.operations.common.Util.getEmptyOperation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author <a href="http://xstefank.io/">Martin Stefanko</a> (c) 2021 Red Hat inc.
 */
public class MicroProfileHealthDefaultEmptyReadinessOperationTestCase extends MicroProfileHealthDefaultEmptyReadinessTestBase {

    void checkGlobalOutcome(ManagementClient managementClient, String operation, boolean mustBeUP, String probeName) throws IOException, InvalidHealthResponseException {
        final ModelNode address = new ModelNode();
        address.add("subsystem", "microprofile-health-smallrye");
        ModelNode checkOp = getEmptyOperation(operation, address);

        ModelNode response = null;
        try {
            response = managementClient.getControllerClient().execute(checkOp);
        } catch (IllegalStateException | IOException e) {
            // management client is not initialized yet
            throw new ConnectException(e.getMessage());
        }

        final String opOutcome = response.get("outcome").asString();

        // if system boot in process - WFLYCTL0379
        if (opOutcome.equals("failed") && response.get("failure-description").asString().contains("WFLYCTL0379")) {
            throw new ConnectException("System boot in process");
        }

        assertEquals("success", opOutcome);

        ModelNode result = response.get("result");
        final String status = result.get("status").asString();

        if (mustBeUP) {
            if (!"UP".equals(status)) {
                throw new InvalidHealthResponseException(UP, "CLI status " + status);
            }
        } else {
            if (!"DOWN".equals(status)) {
                throw new InvalidHealthResponseException(DOWN, "CLI status " + status);
            }
        }

        if (probeName != null) {
            for (ModelNode check : result.get("checks").asList()) {
                if (probeName.equals(check.get("name").asString())) {
                    // probe name found
                    // global outcome is driven by this probe state
                    assertEquals(mustBeUP ? "UP" : "DOWN", check.get("status").asString());
                    return;
                }
            }
            fail("Probe named " + probeName + " not found in " + result);
        }


    }
}

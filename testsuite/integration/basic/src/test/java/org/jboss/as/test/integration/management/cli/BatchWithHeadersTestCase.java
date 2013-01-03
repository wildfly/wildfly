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
package org.jboss.as.test.integration.management.cli;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.test.integration.management.util.CLITestUtil;
import org.jboss.dmr.ModelNode;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 *
 * @author Alexey Loubyansky
 */
@RunWith(Arquillian.class)
@RunAsClient
public class BatchWithHeadersTestCase {

    @Test
    public void testSuccessfulTry() throws Exception {
        final CommandContext ctx = CLITestUtil.getCommandContext();
        try {
            ctx.connectController();
            ctx.handle("batch");
            ctx.handle(":write-attribute(name=name,value=test");
            final ModelNode batchRequest = ctx.buildRequest("run-batch --headers={allow-resource-service-restart=true}");
            assertTrue(batchRequest.hasDefined("operation"));
            assertEquals("composite", batchRequest.get("operation").asString());
            assertTrue(batchRequest.hasDefined("address"));
            assertTrue(batchRequest.get("address").asList().isEmpty());
            assertTrue(batchRequest.hasDefined("steps"));
            List<ModelNode> steps = batchRequest.get("steps").asList();
            assertEquals(1, steps.size());
            final ModelNode op = steps.get(0);
            assertTrue(op.hasDefined("address"));
            assertTrue(op.get("address").asList().isEmpty());
            assertTrue(op.hasDefined("operation"));
            assertEquals("write-attribute", op.get("operation").asString());
            assertEquals("name", op.get("name").asString());
            assertEquals("test", op.get("value").asString());
            assertTrue(batchRequest.hasDefined("operation-headers"));
            final ModelNode headers = batchRequest.get("operation-headers");
            assertEquals("true", headers.get("allow-resource-service-restart").asString());
            ctx.handle("discard-batch");
        } finally {
            ctx.terminateSession();
        }
    }
}

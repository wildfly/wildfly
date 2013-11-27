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
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.test.integration.management.util.CLITestUtil;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 *
 * @author Alexey Loubyansky
 */
@RunWith(Arquillian.class)
@RunAsClient
public class BatchFileTestCase {

    private static final String FILE_NAME = "jboss-cli-batch-file-test.cli";
    private static final File TMP_FILE;
    static {
        TMP_FILE = new File(new File(TestSuiteEnvironment.getTmpDir()), FILE_NAME);
    }

    @AfterClass
    public static void cleanUp() {
        if(TMP_FILE.exists()) {
            TMP_FILE.delete();
        }
    }

    @Test
    public void testBatchFile() throws Exception {
        createFile(new String[]{"/system-property=batchfiletest:add(value=true)"});

        final CommandContext ctx = CLITestUtil.getCommandContext();
        try {
            ctx.connectController();
            ctx.handle("batch --file=" + TMP_FILE.getAbsolutePath());
            final ModelNode batchRequest = ctx.buildRequest("run-batch");
            assertTrue(batchRequest.hasDefined("operation"));
            assertEquals("composite", batchRequest.get("operation").asString());
            assertTrue(batchRequest.hasDefined("address"));
            assertTrue(batchRequest.get("address").asList().isEmpty());
            assertTrue(batchRequest.hasDefined("steps"));
            List<ModelNode> steps = batchRequest.get("steps").asList();
            assertEquals(1, steps.size());
            final ModelNode op = steps.get(0);
            assertTrue(op.hasDefined("address"));
            List<Property> address = op.get("address").asPropertyList();
            assertEquals(1, address.size());
            assertEquals("system-property", address.get(0).getName());
            assertEquals("batchfiletest", address.get(0).getValue().asString());

            assertTrue(op.hasDefined("operation"));
            assertEquals("add", op.get("operation").asString());
            assertEquals("true", op.get("value").asString());
            ctx.handle("discard-batch");
        } finally {
            ctx.terminateSession();
        }
    }

    @Test
    public void testRunBatchFile() throws Exception {
        createFile(new String[]{"/system-property=batchfiletest:add(value=true)"});

        final CommandContext ctx = CLITestUtil.getCommandContext();
        try {
            ctx.connectController();
            final ModelNode batchRequest = ctx.buildRequest("run-batch --file=" + TMP_FILE.getAbsolutePath() + " --headers={allow-resource-service-restart=true}");
            assertTrue(batchRequest.hasDefined("operation"));
            assertEquals("composite", batchRequest.get("operation").asString());
            assertTrue(batchRequest.hasDefined("address"));
            assertTrue(batchRequest.get("address").asList().isEmpty());
            assertTrue(batchRequest.hasDefined("steps"));
            List<ModelNode> steps = batchRequest.get("steps").asList();
            assertEquals(1, steps.size());
            final ModelNode op = steps.get(0);
            assertTrue(op.hasDefined("address"));
            List<Property> address = op.get("address").asPropertyList();
            assertEquals(1, address.size());
            assertEquals("system-property", address.get(0).getName());
            assertEquals("batchfiletest", address.get(0).getValue().asString());

            assertTrue(op.hasDefined("operation"));
            assertEquals("add", op.get("operation").asString());
            assertEquals("true", op.get("value").asString());
            assertTrue(batchRequest.hasDefined("operation-headers"));
            final ModelNode headers = batchRequest.get("operation-headers");
            assertEquals("true", headers.get("allow-resource-service-restart").asString());
        } finally {
            ctx.terminateSession();
        }
    }

    protected void createFile(String[] cmd) {
        if(TMP_FILE.exists()) {
            if(!TMP_FILE.delete()) {
                fail("Failed to delete " + TMP_FILE.getAbsolutePath());
            }
        }

        FileWriter writer = null;
        try {
            writer = new FileWriter(TMP_FILE);
            for(String line : cmd) {
                writer.write(line);
                writer.write('\n');
            }
        } catch (IOException e) {
            fail("Failed to write to " + TMP_FILE.getAbsolutePath() + ": " + e.getLocalizedMessage());
        } finally {
            if(writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                }
            }
        }
    }
}

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

package org.jboss.as.test.integration.management.cli;

import java.io.IOException;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.as.test.integration.management.util.CLIOpResult;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests related to read ops for wildcard addresses.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class WildCardReadsTestCase extends AbstractCliTestBase {

    private static final String OP_PATTERN = "/subsystem=infinispan/cache-container=web/distributed-cache=dist/eviction=%s:%s";
    private static final String READ_OP_DESC_OP = ModelDescriptionConstants.READ_OPERATION_DESCRIPTION_OPERATION + "(name=%s)";
    private static final String READ_RES_DESC_OP = ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION + "(access-control=combined-descriptions,operations=true,recursive=true)";
    private static final String EVICTION = "EVICTION";

    @BeforeClass
    public static void before() throws Exception {
        initCLI();
    }

    @AfterClass
    public static void after() throws Exception {
        closeCLI();
    }

    /**
     * Tests WFLY-2527 added behavior of supporting read-operation-description for
     * wildcard addresses where there is no generic resource registration for the type
     */
    @Test
    public void testLenientReadOperationDescription() throws IOException {
        cli.sendLine(String.format(OP_PATTERN, EVICTION, ModelDescriptionConstants.READ_OPERATION_NAMES_OPERATION));
        CLIOpResult opResult = cli.readAllAsOpResult();
        Assert.assertTrue(opResult.isIsOutcomeSuccess());
        for (ModelNode node : opResult.getResponseNode().get(ModelDescriptionConstants.RESULT).asList()) {
            String opPart = String.format(READ_OP_DESC_OP, node.asString());
            cli.sendLine(String.format(OP_PATTERN, EVICTION, opPart));
            opResult = cli.readAllAsOpResult();
            Assert.assertTrue(opResult.isIsOutcomeSuccess());
            ModelNode specific = opResult.getResponseNode().get(ModelDescriptionConstants.RESULT);
            cli.sendLine(String.format(OP_PATTERN, "*", opPart));
            opResult = cli.readAllAsOpResult();
            Assert.assertTrue(opResult.isIsOutcomeSuccess());
            Assert.assertEquals("mismatch for " + node.asString(), specific, opResult.getResponseNode().get(ModelDescriptionConstants.RESULT));
        }
    }

    /**
     * Tests WFLY-2527 fix.
     */
    @Test
    public void testReadResourceDescriptionNoGenericRegistration() throws IOException {
        cli.sendLine(String.format(OP_PATTERN, EVICTION, READ_RES_DESC_OP));
        CLIOpResult opResult = cli.readAllAsOpResult();
        Assert.assertTrue(opResult.isIsOutcomeSuccess());
        ModelNode specific = opResult.getResponseNode().get(ModelDescriptionConstants.RESULT);
        cli.sendLine(String.format(OP_PATTERN, "*", READ_RES_DESC_OP));
        opResult = cli.readAllAsOpResult();
        Assert.assertTrue(opResult.isIsOutcomeSuccess());
        ModelNode generic = opResult.getResponseNode().get(ModelDescriptionConstants.RESULT);
        Assert.assertEquals(ModelType.LIST, generic.getType());
        Assert.assertEquals(1, generic.asInt());
        Assert.assertEquals(specific, generic.get(0).get(ModelDescriptionConstants.RESULT));
    }
}

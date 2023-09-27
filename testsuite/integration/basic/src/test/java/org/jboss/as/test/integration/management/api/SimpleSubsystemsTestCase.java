/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.management.api;

import java.io.IOException;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.junit.Assert.assertTrue;


/**
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SimpleSubsystemsTestCase extends ContainerResourceMgmtTestBase {

    @Test
    @Ignore
    /*
     * The jaxrs subsystem is no longer empty.
     */
    public void testJaxrs() throws Exception {
        testSimpleSubsystem("jaxrs");
    }

    @Test
    public void testSar() throws Exception {
        testSimpleSubsystem("sar");
    }

    @Test
    public void testPojo() throws Exception {
        testSimpleSubsystem("pojo");
    }

    @Test
    public void testJdr() throws Exception {
        testSimpleSubsystem("jdr");
    }

    private void testSimpleSubsystem(String subsystemName) throws IOException, MgmtOperationException {
        ModelNode op = createOpNode("subsystem=" + subsystemName, "read-resource");

        ModelNode result = executeOperation(op);
        assertTrue("Subsystem not empty.", result.keys().size() == 0);
    }

}

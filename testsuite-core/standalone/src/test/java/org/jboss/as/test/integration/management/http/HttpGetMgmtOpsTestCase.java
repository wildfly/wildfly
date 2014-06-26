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
package org.jboss.as.test.integration.management.http;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.as.test.integration.management.util.HttpMgmtProxy;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.core.testrunner.WildflyTestRunner;

/**
 * Tests all management operation types which are available via HTTP GET requests.
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@RunWith(WildflyTestRunner.class)
public class HttpGetMgmtOpsTestCase {

    private static final int MGMT_PORT = 9990;
    private static final String MGMT_CTX = "/management";


    @BeforeClass
    public static void before() {
    }

    @Test
    public void testReadResource() throws Exception {
        testReadResource(false);
    }


    @Test
    public void testReadResourceRecursive() throws Exception {
        testReadResource(true);
    }

    private void testReadResource(boolean recursive) throws Exception {

        URL mgmtURL = new URL("http", "localhost", MGMT_PORT, MGMT_CTX);
        HttpMgmtProxy httpMgmt = new HttpMgmtProxy(mgmtURL);

        String cmd = recursive ? "/subsystem/logging?operation=resource&recursive=true" : "/subsystem/logging?operation=resource";

        ModelNode node = httpMgmt.sendGetCommand(cmd);

        assertTrue(node.has("root-logger"));
        ModelNode rootLogger = node.get("root-logger");
        assertTrue(rootLogger.has("ROOT"));
        ModelNode root = rootLogger.get("ROOT");

        if (recursive) {
            assertTrue(root.has("level"));
        } else {
            assertFalse(root.isDefined());
        }
    }

    @Test
    public void testReadAttribute() throws Exception {

        URL mgmtURL = new URL("http", "localhost", MGMT_PORT, MGMT_CTX);
        HttpMgmtProxy httpMgmt = new HttpMgmtProxy(mgmtURL);

        ModelNode node = httpMgmt.sendGetCommand("/subsystem/logging?operation=attribute&name=add-logging-api-dependencies");

        // check that a boolean is returned
        assertTrue(node.getType() == ModelType.BOOLEAN);
    }


    @Test
    public void testReadResourceDescription() throws Exception {

        URL mgmtURL = new URL("http", "localhost", MGMT_PORT, MGMT_CTX);
        HttpMgmtProxy httpMgmt = new HttpMgmtProxy(mgmtURL);

        ModelNode node = httpMgmt.sendGetCommand("/subsystem/logging?operation=resource-description");

        assertTrue(node.has("description"));
        assertTrue(node.has("attributes"));
    }


    @Test
    public void testReadOperationNames() throws Exception {
        URL mgmtURL = new URL("http", "localhost", MGMT_PORT, MGMT_CTX);
        HttpMgmtProxy httpMgmt = new HttpMgmtProxy(mgmtURL);

        ModelNode node = httpMgmt.sendGetCommand("/subsystem/logging?operation=operation-names");

        List<ModelNode> names = node.asList();

        System.out.println(names.toString());
        Set<String> strNames = new TreeSet<String>();
        for (ModelNode n : names) { strNames.add(n.asString()); }

        assertTrue(strNames.contains("read-attribute"));

        assertTrue(strNames.contains("read-children-names"));
        assertTrue(strNames.contains("read-children-resources"));
        assertTrue(strNames.contains("read-children-types"));
        assertTrue(strNames.contains("read-operation-description"));
        assertTrue(strNames.contains("read-operation-names"));
        assertTrue(strNames.contains("read-resource"));
        assertTrue(strNames.contains("read-resource-description"));
        assertTrue(strNames.contains("write-attribute"));
    }

    @Test
    public void testReadOperationDescription() throws Exception {

        URL mgmtURL = new URL("http", "localhost", MGMT_PORT, MGMT_CTX);
        HttpMgmtProxy httpMgmt = new HttpMgmtProxy(mgmtURL);

        ModelNode node = httpMgmt.sendGetCommand("/subsystem/logging?operation=operation-description&name=add");

        assertTrue(node.has("operation-name"));
        assertTrue(node.has("description"));
        assertTrue(node.has("request-properties"));
    }
}

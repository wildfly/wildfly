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

import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.http.Authentication;
import org.jboss.as.test.integration.management.util.HttpMgmtProxy;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests all management operation types which are available via HTTP GET requests.
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class HttpGetMgmtOpsTestCase {

    private static final int    MGMT_PORT = 9990;
    private static final String MGMT_CTX = "/management";

    @ArquillianResource URL url;

    @Deployment
    public static Archive<?> getDeployment() {
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "dummy.jar");
        ja.addClass(HttpGetMgmtOpsTestCase.class);
        return ja;
    }

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

        URL mgmtURL = new URL(url.getProtocol(), url.getHost(), MGMT_PORT, MGMT_CTX);
        HttpMgmtProxy httpMgmt = new HttpMgmtProxy(mgmtURL);

        String cmd = recursive ?
                "/subsystem/web?operation=resource&recursive=true" : "/subsystem/web?operation=resource";

        ModelNode node = httpMgmt.sendGetCommand(cmd);

        assertTrue(node.has("virtual-server"));
        ModelNode vServer = node.get("virtual-server");

        assertTrue(vServer.has("default-host"));
        ModelNode host = vServer.get("default-host");

        if (recursive) {
            assertTrue(host.has("alias"));
        } else {
            assertFalse(host.isDefined());
        }

    }

    @Test
    public void testReadAttribute() throws Exception {

       URL mgmtURL = new URL(url.getProtocol(), url.getHost(), MGMT_PORT, MGMT_CTX);
        HttpMgmtProxy httpMgmt = new HttpMgmtProxy(mgmtURL);

        ModelNode node = httpMgmt.sendGetCommand("/subsystem/web?operation=attribute&name=native");

        // check that a boolean is returned
        assertTrue(node.asBoolean() || (! node.asBoolean()));

    }


    @Test
    public void testReadResourceDescription() throws Exception {

        URL mgmtURL = new URL(url.getProtocol(), url.getHost(), MGMT_PORT, MGMT_CTX);
        HttpMgmtProxy httpMgmt = new HttpMgmtProxy(mgmtURL);

        ModelNode node = httpMgmt.sendGetCommand("/subsystem/web?operation=resource-description");


        assertTrue(node.has("description"));
        assertTrue(node.has("attributes"));
    }


    @Test
    public void testReadOperationNames() throws Exception {
        URL mgmtURL = new URL(url.getProtocol(), url.getHost(), MGMT_PORT, MGMT_CTX);
        HttpMgmtProxy httpMgmt = new HttpMgmtProxy(mgmtURL);

        ModelNode node = httpMgmt.sendGetCommand("/subsystem/web?operation=operation-names");

        List<ModelNode> names = node.asList();

        System.out.println(names.toString());
        Set<String> strNames = new TreeSet<String>();
        for (ModelNode n : names) strNames.add(n.asString());

        assertTrue(strNames.contains("read-attribute"));

        assertTrue(strNames.contains("read-children-names"));
        assertTrue(strNames.contains("read-children-resources"));
        assertTrue(strNames.contains("read-children-types"));
        assertTrue(strNames.contains("read-operation-description"));
        assertTrue(strNames.contains("read-operation-names"));
        assertTrue(strNames.contains("read-resource"));
        assertTrue(strNames.contains("read-resource-description"));
        assertTrue(strNames.contains("validate-address"));
        assertTrue(strNames.contains("write-attribute"));
    }

    @Test
    public void testReadOperationDescription() throws Exception {

        URL mgmtURL = new URL(url.getProtocol(), url.getHost(), MGMT_PORT, MGMT_CTX);
        HttpMgmtProxy httpMgmt = new HttpMgmtProxy(mgmtURL);

        ModelNode node = httpMgmt.sendGetCommand("/subsystem/web?operation=operation-description&name=add");

        assertTrue(node.has("operation-name"));
        assertTrue(node.has("description"));
        assertTrue(node.has("request-properties"));
    }
}

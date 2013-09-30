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

import org.jboss.as.test.integration.management.base.AbstractCliTestBase;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import static org.hamcrest.CoreMatchers.is;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.management.util.CLIOpResult;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class GlobalOpsTestCase extends AbstractCliTestBase {

    @ArquillianResource URL url;

    @BeforeClass
    public static void before() throws Exception {
        AbstractCliTestBase.initCLI();
    }

    @AfterClass
    public static void after() throws Exception {
        AbstractCliTestBase.closeCLI();
    }

    @Deployment
    public static Archive<?> getDeployment() {
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "dummy.jar");
        ja.addClass(GlobalOpsTestCase.class);
        return ja;
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
        cli.sendLine("/subsystem=undertow:read-resource(recursive="+ String.valueOf(recursive) +")");
        CLIOpResult result = cli.readAllAsOpResult();

        assertTrue(result.isIsOutcomeSuccess());
        assertTrue(result.getResult() instanceof Map);
        Map map = (Map) result.getResult();

        assertTrue(map.get("server") instanceof Map);

        Map vServer = (Map) map.get("server");
        assertTrue(vServer.containsKey("default-server"));

        if (recursive) {
            assertTrue(vServer.get("default-server") instanceof Map);
            Map host = (Map) vServer.get("default-server");
            assertTrue(host.containsKey("default-host"));
        } else {
            assertTrue(vServer.get("default-server").equals("undefined"));
        }
    }

    @Test
    public void testReadAttribute() throws Exception {
        cli.sendLine("/subsystem=undertow:read-attribute(name=default-servlet-container)");
        CLIOpResult result = cli.readAllAsOpResult();

        assertTrue(result.isIsOutcomeSuccess());
        assertTrue(result.getResult().equals("default"));

    }


    @Test
    public void testReadResourceDescription() throws Exception {
        cli.sendLine("/subsystem=undertow:read-resource-description");
        CLIOpResult result = cli.readAllAsOpResult();

        assertTrue(result.isIsOutcomeSuccess());
        assertTrue(result.getResult() instanceof Map);
        Map map = (Map) result.getResult();

        assertTrue(map.containsKey("description"));
        assertTrue(map.containsKey("attributes"));
    }

    @Test
    public void testReadOperationNames() throws Exception {
        cli.sendLine("/subsystem=undertow:read-operation-names");
        CLIOpResult result = cli.readAllAsOpResult();

        assertTrue(result.isIsOutcomeSuccess());
        assertTrue(result.getResult() instanceof List);
        List names = (List) result.getResult();

        assertTrue(names.contains("read-attribute"));
        assertTrue(names.contains("read-children-names"));
        assertTrue(names.contains("read-children-resources"));
        assertTrue(names.contains("read-children-types"));
        assertTrue(names.contains("read-operation-description"));
        assertTrue(names.contains("read-operation-names"));
        assertTrue(names.contains("read-resource"));
        assertTrue(names.contains("read-resource-description"));
        assertTrue(names.contains("write-attribute"));

    }

    @Test
    public void testReadOperationDescription() throws Exception {
        cli.sendLine("/subsystem=undertow:read-operation-description(name=add)");
        CLIOpResult result = cli.readAllAsOpResult();

        assertTrue(result.isIsOutcomeSuccess());
        assertTrue(result.getResult() instanceof Map);
        Map map = (Map) result.getResult();

        assertTrue(map.containsKey("operation-name"));
        assertTrue(map.containsKey("description"));
        assertTrue(map.containsKey("request-properties"));
    }

    @Test
    public void testReadChildrenTypes() throws Exception {
        cli.sendLine("/subsystem=undertow:read-children-types");
        CLIOpResult result = cli.readAllAsOpResult();

        assertTrue(result.isIsOutcomeSuccess());
        assertTrue(result.getResult() instanceof List);
        List types = (List) result.getResult();

        assertTrue(types.contains("server"));
        assertTrue(types.contains("servlet-container"));
    }

    @Test
    public void testReadChildrenNames() throws Exception {
        cli.sendLine("/subsystem=undertow:read-children-names(child-type=server)");
        CLIOpResult result = cli.readAllAsOpResult();

        assertTrue(result.isIsOutcomeSuccess());
        assertTrue(result.getResult() instanceof List);
        List names = (List) result.getResult();

        assertTrue(names.contains("default-server"));
    }

    @Test
    public void testReadChildrenResources() throws Exception {
        cli.sendLine("/subsystem=undertow:read-children-resources(child-type=server)");
        CLIOpResult result = cli.readAllAsOpResult();

        assertTrue(result.isIsOutcomeSuccess());
        assertTrue(result.getResult() instanceof Map);
        Map res = (Map) result.getResult();
        assertTrue(res.get("default-server") instanceof Map);

    }

    @Test
    public void testAddRemoveOperation() throws Exception {

        // add new connector
        cli.sendLine("/socket-binding-group=standard-sockets/socket-binding=test:add(port=8181)");
        CLIOpResult result = cli.readAllAsOpResult();
        assertTrue(result.isIsOutcomeSuccess());

        cli.sendLine("/subsystem=undertow/server=default-server/http-listener=test-listener:add(socket-binding=test, enabled=true)");
        result = cli.readAllAsOpResult();
        assertTrue(result.isIsOutcomeSuccess());

        // check that the connector is live
        String cURL = "http://" + url.getHost() + ":8181";

        //this tests for default content serving...
        String response = HttpRequest.get(cURL, 10, TimeUnit.SECONDS);
        assertTrue("Invalid response: " + response, response.indexOf("JBoss") >=0);


        // remove connector
        cli.sendLine("/subsystem=undertow/server=default-server/http-listener=test-listener:remove{allow-resource-service-restart=true}");
        result = cli.readAllAsOpResult();
        assertTrue(result.isIsOutcomeSuccess());

        cli.sendLine("/socket-binding-group=standard-sockets/socket-binding=test:remove");
        result = cli.readAllAsOpResult();
        assertTrue(result.isIsOutcomeSuccess());

        // check that the connector is no longer live
        Thread.sleep(5000);
        boolean failed = false;
        try {
            response = HttpRequest.get(cURL, 10, TimeUnit.SECONDS);
        } catch (Exception e) {
            failed = true;
        }
        assertTrue("Connector still live: " + response, failed);
    }

    @Test
    public void testCompositeOp() throws Exception {
        cli.sendLine("/:composite(steps=[{\"operation\"=>\"read-resource\"}])");
        CLIOpResult result = cli.readAllAsOpResult();

        assertTrue(result.isIsOutcomeSuccess());
        assertTrue(result.getResult() instanceof Map);
        Map map = (Map) result.getResult();

        assertTrue(map.get("step-1") instanceof Map);

        assertTrue(((Map)map.get("step-1")).get("result") instanceof Map);

        assertTrue(((Map)((Map)map.get("step-1")).get("result")).containsKey("management-major-version"));

    }

    @Test
    public void testStringValueParsing() throws Exception {
        cli.sendLine("/subsystem=logging/console-handler=TEST-FILTER:add");
        CLIOpResult result = cli.readAllAsOpResult();
        assertTrue(result.isIsOutcomeSuccess());
        cli.sendLine("/subsystem=logging/console-handler=TEST-FILTER:write-attribute(name=filter-spec, value=\"substituteAll(\\\"JBAS\\\",\\\"DUMMY\\\")\")");
        result = cli.readAllAsOpResult();
        assertTrue(result.isIsOutcomeSuccess());
        cli.sendLine("/subsystem=logging/console-handler=TEST-FILTER:read-resource(recursive=true)");
        result = cli.readAllAsOpResult();
        assertTrue(result.isIsOutcomeSuccess());
        Map<String, Object> resource = result.getResultAsMap();
        assertTrue(resource.containsKey("filter-spec"));
        assertThat((String) resource.get("filter-spec"), is("substituteAll(\"JBAS\",\"DUMMY\")"));
    }
}

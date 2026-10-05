/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.jmx.rbac.deployment;

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/")
@Produces(MediaType.TEXT_PLAIN)
public class JmxResource {
    private static final String DATASOURCE_MBEAN_NAME = "jboss.as:subsystem=datasources,data-source=ExampleDS,statistics=pool";
    private static final String ROOT_MBEAN_NAME = "jboss.as:management-root=server";

    String HOST_NAME = "localhost";
    String PORT = "9990";


    @GET
    @Path("/platform")
    public Response usePlatformMBeanServer() {
        try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            return callMBeanServer(server);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @GET
    @Path("/found")
    public Response useFoundMBeanServer() throws Exception {
        List<MBeanServer> servers = MBeanServerFactory.findMBeanServer(null);

        Response response = null;
        for (int i = 0; i < servers.size(); i++) {
            MBeanServer server = servers.get(i);

            if (server.isRegistered(ObjectName.getInstance(DATASOURCE_MBEAN_NAME))) {
                response = callMBeanServer(server);
            }
        }

        if (response == null) {
            throw new IllegalStateException("No MBeanServer found");
        }
        return response;
    }

    @GET
    @Path("/remote")
    public Response useRemoteMBeanServer() throws Exception {
        JMXServiceURL jmx_service_url = new JMXServiceURL("service:jmx:remote+http://" + HOST_NAME + ":" + PORT);
        Map<String,Object> environment = new HashMap<String,Object>();
        JMXConnector connector = JMXConnectorFactory.connect(jmx_service_url, null);
        MBeanServerConnection conn = connector.getMBeanServerConnection();
        try {
            return callMBeanServerAccessOnly(conn);
        } finally {
            connector.close();
        }
    }

    private Response callMBeanServer(MBeanServerConnection server) {
        try {
            server.getAttribute(new ObjectName(DATASOURCE_MBEAN_NAME), "ActiveCount");
            Object whoAmI = server.invoke(new ObjectName(ROOT_MBEAN_NAME), "whoami", new Object[]{Boolean.TRUE}, new String[]{"verbose"});
            return Response.ok(whoAmI.toString()).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Response callMBeanServerAccessOnly(MBeanServerConnection server) {
        try {
            server.getAttribute(new ObjectName(DATASOURCE_MBEAN_NAME), "ActiveCount");
            return Response.ok("ok").build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

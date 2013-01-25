/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.manualmode.web.valve.authenticator;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import static org.junit.Assert.assertEquals;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * @author Jean-Frederic Clere
 * @author Ondrej Chaloupka
 */
public class ValveUtil {

    private static Logger log = Logger.getLogger(ValveUtil.class);

    public static void createValveModule(final ManagementClient managementClient, String modulename, String baseModulePath, String jarName, Class valveClass) throws Exception {
        log.info("Creating a valve module " + modulename);
        String path = ValveUtil.readASPath(managementClient.getControllerClient());
        File file = new File(path);
        if (file.exists()) {
            file = new File(path + baseModulePath);
            file.mkdirs();
            file = new File(path + baseModulePath + "/" + jarName);
            if (file.exists()) {
                file.delete();
            }
            createJar(file, valveClass);
            file = new File(path + baseModulePath + "/module.xml");
            if (file.exists()) {
                file.delete();
            }              
            FileWriter fstream = new FileWriter(path + baseModulePath + "/module.xml");
            BufferedWriter out = new BufferedWriter(fstream);
            out.write("<module xmlns=\"urn:jboss:module:1.1\" name=\"" + modulename + "\">\n");
            out.write("    <properties>\n");
            out.write("        <property name=\"jboss.api\" value=\"private\"/>\n");
            out.write("    </properties>\n");

            out.write("    <resources>\n");
            out.write("        <resource-root path=\""+ jarName + "\"/>\n");
            out.write("    </resources>\n");

            out.write("    <dependencies>\n");
            out.write("        <module name=\"sun.jdk\"/>\n");
            out.write("        <module name=\"javax.servlet.api\"/>\n");
            out.write("        <module name=\"org.jboss.as.web\"/>\n");
            out.write("       <module name=\"org.jboss.logging\"/>\n");
            out.write("    </dependencies>\n");
            out.write("</module>");
            out.close();
        }
    }
    
    private static void createJar(File file, Class valveClass) {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "temporary-name.jar")
                              .addClass(valveClass);       
        log.info(archive.toString(true));
        archive.as(ZipExporter.class).exportTo(file);
    }
    
    /**
     * Adding valve via DMR operation for jboss config file (standalone.xml)
     */
    public static void addValve(final ManagementClient managementClient, String valveName, String modulename, String classname, Map<String,String> params) throws Exception {
        List<ModelNode> updates = new ArrayList<ModelNode>();

        ModelNode op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).set(getValveAddr(valveName));
        // op.get(NAME).set(valveName);
        op.get("enabled").set("true");
        op.get("class-name").set(classname);
        op.get("module").set(modulename);
        updates.add(op);
        
        if(params != null) {
            for (String paramName: params.keySet()) {
                op = new ModelNode();
                op.get(OP).set("add-param");
                op.get(OP_ADDR).set(getValveAddr(valveName));
                op.get("param-name").set(paramName);
                op.get("param-value").set(params.get(paramName));
                updates.add(op);
            }
        }

        applyUpdates(managementClient.getControllerClient(), updates);
    }
    
   
    
    public static void activateValve(final ManagementClient managementClient, String valveName, boolean isActive) throws Exception {
        ModelNode op = new ModelNode();
        op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        op.get(OP_ADDR).set(getValveAddr(valveName));
        op.get(NAME).set("enabled");
        op.get(VALUE).set(Boolean.toString(isActive));
        applyUpdate(managementClient, op);
    }
    
    private static ModelNode getValveAddr(String valveName) {
        ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, "web");
        address.add("valve", valveName);
        address.protect();
        return address;
    }    

    public static void removeValve(final ManagementClient managementClient, String valveName) throws Exception {
        ModelNode op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(OP_ADDR).set(getValveAddr(valveName));

        applyUpdate(managementClient, op);
    }

    public static void applyUpdate(final ManagementClient client, final ModelNode update) throws Exception {
        final List<ModelNode> updates = new ArrayList<ModelNode>();
        updates.add(update);
        applyUpdates(client.getControllerClient(), updates);
    }
    
    public static void applyUpdates(final ModelControllerClient client, final List<ModelNode> updates) throws Exception {
        for (ModelNode update : updates) {
            log.info("+++ Update on " + client + ":\n" + update.toString());
            ModelNode result = client.execute(new OperationBuilder(update).build());
            log.info("Whole result: " + result);
            if (result.hasDefined("outcome") && "success".equals(result.get("outcome").asString())) {
                if (result.hasDefined("result")) {
                    log.info(result.get("result"));
                }
            } else if (result.hasDefined("failure-description")) {
                throw new RuntimeException(result.get("failure-description").toString());
            } else {
                throw new RuntimeException("Operation not successful; outcome = " + result.get("outcome"));
            }
        }
    }

    /**
     * Provide reload operation on server
     *
     * @throws Exception
     */
    public static void reload(final ManagementClient managementClient) throws Exception {
        ModelNode operation = new ModelNode();
        operation.get(OP).set("reload");

        try {
            applyUpdate(managementClient, operation);
        } catch (Exception e) {
            log.error("Exception applying reload operation. This is probably fine, as the server probably shut down before the response was sent", e);
        }
        boolean reloaded = false;
        int i = 0;
        while (!reloaded) {
            try {
                Thread.sleep(2000);
                if (managementClient.isServerInRunningState()) {
                    reloaded = true;
                }
            } catch (Throwable t) {
                // nothing to do, just waiting
            } finally {
                if (!reloaded && i++ > 20) {
                    throw new Exception("Server reloading failed");
                }
            }
        }
    }

    public static String readASPath(final ModelControllerClient client) throws Exception {
        ModelNode op = new ModelNode();
        op.get(OP).set("read-attribute");
        op.get(OP_ADDR).add("path", "jboss.home.dir");
        op.get("name").set("path");
        ModelNode result = client.execute(new OperationBuilder(op).build());
        if (result.hasDefined("outcome") && "success".equals(result.get("outcome").asString())) {
            if (result.hasDefined("result")) {
                return result.get("result").asString();
            }
        } else if (result.hasDefined("failure-description")) {
            throw new RuntimeException(result.get("failure-description").toString());
        } else {
            throw new RuntimeException("Operation not successful; outcome = " + result.get("outcome"));
        }
        return null;
    }
    
     /**
     * Access http://localhost/
     * @return "valve" headers
     */
    public static Header[] hitValve(URL url, int expectedResponseCode) throws Exception {
        HttpGet httpget = new HttpGet(url.toURI());
        DefaultHttpClient httpclient = new DefaultHttpClient();

        log.info("executing request" + httpget.getRequestLine());
        HttpResponse response = httpclient.execute(httpget);

        int statusCode = response.getStatusLine().getStatusCode();
        Header[] errorHeaders = response.getHeaders("X-Exception");
        assertEquals("Wrong response code: " + statusCode + " On " + url, expectedResponseCode, statusCode);
        assertEquals("X-Exception(" + Arrays.toString(errorHeaders) + ") is null", 0, errorHeaders.length);
        
        return response.getHeaders("valve");
    }
    
     public static Header[] hitValve(URL url) throws Exception {
         return hitValve(url, HttpURLConnection.HTTP_OK);
         
     }
}

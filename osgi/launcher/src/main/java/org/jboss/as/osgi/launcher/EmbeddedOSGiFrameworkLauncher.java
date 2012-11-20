/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.osgi.launcher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.embedded.EmbeddedServerFactory;
import org.jboss.as.embedded.ServerStartException;
import org.jboss.as.embedded.StandaloneServer;
import org.jboss.dmr.ModelNode;
import org.osgi.framework.Constants;

/**
 * Launch the Application Server using its EmbeddedServerFactory and activate the
 * OSGi subsystem.
 *
 * @author David Bosschaert
 */
public class EmbeddedOSGiFrameworkLauncher {
    private static StandaloneServer server;

    /*
    // Testing code, can be used to launch from the command line...
    public static void main(String ... args) throws Exception {
        System.out.println("Checking class path...");
        for (String cp : System.getProperty("java.class.path").split("[:]")) {
            File f = new File(cp);
            boolean exists = f.exists();
            System.out.println("  Checking: " + cp + (exists ? " exists" : " DOES NOT EXIST"));
            if (!exists)
                System.exit(-1);
        }

        new EmbeddedOSGiFrameworkLauncher().launch(new HashMap<String, Object>());
    }
    */

    public Object launch(Map<String, Object> configuration) throws ServerStartException, IOException {
        String jbossHomeKey = "jboss.home";
        String jbossHomeProp = System.getProperty(jbossHomeKey);
        if (jbossHomeProp == null)
            throw new IllegalStateException("Cannot find system property: " + jbossHomeKey);

        File jbossHomeDir = new File(jbossHomeProp).getAbsoluteFile();

        Properties sysprops = new Properties();
        sysprops.putAll(System.getProperties());
        sysprops.setProperty("jboss.home.dir", jbossHomeDir.getAbsolutePath());
        sysprops.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
        sysprops.setProperty("logging.configuration", "file:" + jbossHomeDir + "/standalone/configuration/logging.properties");
        sysprops.setProperty("org.jboss.boot.log.file", jbossHomeDir + "/standalone/log/boot.log");

        List<String> systemPackages = new ArrayList<String>();
        systemPackages.add("org.jboss.logmanager");

        // The OSGi packages have to be shared between the caller and the framework. Note that also some OSGi 4.3/5.0 packages
        // are included here because these are used by the resolver. When a package is picked up from the parent classloader
        // JBoss Modules also expects all subpackages to be picked up from the parent classloader, therefore these need to be
        // as well. This can probably be removed as soon as we support OSGi 4.3/5.0
        systemPackages.add("org.osgi.framework");
        systemPackages.add("org.osgi.framework.launch");
        systemPackages.add("org.osgi.framework.namespace");
        systemPackages.add("org.osgi.framework.wiring");
        systemPackages.add("org.osgi.resource");
        systemPackages.add("org.osgi.service.resolver");
        systemPackages.add("org.osgi.util.tracker");
        systemPackages.add("org.osgi.util.xml");

        Object spExtra = configuration.get(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA);
        StringBuilder sb = new StringBuilder();
        if (spExtra instanceof String) {
            sb.append(spExtra);
            sb.append(',');
        }

        for (String sp : systemPackages) {
            sb.append(sp);
            sb.append(',');
        }
        configuration.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, sb.toString());

        // Set the launcher properties as system properties. Not the best approach, but we cannot
        // use the DMR at this point yet.
        for (Map.Entry<String, Object> entry : configuration.entrySet()) {
            System.setProperty(entry.getKey(), "" + entry.getValue());
        }

        // Strip any attributes of the package information as only bare packages need to passed into create() below.
        List<String> sysPgsNoAttrs = new ArrayList<String>();
        for (String sp : sb.toString().split(",")) {
            int idx = sp.indexOf(';');
            if (idx >= 0)
                sysPgsNoAttrs.add(sp.substring(0, idx));
            else
                sysPgsNoAttrs.add(sp);
        }

        server = EmbeddedServerFactory.create(jbossHomeDir, sysprops, System.getenv(), sysPgsNoAttrs.toArray(new String [] {}));
        server.start();

        activateOSGiSubsystem(server);

        // ServiceName.toArray() does not produce the correct output for msc 1.0.2.GA.
        // This issue is already fixed on 1.0.3
        // return server.getService(10000, Services.FRAMEWORK_INIT.toArray());
        return server.getService(10000, "jbosgi", "framework", "INIT");
    }

    Object getService(long timeout, String ... nameSegments) {
        return server.getService(timeout, nameSegments);
    }

    public void stop() {
        server.stop();
    }

    /**
     * Uses the Management API to activate the OSGi subsystem.
     */
    private static void activateOSGiSubsystem(StandaloneServer server) throws IOException {
        ModelControllerClient controllerClient = server.getModelControllerClient();
        ModelNode activationOp = new ModelNode();
        ModelNode opList = activationOp.get("address").setEmptyList();
        opList.add("subsystem", "osgi");
        activationOp.get("operation").set("activate");
        controllerClient.execute(activationOp);
    }
}

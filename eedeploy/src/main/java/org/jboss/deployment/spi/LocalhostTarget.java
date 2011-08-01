/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.deployment.spi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.enterprise.deploy.shared.ModuleType;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.exceptions.TargetException;

import org.jboss.logging.Logger;

/**
 * A Target interface represents a single logical core server of one instance of a J2EE platform product. It is a designator for
 * a server and the implied location to copy a configured application for the server to access.
 *
 * @author thomas.diesler@jboss.org
 *
 */
public class LocalhostTarget implements JBossTarget {
    // deployment logging
    private static final Logger log = Logger.getLogger(LocalhostTarget.class);

    /**
     * Get the target's description
     *
     * @return the description
     */
    public String getDescription() {
        return "JBoss localhost deployment target";
    }

    /**
     * Get the target's name
     *
     * @return the name
     */
    public String getName() {
        return "localhost";
    }

    /**
     * Get the target's host name
     */
    public String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.error("Cannot obtain localhost", e);
            return null;
        }
    }

    /**
     * Deploy a given module
     */
    public void deploy(TargetModuleID targetModuleID) throws Exception {
        // get the server deploydir
        String deployDir = getDeploydir();

        FileOutputStream outs = null;
        FileInputStream ins = null;
        try {
            File deployableFile = new File(targetModuleID.getModuleID());
            File targetFile = new File(deployDir + "/" + deployableFile.getName());
            log.info("Writing deployableFile: " + deployableFile.getAbsolutePath() + " to: " + targetFile.getAbsolutePath());
            outs = new FileOutputStream(targetFile);
            ins = new FileInputStream(deployableFile);
            JarUtils.copyStream(outs, ins);
            log.info("Waiting 10 seconds for deploy to finish...");
            Thread.sleep(10000);
        } finally {
            try {
                if (outs != null) {
                    outs.close();
                }
                if (ins != null) {
                    ins.close();
                }
            } catch (IOException e) {
                // ignore this one
            }
        }
    }

    /**
     * Start a given module
     */
    public void start(TargetModuleID targetModuleID) throws Exception {
    }

    /**
     * Stop a given module
     */
    public void stop(TargetModuleID targetModuleID) throws Exception {
    }

    /**
     * Undeploy a given module
     */
    public void undeploy(TargetModuleID targetModuleID) throws Exception {
        // get the server deploydir
        String deployDir = getDeploydir();
        File file = new File(deployDir + "/" + targetModuleID.getModuleID());
        file.delete();
    }

    /**
     * Retrieve the list of all J2EE application modules running or not running on the identified targets.
     */
    public TargetModuleID[] getAvailableModules(ModuleType moduleType) throws TargetException {
        return null;
    }

    /**
     * Get the server deploydir
     */
    private String getDeploydir() {
        // [todo] replace this System property lookup
        String deployDir = System.getProperty("jboss.deploy.dir");
        if (deployDir == null) {
            String j2eeHome = System.getProperty("J2EE_HOME");
            if (j2eeHome == null)
                throw new RuntimeException("Cannot obtain system property: jboss.deploy.dir or J2EE_HOME");
            deployDir = j2eeHome + "/server/cts/deploy";
        }
        log.info("Using deploy dir: " + deployDir);
        return deployDir;
    }
}

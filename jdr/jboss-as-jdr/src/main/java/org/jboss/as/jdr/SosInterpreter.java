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
package org.jboss.as.jdr;

import static org.jboss.as.jdr.JdrLogger.ROOT_LOGGER;
import static org.jboss.as.jdr.JdrMessages.MESSAGES;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Date;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.python.util.PythonInterpreter;

/**
 * Wraps up the access to the jython interpreter to encapsulate its use
 * for running sosreport.
 *
 * @author Mike M. Clark
 * @author Jesse Jaggars
 */
public class SosInterpreter {

    private String jbossHomeDir = null;
    private String reportLocationDir = System.getProperty("user.dir");
    private String hostControllerName = null;
    private String serverName = null;
    private ModelControllerClient controllerClient = null;

    public SosInterpreter() {
        jbossHomeDir = System.getProperty("jboss.home.dir");
        if (jbossHomeDir == null) {
            jbossHomeDir = System.getenv("JBOSS_HOME");
        }
        ROOT_LOGGER.debug("JBoss Home directory: " + jbossHomeDir);
    }

    public JdrReport collect() throws OperationFailedException {
        return collect(null, null, null, null);
    }

    public JdrReport collect(String username, String password, String host, String port) throws OperationFailedException {
        ROOT_LOGGER.startingCollection();
        Date startTime = new Date();

        String homeDir = getJbossHomeDir();
        if (homeDir == null) {
            ROOT_LOGGER.jbossHomeNotSet();
            throw new OperationFailedException(MESSAGES.jbossHomeNotSet(),
                    new ModelNode().set(MESSAGES.jbossHomeNotSet()));
        }

        String pyLocation = getPythonScriptLocation();
        ROOT_LOGGER.debug("Location of standard JDR scripts: " + pyLocation);

        String locationDir = getReportLocationDir();

        ROOT_LOGGER.debug("locationDir = " + locationDir);
        ROOT_LOGGER.debug("homeDir = " + homeDir);

        String pathToReport = "";
        PythonInterpreter interpreter = new PythonInterpreter();

        try {
            SoSReport reporter = new SoSReport(interpreter, pyLocation, homeDir);
            reporter.setUsername(username);
            reporter.setPassword(password);
            reporter.setHostname(host);
            reporter.setPort(port);
            if (controllerClient != null) {
                reporter.setControllerClient(controllerClient);
            }
            reporter.setHostControllerName(hostControllerName);
            reporter.setServerName(serverName);
            reporter.setHome(homeDir);
            reporter.setTmpDir(locationDir);
            reporter.setCompressionType(CompressionType.ZIP);
            pathToReport = reporter.execute();

        } catch (Throwable t) {
            ROOT_LOGGER.pythonExceptionEncountered(t);
        } finally {
            interpreter.cleanup();
        }

        Date endTime = new Date();
        ROOT_LOGGER.endingCollection();

        JdrReport result = new JdrReport();
        result.setStartTime(startTime);
        result.setEndTime(endTime);
        result.setLocation(pathToReport);
        return result;
    }

    /**
     * Sets the location for where the report archive will be created.
     *
     * @param dir location of generated report archive
     */
    public void setReportLocationDir(String dir) {
        reportLocationDir = dir;
    }

    /**
     * Location for the generated report archive.  The default value
     * is the current working directory as specified in the <code>user.dir</code>
     * System property.
     *
     * @return location for the archive
     */
    public String getReportLocationDir() {
        return cleanPath(reportLocationDir);
    }

    public void setControllerClient(ModelControllerClient controllerClient) {
        this.controllerClient = controllerClient;
    }

    public void setHostControllerName(String hostControllerName) {
        this.hostControllerName = hostControllerName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    /**
     * Location of the JBoss distribution.
     *
     * @return JBoss home location.  If not set the value of the <code>jboss.home.dir</code>
     * System property is used.  If this value is not set, the current working directory,
     * as specified by the <code>user.dir</code> System property is used.
     */
    public String getJbossHomeDir() {
        if (jbossHomeDir == null) {
            jbossHomeDir = System.getenv("JBOSS_HOME");
        }
        return cleanPath(jbossHomeDir);
    }


    /**
     * Sets the root directory
     * @param jbossHomeDir
     * @throws IllegalArgumentException if <code>jbossHomeDir</code> is <code>null</code>.
     */
    public void setJbossHomeDir(String jbossHomeDir) throws IllegalArgumentException {
        if (jbossHomeDir == null) {
            throw MESSAGES.varNull("jbossHomeDir");
        }
        this.jbossHomeDir = jbossHomeDir;
    }

    /**
     * Splits a URL jar path to find the referenced jar location.
     *
     * @param path location of a resource in a jar file.
     * @return path location of the jar file containing the resource.
     */
    public static String getPath(String path) {
        return path.split(":", 2)[1].split("!")[0];
    }

    /**
     * Decodes a UTF-8 directory path obtained via a URL and "fixes" '\' characters
     * for proper quoting. This is to fix up windows paths. If an the path's encoding
     * is unsupported, a warning will be issued and separator replacement will proceed.
     *
     * @param path to be decoded
     * @return "cleaned" path
     */
    public static String cleanPath(String path) {
        try {
            path = URLDecoder.decode(path, "utf-8");
        } catch (UnsupportedEncodingException e) {
            ROOT_LOGGER.urlDecodeExceptionEncountered(e);
        }
        return path.replace("\\", "\\\\");
    }

    private String getPythonScriptLocation() {
        URL pyURL = this.getClass().getClassLoader().getResource("sos");
        String decodedPath = cleanPath(pyURL.getPath());
        return getPath(decodedPath);
    }
}

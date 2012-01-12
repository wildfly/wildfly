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

import static java.io.File.separator;
import static org.jboss.as.jdr.JdrLogger.ROOT_LOGGER;

import java.io.File;
import java.io.FileFilter;
import org.jboss.as.controller.client.ModelControllerClient;

import org.python.core.adapter.ClassicPyObjectAdapter;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.util.PythonInterpreter;

/**
 * Provides an encapsulated interface to the jython interpreter
 * for executing sosreport
 *
 * @author Jesse Jaggars
 * @author Mike M. Clark
 */
public class SoSReport {

    PyObject sosreport;
    PyObject sysPath;
    PythonInterpreter interpreter;
    private static final String SET_OPTION = "set_option";
    private static final String SET_GLOBAL = "set_global_plugin_option";

    /**
     * @param interpreter the jython interpreter instance to use
     * @param pyLocation the full path to the jar containing sosreport
     * @param jbossHomeDir the full path to JBOSS_HOME
     */
    public SoSReport(PythonInterpreter interpreter, String pyLocation, String jbossHomeDir) {
        this.interpreter = interpreter;

        interpreter.exec("from sys import path");
        sysPath = interpreter.get("path");

        addToJythonPath(pyLocation);

        interpreter.exec("from sos.sosreport import SoSReport");
        interpreter.exec("reporter = SoSReport([])");
        this.sosreport = interpreter.get("reporter");

        String contribPath = determineContribLocation(pyLocation);
        addContribScriptsToPath(contribPath);

        enableOption("--batch");
        enableOption("--report");
        enableOption("--silent");
        setHome(jbossHomeDir);
    }

    private void addToJythonPath(String location) {
        sysPath.invoke("append", new PyString(location));
    }

    /**
     * Determines the location of the contrib directory used
     * to add additional sosreport plugins.  The location is
     * determined by the jdr sosreport location (which are included
     * in a jar file). The contrib directory is expected to be in the
     * same directory
     *
     * @param pyLocation the jdr sos report scripts jar file path
     */
    private String determineContribLocation(String pyLocation) {
        int lastSeparatorIndex = pyLocation.lastIndexOf(separator);
        String contribLocation = pyLocation.substring(0, lastSeparatorIndex + 1) + "contrib";
        ROOT_LOGGER.debug("JDR plugin contrib directory location: " + contribLocation);
        return contribLocation;
    }

    private void addContribScriptsToPath(String contrib) {
        addToJythonPath(contrib);
        interpreter.exec("import sos.plugins");
        interpreter.exec("sos_path = sos.plugins.__path__");
        PyObject sosModulePath = interpreter.get("sos_path");
        sosModulePath.invoke("append", new PyString(contrib));
        addJarsToJythonPath(sosModulePath, contrib);
    }

    private void addJarsToJythonPath(PyObject sosModulePath, String contribPath) {
        File contrib = new File(contribPath);
        if (!contrib.exists()) {
            ROOT_LOGGER.debug("No plugin contrib directory found");
        } else if (!contrib.isDirectory()) {
            ROOT_LOGGER.contribNotADirectory();
        } else {
            File[] jarFiles = contrib.listFiles(new FileFilter() {

                @Override
                public boolean accept(File pathname) {
                    return pathname.getName().endsWith(".jar") || pathname.getName().endsWith(".zip");
                }
            });

            for (int i = 0; i < jarFiles.length; ++i) {
                String jarFile = jarFiles[i].getPath();
                ROOT_LOGGER.debug("Adding plugin contrib jar file to jython path: " + jarFile);
                sosModulePath.invoke("append", new PyString(jarFile));
            }
        }
    }

    /**
     * Executes sosreport and returns the path to the final archived report
     *
     * @return the full path to the report archive
     */
    public String execute() {
        return sosreport.invoke("execute").asString();
    }

    /**
     * Sets a configuration option for sosreport.
     *
     * @param name the name of the option
     * @param value the value of the option
     */
    public void setOption(String name, String value) {
        sosreport.invoke(SET_OPTION, new PyString(name), new PyString(value));
    }

    /**
     * Enables a flag-like option for sosreport.
     *
     * @param name the name of the option
     */
    public void enableOption(String name) {
        sosreport.invoke(SET_OPTION, new PyString(name));
    }

    /**
     * Sets a globally visible variable for use within sosreport.
     *
     * @param name the name of the variable
     * @param value the value of the variable, must be adaptable by Jython
     */
    public void setGlobal(String name, Object value) {
        if (value != null) {
            ClassicPyObjectAdapter adapter = new ClassicPyObjectAdapter();
            sosreport.invoke(SET_GLOBAL, new PyString(name), adapter.adapt(value));
        }
    }

    /**
     * Sets the method of compressing the report archive.
     *
     * @param type the type of compression to use
     */
    public void setCompressionType(CompressionType type) {
        setOption("--compression-type", type.toString());
    }

    /**
     * Sets the ModelControllerClient instance for sosreport to use.
     *
     * @param controllerClient the ModelControllerClient instance to use
     * */
    public void setControllerClient(ModelControllerClient controllerClient) {
        if (controllerClient != null ) {
            setGlobal("controller_client_proxy",
                    new ModelControllerClientProxy(controllerClient));
        }
        else {
            System.out.println("CONTROLLER CLIENT IS NULL!?");
        }
    }

    /**
     * Sets the temporary directory for sosreport to use.
     *
     * @param tmpDir the path to the temporary directory to use
     * */
    public void setTmpDir(String tmpDir) {
        setOption("--tmp-dir", tmpDir);
    }

    /**
     * Sets the management api username for sosreport to use
     *
     * @param username the management api username to use
     * */
    public void setUsername(String username) {
        setGlobal("as7_user", username);
    }

    /**
     * Sets the management api password for sosreport to use
     *
     * @param password the management api password to use
     * */
    public void setPassword(String password) {
        setGlobal("as7_pass", password);
    }

    /**
     * Sets the hostname for the management api that sosreport
     * should contact.
     *
     * @param hostname the hostname to use
     */
    public void setHostname(String hostname) {
        setGlobal("as7_host", hostname);
    }

    /**
     * Sets the port for the management api that sosreport should
     * contact.
     *
     * @param port the port to use
     * */
    public void setPort(String port) {
        setGlobal("as7_port", port);
    }

    /**
     * Sets JBOSS_HOME for sosreport to use
     *
     * @param homeDir the path to JBOSS_HOME
     * */
    public void setHome(String homeDir) {
        setGlobal("as7_home", homeDir);
    }

    /**
     * Sets the host controller name for sosreport to use
     * This is null in standalone mode
     *
     * @param hostControllerName the host controller name to use
     * */
    public void setHostControllerName(String hostControllerName) {
        setGlobal("as7_host_controller_name", hostControllerName);
    }

    /**
     * Sets the server instance name for sosreport to use
     * This is null in standalone mode
     *
     * @param serverName the server instance name to use
     * */
    public void setServerName(String serverName) {
        setGlobal("as7_server_name", serverName);
    }
}

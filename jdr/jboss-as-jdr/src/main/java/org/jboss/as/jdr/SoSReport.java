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
 */
public class SoSReport {

    PyObject sosreport;
    private static final String SET_OPTION = "set_option";
    private static final String SET_GLOBAL = "set_global_plugin_option";

    public SoSReport(PythonInterpreter interpreter, String pyLocation) {
        interpreter.exec("import sys");
        interpreter.exec("sys.path.append(\"" + pyLocation + "\")");
        interpreter.exec("from sos.sosreport import SoSReport");
        interpreter.exec("reporter = SoSReport([])");
        this.sosreport = interpreter.get("reporter");

        enableOption("--batch");
        enableOption("--report");
        enableOption("--silent");
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
    public void setClientController(ModelControllerClient controllerClient) {
        if (controllerClient != null ) {
            setGlobal("controller_client_proxy",
                    new ModelControllerClientProxy(controllerClient));
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
     * Sets the port for the management api that sosreport should
     * contact.
     *
     * @param port the port to use
     * */
    public void setPort(int port) {
        setGlobal("as7_port", Integer.valueOf(port).toString());
    }

    /**
     * Sets JBOSS_HOME for sosreport to use
     *
     * @param homeDir the path to JBOSS_HOME
     * */
    public void setHome(String homeDir) {
        setGlobal("as7_home", homeDir);
    }
}

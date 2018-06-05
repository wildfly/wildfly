/*
 * Copyright 2016 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  *
 * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.wildfly.test.integration.vdx.utils.server;

import org.wildfly.extras.creaper.core.offline.OfflineCommand;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public interface Server {

    String JBOSS_HOME = System.getProperty("jboss.home", "jboss-as");
    String SERVER_MODE = isDomain() ? "domain" : "standalone";

    String DEFAULT_SERVER_CONFIG = SERVER_MODE + ".xml"; // domain.xml or standalone.xml
    String DEFAULT_HOST_CONFIG = "host.xml";

    Path CONFIGURATION_PATH = Paths.get(JBOSS_HOME, SERVER_MODE, "configuration");
    String RESOURCES_DIRECTORY = "configurations" + File.separator + SERVER_MODE + File.separator;

    String LOGGING_PROPERTIES_FILE_NAME = "logging.properties";
    String ERRORS_LOG_FILE_NAME = "target/errors.log";

    /**
     * Starts the server. If @ServerConfig annotation is present on method in calling stacktrace (for example test method) then
     * it's applied before the server is started.
     *
     * Start of the server is expected to fail due to xml syntac error. It does not throw any exception when  tryStartAndWaitForFail of server fails.
     *
     * @throws Exception when something unexpected happens
     */
    void tryStartAndWaitForFail() throws Exception;

    void tryStartAndWaitForFail(OfflineCommand... offlineCommands) throws Exception;

    /**
     * Stops server.
     */
    void stop();

    /**
     *
     * @return true if started server is in domain mode. false if it's standalone mode.
     */
    static boolean isDomain() {
        return Boolean.parseBoolean(System.getProperty("domain", "false"));
    }

    Path getServerLogPath();

   /**
    * Reads file with ERROR log output from server start.
    * @return all error logs output
    * @throws Exception
    */
    String getErrorMessageFromServerStart() throws Exception;

    void setTestArchiveDirectory(Path testArchiveDirectory);
}

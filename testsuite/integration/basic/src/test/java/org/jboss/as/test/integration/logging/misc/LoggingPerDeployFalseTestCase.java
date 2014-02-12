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
package org.jboss.as.test.integration.logging.misc;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.logging.util.AbstractLoggingTest;
import org.jboss.as.test.integration.logging.util.LoggingBean;
import org.jboss.as.test.integration.logging.util.LoggingServlet;
import org.jboss.as.test.integration.management.base.AbstractMgmtServerSetupTask;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Petr Křemenský <pkremens@redhat.com>
 * @deprecated this needs to be moved/copied to the manualmode tests and the new use-deployment-logging-config attribute
 * needs to be used which requires a reload/restart of the server. Also I'm not sure the test actually works as it
 * passed when I broke the property. See WFLY-2648
 */

@ServerSetup(LoggingPerDeployFalseTestCase.LoggingPerDeployFalseTestCaseSetup.class)
@RunWith(Arquillian.class)
@Deprecated
public class LoggingPerDeployFalseTestCase extends AbstractLoggingTest {

    private static Logger log = Logger
            .getLogger(LoggingPerDeployFalseTestCase.class);

    private static final String LOG_FILE_NAME = "per-deploy-false-test.log";
    private static final String PER_DEPLOY_NAME = "jboss-logging-properties-test.log";
    private static File logFile;
    private static File perDeployLogFile;

    static class LoggingPerDeployFalseTestCaseSetup extends
            AbstractMgmtServerSetupTask {

        @Override
        protected void doSetup(ManagementClient managementClient)
                throws Exception {
            final List<ModelNode> updates = new ArrayList<ModelNode>();

            // prepare log files
            logFile = prepareLogFile(managementClient,
                    LOG_FILE_NAME);
            perDeployLogFile = prepareLogFile(managementClient,
                    PER_DEPLOY_NAME);

            // add custom file-handler
            ModelNode op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).add(SUBSYSTEM, "logging");
            op.get(OP_ADDR).add("periodic-rotating-file-handler",
                    "LOGGING_TEST");
            op.get("append").set("true");
            op.get("suffix").set(".yyyy-MM-dd");
            ModelNode file = new ModelNode();
            file.get("relative-to").set("jboss.server.log.dir");
            file.get("path").set(LOG_FILE_NAME);
            op.get("file").set(file);
            op.get("formatter").set("%d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%E%n");
            updates.add(op);

            // add handler to root-logger
            op = new ModelNode();
            op.get(OP).set("root-logger-assign-handler");
            op.get(OP_ADDR).add(SUBSYSTEM, "logging");
            op.get(OP_ADDR).add("root-logger", "ROOT");
            op.get("name").set("LOGGING_TEST");
            updates.add(op);

            // add "org.jboss.as.logging.per-deployment=false" system property
            op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).add("system-property",
                    "org.jboss.as.logging.per-deployment");
            op.get("value").set("false");
            updates.add(op);

            // we want all operations to perform
            for (ModelNode modelNode : updates) {
                try {
                    executeOperation(modelNode);
                } catch (MgmtOperationException exp) {
                    log.warn(exp.getMessage());
                }
            }
        }

        @Override
        public void tearDown(ManagementClient managementClient,
                             String containerId) throws Exception {
            final List<ModelNode> updates = new ArrayList<ModelNode>();

            // delete log files
            logFile.delete();
            perDeployLogFile.delete();

            // remove LOGGING_TEST from root-logger
            ModelNode op = new ModelNode();
            op.get(OP).set("root-logger-unassign-handler");
            op.get(OP_ADDR).add(SUBSYSTEM, "logging");
            op.get(OP_ADDR).add("root-logger", "ROOT");
            op.get("name").set("LOGGING_TEST");
            updates.add(op);

            // remove custom file handler
            op = new ModelNode();
            op.get(OP).set(REMOVE);
            op.get(OP_ADDR).add(SUBSYSTEM, "logging");
            op.get(OP_ADDR).add("periodic-rotating-file-handler",
                    "LOGGING_TEST");
            updates.add(op);

            // remove "org.jboss.as.logging.per-deployment=false" system
            // property
            op = new ModelNode();
            op.get(OP).set(REMOVE);
            op.get(OP_ADDR).add("system-property",
                    "org.jboss.as.logging.per-deployment");
            updates.add(op);

            // we want to perform all operations
            for (ModelNode modelNode : updates) {
                try {
                    executeOperation(modelNode);
                } catch (MgmtOperationException exp) {
                    log.warn(exp.getMessage());
                }
            }

        }

    }

    @ArquillianResource(LoggingServlet.class)
    URL url;

    @Deployment
    public static WebArchive createDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "logging.war");
        archive.addClasses(LoggingServlet.class);
        archive.addAsResource(LoggingBean.class.getPackage(),
                "jboss-logging.properties",
                "WEB-INF/classes/jboss-logging.properties");
        return archive;
    }

    @Test
    @RunAsClient
    @InSequence(1)
    public void defaultLoggingTest() throws IOException {
        // make some logs
        HttpURLConnection http = (HttpURLConnection) new URL(url, "Logger")
                .openConnection();
        int statusCode = http.getResponseCode();
        assertTrue("Invalid response statusCode: " + statusCode,
                statusCode == HttpServletResponse.SC_OK);
        // check logs
        BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(logFile), StandardCharsets.UTF_8));
        String line;
        boolean logFound = false;
        while ((line = br.readLine()) != null) {
            if (line.contains("LoggingServlet is logging")) {
                logFound = true;
                break;
            }
        }
        br.close();
        Assert.assertTrue(logFound);
    }

    @Test
    @RunAsClient
    @InSequence(2)
    public void perDeployFilePresenceTest() {
        Assert.assertFalse("File: " + perDeployLogFile.toString()
                + " should not be created!", perDeployLogFile.exists());
    }

}

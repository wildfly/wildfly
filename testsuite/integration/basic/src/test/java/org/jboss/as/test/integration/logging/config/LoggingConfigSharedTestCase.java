/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.logging.config;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.json.JsonObject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(AbstractConfigTestCase.LogFileServerSetupTask.class)
public class LoggingConfigSharedTestCase extends AbstractConfigTestCase {

    private static final String WAR_DEPLOYMENT_1 = "logging-war-1";
    private static final String WAR_DEPLOYMENT_2 = "logging-war-2";
    private static final String EJB_DEPLOYMENT = "logging-ejb";

    private static final String EJB_DEPLOYMENT_NAME = "logging-ejb.jar";
    private static final String WAR_DEPLOYMENT_1_NAME = "logging-war-1.war";
    private static final String WAR_DEPLOYMENT_2_NAME = "logging-war-2.war";
    private static final String FILE_NAME = "logging-config-shared-json.log";

    // This needs to be deployed first as the two other deployments rely on this
    @Deployment(name = EJB_DEPLOYMENT, order = 1)
    public static JavaArchive createEjbDeployment() {
        return ShrinkWrap.create(JavaArchive.class, EJB_DEPLOYMENT_NAME)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addClasses(LoggingStartup.class, LoggerResource.class);
    }

    // This should be deployed last to ensure that WAR_DEPLOYMENT_2 does register a log context
    @Deployment(name = WAR_DEPLOYMENT_1, order = 3)
    public static WebArchive createWar1() throws Exception {
        return ShrinkWrap.create(WebArchive.class, WAR_DEPLOYMENT_1_NAME)
                .addAsManifestResource(createLoggingConfiguration(FILE_NAME), "logging.properties")
                .addAsManifestResource(createJBossDeploymentStructure(), "jboss-deployment-structure.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addClasses(LoggingServlet.class);
    }

    @Deployment(name = WAR_DEPLOYMENT_2, order = 2)
    public static WebArchive createWar2() {
        return ShrinkWrap.create(WebArchive.class, WAR_DEPLOYMENT_2_NAME)
                .addAsManifestResource(createJBossDeploymentStructure(), "jboss-deployment-structure.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addClasses(LoggingServlet.class);
    }

    @Test
    public void testDeployments(@OperateOnDeployment(WAR_DEPLOYMENT_1) @ArquillianResource URL war1, @OperateOnDeployment(WAR_DEPLOYMENT_2) @ArquillianResource URL war2) throws Exception {
        // First invoke the first deployment to log
        final String msg1 = "Test from shared WAR1";
        UrlBuilder builder = UrlBuilder.of(war1, "log");
        builder.addParameter("msg", msg1);
        performCall(builder.build());

        // Next invoke the second deployment which should not use the log context from the first deployment
        final String msg2 = "Test from shared WAR2";
        builder = UrlBuilder.of(war2, "log");
        builder.addParameter("msg", msg2);
        performCall(builder.build());

        final List<JsonObject> depLogs = readJsonLogFile(FILE_NAME);

        // There should only be a log message from the servlet
        assertLength(depLogs, 1, FILE_NAME);

        // Check the expected dep log file
        Collection<JsonObject> unexpectedLogs = depLogs.stream()
                .filter(logMessage -> {
                    final String msg = logMessage.getString("message");
                    return (!msg.equals(LoggingServlet.formatMessage(msg1)));
                })
                .collect(Collectors.toList());
        assertUnexpectedLogs(unexpectedLogs, FILE_NAME);

        // Now we want to make sure that only WAR2 logs made it into the default log context
        final List<JsonObject> defaultLogs = readJsonLogFileFromModel(null, DEFAULT_LOG_FILE);

        // There should be 1 startup messages in this file, 3 from WAR2 servlet and 2 from the static logger in WAR1
        assertLength(defaultLogs, 6, DEFAULT_LOG_FILE);

        unexpectedLogs = defaultLogs.stream()
                .filter(logMessage -> {
                    final String msg = logMessage.getString("message");
                    return !msg.equals(LoggerResource.formatStaticLogMsg(msg1)) &&
                            !msg.equals(LoggerResource.formatLogMsg(msg1)) &&
                            !msg.equals(LoggingServlet.formatMessage(msg2)) &&
                            !msg.equals(LoggerResource.formatLogMsg(msg2)) &&
                            !msg.equals(LoggerResource.formatStaticLogMsg(msg2)) &&
                            !msg.equals(LoggingStartup.STARTUP_MESSAGE);
                })
                .collect(Collectors.toList());
        assertUnexpectedLogs(unexpectedLogs, DEFAULT_LOG_FILE);
    }

    private static Asset createJBossDeploymentStructure() {
        return new StringAsset(
                "<jboss-deployment-structure>\n" +
                        "   <deployment>\n" +
                        "       <dependencies>\n" +
                        "           <module name=\"deployment." + EJB_DEPLOYMENT_NAME + "\" meta-inf=\"import\" />\n" +
                        "       </dependencies>\n" +
                        "   </deployment>\n" +
                        "</jboss-deployment-structure>");
    }
}

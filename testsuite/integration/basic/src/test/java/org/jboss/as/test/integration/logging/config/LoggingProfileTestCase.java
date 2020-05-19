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

import static org.jboss.as.controller.client.helpers.Operations.CompositeOperationBuilder;

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
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(LoggingProfileTestCase.LoggingProfileSetupTask.class)
public class LoggingProfileTestCase extends AbstractConfigTestCase {

    private static final String PROFILE_NAME = "test-profile";

    private static final String EAR_DEPLOYMENT = "logging-profile-deployment-1";
    private static final String WAR_DEPLOYMENT = "logging-profile-deployment-2";

    private static final String EAR_DEPLOYMENT_NAME = "logging-profile-ear.ear";
    private static final String EJB_DEPLOYMENT_NAME = "logging-profile-ejb.jar";
    private static final String WAR_DEPLOYMENT_1_NAME = "logging-profile-war-1.war";
    private static final String WAR_DEPLOYMENT_2_NAME = "logging-profile-war-2.war";
    private static final String FILE_NAME = "logging-profile-json.log";

    @Deployment(name = EAR_DEPLOYMENT, order = 2)
    public static EnterpriseArchive createEar() {
        return ShrinkWrap.create(EnterpriseArchive.class, EAR_DEPLOYMENT_NAME)
                .addAsModules(
                        ShrinkWrap.create(WebArchive.class, WAR_DEPLOYMENT_1_NAME)
                                .addAsWebResource(new StringAsset("Logging-Profile: " + PROFILE_NAME), "META-INF/MANIFEST.MF")
                                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                                .addClasses(LoggingServlet.class),
                        ShrinkWrap.create(JavaArchive.class, EJB_DEPLOYMENT_NAME)
                                .addClasses(LoggingStartup.class, LoggerResource.class)
                );
    }

    @Deployment(name = WAR_DEPLOYMENT, order = 1)
    public static WebArchive createWar() {
        return ShrinkWrap.create(WebArchive.class, WAR_DEPLOYMENT_2_NAME)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addClasses(LoggingServlet.class, LoggerResource.class);
    }

    @Test
    public void testDeployments(@OperateOnDeployment(EAR_DEPLOYMENT) @ArquillianResource URL war1, @OperateOnDeployment(WAR_DEPLOYMENT) @ArquillianResource URL war2) throws Exception {
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

        final List<JsonObject> depLogs = readJsonLogFileFromModel(PROFILE_NAME, FILE_NAME);

        // There should be two log messages in the dependency logs; 1 from the servlet and 1 from the logger resource injected into the servlet
        assertLength(depLogs, 2, FILE_NAME);

        // Check the expected dep log file
        Collection<JsonObject> unexpectedLogs = depLogs.stream()
                .filter(logMessage -> {
                    final String msg = logMessage.getString("message");
                    return (!msg.equals(LoggingServlet.formatMessage(msg1)) &&
                            // This is the current behavior, but it seems incorrect. See WFCORE-4888 for details.
                            !(msg.equals(LoggerResource.formatLogMsg(msg1))));
                })
                .collect(Collectors.toList());
        assertUnexpectedLogs(unexpectedLogs, FILE_NAME);

        // Now we want to make sure that only WAR2 logs made it into the default log context
        final List<JsonObject> defaultLogs = readJsonLogFileFromModel(null, DEFAULT_LOG_FILE);

        // There should be 1 startup messages in this file, 3 from WAR2 servlet and 1 from the static logger in WAR1
        assertLength(defaultLogs, 5, DEFAULT_LOG_FILE);

        unexpectedLogs = defaultLogs.stream()
                .filter(logMessage -> {
                    final String msg = logMessage.getString("message");
                    return !msg.equals(LoggerResource.formatStaticLogMsg(msg1)) &&
                            !msg.equals(LoggingServlet.formatMessage(msg2)) &&
                            !msg.equals(LoggerResource.formatLogMsg(msg2)) &&
                            !msg.equals(LoggerResource.formatStaticLogMsg(msg2)) &&
                            !msg.equals(LoggingStartup.STARTUP_MESSAGE);
                })
                .collect(Collectors.toList());
        assertUnexpectedLogs(unexpectedLogs, DEFAULT_LOG_FILE);
    }

    public static class LoggingProfileSetupTask extends LogFileServerSetupTask {
        @Override
        protected CompositeOperationBuilder createBuilder() {
            final CompositeOperationBuilder builder = super.createBuilder();
            builder.addStep(Operations.createAddOperation(createSubsystemAddress(PROFILE_NAME)));
            addDefaults(builder, PROFILE_NAME, FILE_NAME);
            return builder;
        }
    }
}

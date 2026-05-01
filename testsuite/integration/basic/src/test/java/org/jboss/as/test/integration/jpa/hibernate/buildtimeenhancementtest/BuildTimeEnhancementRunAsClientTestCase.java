/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.hibernate.buildtimeenhancementtest;

import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Collections;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.jpa.hibernate.buildtimeenhancementtest.entityClasses.Employee;
import org.jboss.as.test.shared.logging.LoggingUtil;
import org.jboss.as.test.shared.TestLogHandlerSetupTask;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jboss.as.arquillian.api.ServerSetup;

/**
 * Verify that the Hibernate ORM build time bytecode enhancement Maven plugin works as expected.
 * - Verify that Hibernate ORM build time enhancement was run against the Employee class.
 * - Verify that the Hibernate ORM TRACE logger message for "HHH90009001" is logged to server.log indicating that
 * runtime bytecode enhancement was ignored since build time enhancement was already performed.
 * <p>
 * For more details on build time enhancement see:
 * - https://docs.hibernate.org/orm/6.6/userguide/html_single/#tooling-maven
 * - https://docs.hibernate.org/orm/7.4/userguide/html_single/#tooling-maven
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({BuildTimeEnhancementRunAsClientTestCase.TestLogHandlerSetup.class})
public class BuildTimeEnhancementRunAsClientTestCase {

    private static final String ARCHIVE_NAME = "jpa_BuildTimeEnhancementRunAsClientTestCase";
    private static final String TEST_HANDLER_NAME = "test-" + BuildTimeEnhancementRunAsClientTestCase.class.getSimpleName();
    private static final String TEST_LOG_FILE_NAME = TEST_HANDLER_NAME + ".log";

    public static class TestLogHandlerSetup extends TestLogHandlerSetupTask {

        @Override
        public Collection<String> getCategories() {
            return Collections.singletonList("org.hibernate");
        }
        @Override
        public String getLevel() {
            return "TRACE";
        }
        @Override
        public String getHandlerName() {
            return TEST_HANDLER_NAME;
        }
        @Override
        public String getLogFileName() {
            return TEST_LOG_FILE_NAME;
        }

    }

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(BuildTimeEnhancementRunAsClientTestCase.class,
                Employee.class,
                SFSB1.class
        );
        jar.addAsManifestResource(BuildTimeEnhancementRunAsClientTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        return jar;
    }

    @Test
    public void testHibernateBuildTimeEnhancement() {
        // Note: ManagedTypeHelper is an internal Hibernate ORM class, if it is removed or renamed then this test can be updated
        // accordingly.
        assertTrue("Employee class is expected to be be build time bytecode enhanced but wasn't (" +
                        org.hibernate.engine.internal.ManagedTypeHelper.isManagedType(Employee.class) + ")",
                org.hibernate.engine.internal.ManagedTypeHelper.isManagedType(Employee.class));
    }

    @ContainerResource
    private ManagementClient managementClient;

    /**
     * Ensure that Hibernate trace logging is enabled and validate that below "TRACE message HHH90009001" is logged by Hibernate for the
     * org.jboss.as.test.integration.jpa.hibernate.buildtimeenhancementtest.entityClasses.Employee class.  The logged message will be something like:
     * HHH90009001: Skipping enhancement of [org.jboss.as.test.integration.jpa.hibernate.buildtimeenhancementtest.entityClasses.Employee]: it's already annotated with @EnhancementInfo"
     */
    @Test
    public void testHibernateRuntimeEnhancementLogsTraceMessageHHH90009001() throws Exception {
        String expected = "Skipping enhancement of [org.jboss.as.test.integration.jpa.hibernate.buildtimeenhancementtest.entityClasses.Employee]: it's already annotated with @EnhancementInfo";
        assertTrue("Hibernate ORM Log message not found: " + expected,
                LoggingUtil.hasLogMessage(managementClient.getControllerClient(), TEST_HANDLER_NAME, expected));
    }

}

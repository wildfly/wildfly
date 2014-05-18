/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.ejb.timerservice.database;

import java.util.Date;

import javax.ejb.TimerConfig;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

/**
 * Tests that an @Timeout method is called when a timer is created programatically.
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
@ServerSetup(DatabaseTimerServiceTestCase.DatabaseTimerServiceTestCaseServerSetup.class)
public class DatabaseTimerServiceTestCase {

    private static int TIMER_INIT_TIME_MS = 100;
    private static int TIMER_TIMEOUT_TIME_MS = 100;

    private static String INFO_MSG_FOR_CHECK = "info";

    static class DatabaseTimerServiceTestCaseServerSetup implements ServerSetupTask {

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            ModelNode op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).add(SUBSYSTEM, "ejb3");
            op.get(OP_ADDR).add("service", "timer-service");
            op.get(OP_ADDR).add("database-data-store", "dbstore");
            op.get("datasource-jndi-name").set("java:jboss/datasources/ExampleDS");
            op.get("database").set("hsql");
            op.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            ModelNode op = new ModelNode();
            op.get(OP).set(REMOVE);
            op.get(OP_ADDR).add(SUBSYSTEM, "ejb3");
            op.get(OP_ADDR).add("service", "timer-service");
            op.get(OP_ADDR).add("database-data-store", "dbstore");
            op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
            op.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
        }
    }

    @Deployment
    public static Archive<?> deploy() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "testTimerServiceSimple.war");
        war.addPackage(DatabaseTimerServiceTestCase.class.getPackage());
        war.addAsWebInfResource(DatabaseTimerServiceTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        return war;
    }


    @Test
    public void testTimedObjectTimeoutMethod() throws NamingException {
        InitialContext ctx = new InitialContext();
        TimedObjectTimerServiceBean bean = (TimedObjectTimerServiceBean) ctx.lookup("java:module/" + TimedObjectTimerServiceBean.class.getSimpleName());
        bean.resetTimerServiceCalled();
        bean.getTimerService().createTimer(TIMER_TIMEOUT_TIME_MS, INFO_MSG_FOR_CHECK);
        Assert.assertTrue(TimedObjectTimerServiceBean.awaitTimerCall());

        bean.resetTimerServiceCalled();
        long ts = (new Date()).getTime() + TIMER_INIT_TIME_MS;
        TimerConfig timerConfig = new TimerConfig();
        timerConfig.setInfo(INFO_MSG_FOR_CHECK);
        bean.getTimerService().createSingleActionTimer(new Date(ts), timerConfig);
        Assert.assertTrue(TimedObjectTimerServiceBean.awaitTimerCall());

        Assert.assertEquals(INFO_MSG_FOR_CHECK, bean.getTimerInfo());
        Assert.assertFalse(bean.isCalendar());
        Assert.assertTrue(bean.isPersistent());
    }

}

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
package org.jboss.as.test.integration.ejb.timerservice.suspend;

import java.io.FilePermission;
import java.io.IOException;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.remoting3.security.RemotingPermission;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

/**
 * Tests suspend/resume for non-calendar based timers
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class TimerServiceSuspendTestCase {

    @ArquillianResource
    private ManagementClient managementClient;

    @Deployment
    public static Archive<?> deploy() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "testTimerServiceSimple.war");
        war.addPackage(TimerServiceSuspendTestCase.class.getPackage());
        war.addAsManifestResource(new StringAsset("Dependencies: org.jboss.dmr, org.jboss.as.controller-client, org.jboss.remoting3\n"), "MANIFEST.MF");
        war.addAsManifestResource(createPermissionsXmlAsset(
                new RemotingPermission("createEndpoint"),
                new RemotingPermission("connect"),
                new FilePermission(System.getProperty("jboss.inst") + "/standalone/tmp/auth/*", "read")
        ), "permissions.xml");
        return war;
    }

    @Test
    public void testSuspendWithIntervalTimer() throws NamingException, IOException, InterruptedException {
        SuspendTimerServiceBean.resetTimerServiceCalled();
        InitialContext ctx = new InitialContext();
        SuspendTimerServiceBean bean = (SuspendTimerServiceBean) ctx.lookup("java:module/" + SuspendTimerServiceBean.class.getSimpleName());


        ModelNode op = new ModelNode();
        Timer timer = null;
        try {
            try {
                timer = bean.getTimerService().createIntervalTimer(100, 100, new TimerConfig("", false));
                Assert.assertTrue(SuspendTimerServiceBean.awaitTimerServiceCount() > 0);

                op.get(ModelDescriptionConstants.OP).set("suspend");
                managementClient.getControllerClient().execute(op);

                SuspendTimerServiceBean.resetTimerServiceCalled();

                Thread.sleep(200);
                Assert.assertEquals(0, SuspendTimerServiceBean.getTimerServiceCount());

            } finally {
                op = new ModelNode();
                op.get(ModelDescriptionConstants.OP).set("resume");
                managementClient.getControllerClient().execute(op);

            }
            Assert.assertTrue(SuspendTimerServiceBean.awaitTimerServiceCount() > 0);
        } finally {
            if (timer != null) {
                timer.cancel();
                Thread.sleep(100);
            }
        }
    }

    /**
     * This test makes sure that interval timers that are scheduled while a timer is suspended do not back up, and only a single
     * run will occur when the container is resumed
     */
    @Test
    public void testIntervalTimersDoNotBackUp() throws NamingException, IOException, InterruptedException {
        SuspendTimerServiceBean.resetTimerServiceCalled();
        InitialContext ctx = new InitialContext();
        SuspendTimerServiceBean bean = (SuspendTimerServiceBean) ctx.lookup("java:module/" + SuspendTimerServiceBean.class.getSimpleName());
        Timer timer = null;
        try {
            long start = 0;
            ModelNode op = new ModelNode();
            try {

                op.get(ModelDescriptionConstants.OP).set("suspend");
                managementClient.getControllerClient().execute(op);

                //create the timer while the container is suspended
                start = System.currentTimeMillis();
                timer = bean.getTimerService().createIntervalTimer(100, 100, new TimerConfig("", false));
                Thread.sleep(5000);

                Assert.assertEquals(0, SuspendTimerServiceBean.getTimerServiceCount());

            } finally {
                op = new ModelNode();
                op.get(ModelDescriptionConstants.OP).set("resume");
                managementClient.getControllerClient().execute(op);
            }
            Thread.sleep(300); //if they were backed up we give them some time to run
            int timerServiceCount = SuspendTimerServiceBean.getTimerServiceCount();
            Assert.assertTrue("Interval " + (System.currentTimeMillis() - start) + " count " + timerServiceCount, timerServiceCount < 40);
        } finally {
            if (timer != null) {
                timer.cancel();
                Thread.sleep(100);
            }
        }
    }


    /**
     * Tests that a single action timer that executes when the container is suspended will run as normal once it is resumed
     */
    @Test
    public void testSingleActionTimerWhenSuspended() throws NamingException, IOException, InterruptedException {
        SuspendTimerServiceBean.resetTimerServiceCalled();
        InitialContext ctx = new InitialContext();
        SuspendTimerServiceBean bean = (SuspendTimerServiceBean) ctx.lookup("java:module/" + SuspendTimerServiceBean.class.getSimpleName());
        Timer timer = null;
        try {
            long start = 0;
            ModelNode op = new ModelNode();
            try {

                op.get(ModelDescriptionConstants.OP).set("suspend");
                managementClient.getControllerClient().execute(op);

                //create the timer while the container is suspended
                start = System.currentTimeMillis();
                timer = bean.getTimerService().createSingleActionTimer(1, new TimerConfig("", false));
                Thread.sleep(1000);

                Assert.assertEquals(0, SuspendTimerServiceBean.getTimerServiceCount());

            } finally {
                op = new ModelNode();
                op.get(ModelDescriptionConstants.OP).set("resume");
                managementClient.getControllerClient().execute(op);
            }
            Assert.assertEquals(1, SuspendTimerServiceBean.awaitTimerServiceCount());
        } finally {
            if (timer != null) {
                try {
                    timer.cancel();
                    Thread.sleep(100);
                } catch(Exception e) {
                    //as the timer has already expired this may throw an exception
                }
            }
        }
    }
}

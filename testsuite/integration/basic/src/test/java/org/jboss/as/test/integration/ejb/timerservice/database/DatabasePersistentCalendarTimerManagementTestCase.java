/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
 * 2110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.ejb.timerservice.database;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.ejb.timerservice.mgmt.AbstractTimerBean;
import org.jboss.as.test.integration.ejb.timerservice.mgmt.AbstractTimerManagementTestCase;
import org.jboss.as.test.integration.ejb.timerservice.mgmt.CalendarTimerBean;
import org.jboss.as.test.integration.ejb.timerservice.mgmt.SimpleFace;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.Serializable;

/**
 *
 * Test non persistent interval timer.
 *
 * @author: baranowb
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(DatabaseTimerServerSetup.class)
public class DatabasePersistentCalendarTimerManagementTestCase extends AbstractTimerManagementTestCase {


    @Deployment
    public static Archive createDeployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, getArchiveName());
        ejbJar.addClasses(AbstractTimerManagementTestCase.class, CalendarTimerBean.class, AbstractTimerBean.class, SimpleFace.class);
        ejbJar.addAsManifestResource(DatabasePersistentCalendarTimerManagementTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        return ejbJar;
    }

    @After
    public void clean() {
        super.clean();
    }

    @Before
    public void setup() throws Exception {
        super.setup();
    }

    @Test
    @InSequence(1)
    public void testResourceExistence() throws Exception {
        super.testResourceExistence();
    }

    @Test
    @InSequence(2)
    public void testSuspendAndActivate() throws Exception {
        super.testSuspendAndActivate();
    }

    @Test
    @InSequence(3)
    public void testCancel() throws Exception {
        super.testCancel();
    }

    @Test
    @InSequence(4)
    public void testTrigger() throws Exception {
        super.testTrigger();
    }

    @Test
    @InSequence(5)
    public void testSuspendAndTrigger() throws Exception {
        super.testSuspendAndTrigger();
    }

    protected String getBeanClassName() {
        return CalendarTimerBean.class.getSimpleName();
    }

    private Serializable info = "PersistentCalendarTimerCLITestCase";

    @Override
    protected Serializable getInfo() {
        return info;
    }

    @Override
    protected boolean isPersistent() {
        return true;
    }

    @Override
    protected void assertCalendar(final ModelNode timerDetails) {
        Assert.assertTrue("Calendar", this.isCalendarTimer(timerDetails));
    }
}

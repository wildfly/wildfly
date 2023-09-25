/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.timerservice.database;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.ejb.timerservice.mgmt.AbstractTimerBean;
import org.jboss.as.test.integration.ejb.timerservice.mgmt.AbstractTimerManagementTestCase;
import org.jboss.as.test.integration.ejb.timerservice.mgmt.IntervalTimerBean;
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
public class DatabasePersistentIntervalTimerManagementTestCase extends AbstractTimerManagementTestCase {

    @Deployment
    public static Archive createDeployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, getArchiveName());
        ejbJar.addClasses(AbstractTimerManagementTestCase.class, IntervalTimerBean.class, AbstractTimerBean.class, SimpleFace.class);
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
        return IntervalTimerBean.class.getSimpleName();
    }

    private Serializable info = "PersistentIntervalTimerCLITestCase";

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
        Assert.assertFalse("Calendar", this.isCalendarTimer(timerDetails));
    }
}

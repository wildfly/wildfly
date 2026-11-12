/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.timerservice.mgmt;

import java.io.Serializable;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * Test non persistent interval timer.
 *
 * @author: baranowb
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TimerManagementTestCase extends AbstractTimerManagementTestCase {

    @Deployment
    public static Archive createDeployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, getArchiveName());
        ejbJar.addClasses(AbstractTimerManagementTestCase.class, TimerBean.class, AbstractTimerBean.class, SimpleFace.class);
        return ejbJar;
    }

    @AfterEach
    public void clean() {
        super.clean();
    }

    @BeforeEach
    public void setup() throws Exception {
        super.setup();
    }

    @Test
    @Order(1)
    public void testResourceExistence() throws Exception {
        super.testResourceExistence();
        this.bean.waitOnTimeout();
        Assertions.assertEquals(1, this.bean.getTimerTicks(), "Timer ticks should register some ++!");

    }

    @Test
    @Order(2)
    public void testSuspendAndActivate() throws Exception {
        this.bean.createTimer();
        this.suspendTimer();
        final long ticksCount = this.bean.getTimerTicks();
        this.waitOverTimer();

        Assertions.assertEquals(ticksCount, this.bean.getTimerTicks(), "Timer ticks should not change after suspension!");
        this.activateTimer();
        this.bean.waitOnTimeout();
        Assertions.assertEquals(ticksCount + 1, this.bean.getTimerTicks(), "Timer ticks should register some ++!");
        try {
            getTimerDetails();
        } catch (OperationFailedException ofe) {
            final ModelNode failureDescription = ofe.getFailureDescription();
            assertThat(failureDescription.toString()).as("Wrong failure description").contains("WFLYCTL0216");
        }
    }

    @Test
    @Order(3)
    public void testCancel() throws Exception {
        this.bean.createTimer();
        this.cancelTimer();
        try {
            getTimerDetails();
        } catch (OperationFailedException ofe) {
            final ModelNode failureDescription = ofe.getFailureDescription();
            assertThat(failureDescription.toString()).as("Wrong failure description").contains("WFLYCTL0216");
        }
    }

    @Test
    @Order(4)
    public void testWildcard() throws Exception {
        assertNoTimers();
        this.bean.createTimer();
        final PathAddress address = PathAddress.pathAddress(getTimerAddressBase(), PathElement.pathElement("timer", "*"));
        final ModelNode operation = Util.createOperation("suspend", address);
        ModelNode response = executeForResult(operation);
        Assertions.assertFalse(Operations.isSuccessfulOutcome(response));
        assertThat(Operations.getFailureDescription(response).toString()).as("Wrong failure description").contains("WFLYEJB0526");
        this.waitOverTimer();
    }

    protected String getBeanClassName() {
        return TimerBean.class.getSimpleName();
    }

    private Serializable info = "PersistentIntervalTimerCLITestCase";

    @Override
    protected Serializable getInfo() {
        return info;
    }

    @Override
    protected boolean isPersistent() {
        // its persistent by default
        return true;
    }

    @Override
    protected void assertCalendar(final ModelNode timerDetails) {
        Assertions.assertFalse(this.isCalendarTimer(timerDetails), "Calendar");
    }
}

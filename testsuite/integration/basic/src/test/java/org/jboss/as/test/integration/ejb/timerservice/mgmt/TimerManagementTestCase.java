package org.jboss.as.test.integration.ejb.timerservice.mgmt;

import java.io.Serializable;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.containsString;

/**
 *
 * Test non persistent interval timer.
 *
 * @author: baranowb
 */
@RunWith(Arquillian.class)
@RunAsClient
public class TimerManagementTestCase extends AbstractTimerManagementTestCase {

    @Deployment
    public static Archive createDeployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, getArchiveName());
        ejbJar.addClasses(AbstractTimerManagementTestCase.class, TimerBean.class, AbstractTimerBean.class, SimpleFace.class);
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
        this.bean.waitOnTimeout();
        Assert.assertEquals("Timer ticks should register some ++!", 1, this.bean.getTimerTicks());

    }

    @Test
    @InSequence(2)
    public void testSuspendAndActivate() throws Exception {
        this.bean.createTimer();
        this.suspendTimer();
        final long ticksCount = this.bean.getTimerTicks();
        this.waitOverTimer();

        Assert.assertEquals("Timer ticks should not change after suspension!", ticksCount, this.bean.getTimerTicks());
        this.activateTimer();
        this.bean.waitOnTimeout();
        Assert.assertEquals("Timer ticks should register some ++!", ticksCount + 1, this.bean.getTimerTicks());
        try {
            getTimerDetails();
        } catch (OperationFailedException ofe) {
            final ModelNode failureDescription = ofe.getFailureDescription();
            Assert.assertThat("Wrong failure description", failureDescription.toString(), containsString("WFLYCTL0216"));
        }
    }

    @Test
    @InSequence(3)
    public void testCancel() throws Exception {
        this.bean.createTimer();
        this.cancelTimer();
        try {
            getTimerDetails();
        } catch (OperationFailedException ofe) {
            final ModelNode failureDescription = ofe.getFailureDescription();
            Assert.assertThat("Wrong failure description", failureDescription.toString(), containsString("WFLYCTL0216"));
        }
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
        Assert.assertFalse("Calendar", this.isCalendarTimer(timerDetails));
    }
}

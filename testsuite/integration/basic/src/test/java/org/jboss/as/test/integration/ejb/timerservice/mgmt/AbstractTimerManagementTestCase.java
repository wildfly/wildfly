package org.jboss.as.test.integration.ejb.timerservice.mgmt;

import java.io.IOException;
import java.io.Serializable;
import java.util.Hashtable;
import java.util.Set;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.Assert;

/**
 *
 * Base test class for timer mgmt operations
 *
 * @author: baranowb
 */
public abstract class AbstractTimerManagementTestCase {
    @ContainerResource
    protected ManagementClient managementClient;
    private static final String APP_NAME = "ejb-mgmt-timers";

    protected SimpleFace bean;
    protected PathAddress timerAddress;
    protected String timerId;

    public static String getArchiveName() {
        return APP_NAME + ".jar";
    }

    public void clean() {
        if (bean != null) {
            try {
                bean.clean();
            } catch (Exception e) {
            }
        }
        this.timerId = null;
        this.bean = null;
    }

    public void setup() throws Exception {
        this.lookupBean();
        this.bean.setPersistent(isPersistent());
        this.bean.setInfo(getInfo());
        this.bean.setDelay(getDelay());
    }

    protected void lookupBean() throws Exception {
        final Hashtable<String, String> jndiProperties = new Hashtable<String, String>();
        jndiProperties.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.as.naming.InitialContextFactory");
        jndiProperties.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        final Context context = new InitialContext(jndiProperties);
        this.bean = (SimpleFace) context.lookup("ejb:/" + APP_NAME + "/" + getBeanClassName()
                + "!org.jboss.as.test.integration.ejb.timerservice.mgmt.SimpleFace");
    }

    protected String getBeanClassName() {
        return IntervalTimerBean.class.getSimpleName();
    }

    public void testResourceExistence() throws Exception {
        assertNoTimers();
        this.bean.createTimer();
        ModelNode timerDetails = getTimerDetails();
        Assert.assertEquals("Persistent", this.isPersistent(), this.isPersistent(timerDetails));
        Assert.assertEquals("Serializable Info", this.getInfo(), this.getInfo(timerDetails));
        Assert.assertTrue("Active", this.isActive(timerDetails));
        assertCalendar(timerDetails);

        final int left = this.getTimeRemaining(timerDetails);
        final int to = getDelay();
        Assert.assertTrue("Not enough time? '"+left+"'<='"+to+"'", left <= to);
        if(this.isCalendarTimer(timerDetails)){
            final ModelNode schedule = timerDetails.get("schedule");
            checkCalendardSchedule("year", "*", true, schedule);
            checkCalendardSchedule("month", "*", true, schedule);
            checkCalendardSchedule("day-of-month", "*", true, schedule);
            checkCalendardSchedule("day-of-week", "*", true, schedule);
            checkCalendardSchedule("hour", "*", true, schedule);
            checkCalendardSchedule("minute", "*", true, schedule);
            checkCalendardSchedule("second", getCalendarTimerDetail(), true, schedule);

            checkCalendardSchedule("timezone", null, false, schedule);
            checkCalendardSchedule("start", null, false, schedule);
            checkCalendardSchedule("end", null, false, schedule);
        }
    }

    public void testSuspendAndActivate() throws Exception {
        assertNoTimers();
        this.bean.createTimer();
        this.bean.waitOnTimeout();
        this.suspendTimer();
        final long ticksCount = this.bean.getTimerTicks();
        this.waitOverTimer();

        Assert.assertEquals("Timer ticks should not change after suspension!", ticksCount, this.bean.getTimerTicks());
        this.activateTimer();
        this.bean.waitOnTimeout();
    }

    public void testCancel() throws Exception {
        assertNoTimers();
        this.bean.createTimer();
        this.bean.waitOnTimeout();
        getTimerDetails();
        this.cancelTimer();
        try {
            getTimerDetails();
        } catch (OperationFailedException ofe) {
            final ModelNode failureDescription = ofe.getFailureDescription();
            Assert.assertTrue(failureDescription.toString(), failureDescription.toString().contains("WFLYCTL0216"));
        }
    }

    public void testTrigger() throws Exception {
        assertNoTimers();
        this.bean.createTimer();
        Assert.assertEquals("Wrong initial timer ticks!", 0, this.bean.getTimerTicks());
        triggerTimer();
        Assert.assertEquals("Wrong after trigger timer ticks!", 1, this.bean.getTimerTicks());
        this.bean.waitOnTimeout();
        Assert.assertEquals("Timer should fire twice!", 2, this.bean.getTimerTicks());
    }

    public void testSuspendAndTrigger() throws Exception {
        assertNoTimers();
        this.bean.createTimer();
        Assert.assertEquals("Wrong initial timer ticks!", 0, this.bean.getTimerTicks());
        triggerTimer();
        this.suspendTimer();
        int ticksCount = this.bean.getTimerTicks();
        Assert.assertTrue("Timer should fire at least once!", ticksCount >= 1);
        this.waitOverTimer();
        Assert.assertEquals("The tick count should not increase while the timer was suspended!",
                ticksCount, this.bean.getTimerTicks());
        this.activateTimer();
        this.bean.waitOnTimeout();
        Assert.assertTrue("Number of ticks should increase after timer activation!",
                this.bean.getTimerTicks() > ticksCount);
    }

    protected void suspendTimer() throws Exception {
        final PathAddress address = getTimerAddress();
        final ModelNode operation = Util.createOperation("suspend", address);
        executeForResult(operation, true);
    }

    protected void activateTimer() throws Exception {
        final PathAddress address = getTimerAddress();
        final ModelNode operation = Util.createOperation("activate", address);
        executeForResult(operation, true);
    }

    protected void triggerTimer() throws Exception {
        final PathAddress address = getTimerAddress();
        final ModelNode operation = Util.createOperation("trigger", address);
        executeForResult(operation, true);
    }

    protected void cancelTimer() throws Exception {
        final PathAddress address = getTimerAddress();
        final ModelNode operation = Util.createOperation("cancel", address);
        executeForResult(operation, true);
    }

    protected ModelNode getTimerDetails() throws Exception {
        final PathAddress address = getTimerAddress();
        final ModelNode operation = Util.createOperation("read-resource", address);
        operation.get(ModelDescriptionConstants.INCLUDE_RUNTIME).set(Boolean.toString(true));
        return executeForResult(operation, false);
    }

    protected PathAddress getTimerAddress() throws Exception {
        if (this.timerAddress != null) {
            return this.timerAddress;
        }
        final PathAddress address = PathAddress.pathAddress(
                PathElement.pathElement(ModelDescriptionConstants.DEPLOYMENT, APP_NAME + ".jar"),
                PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, "ejb3"),
                PathElement.pathElement("stateless-session-bean", getBeanClassName()),
                PathElement.pathElement("service", "timer-service"));
        final ModelNode operation = Util.createOperation("read-resource", address);
        operation.get(ModelDescriptionConstants.INCLUDE_RUNTIME).set(true);
        final ModelNode result = managementClient.getControllerClient().execute(operation);

        Assert.assertEquals(result.toString(), ModelDescriptionConstants.SUCCESS, result.get(ModelDescriptionConstants.OUTCOME)
                .asString());
        final ModelNode tmp = result.get("result").get("timer");
        final Set<String> lst = tmp.keys();
        Assert.assertEquals(1, lst.size());
        this.timerId = lst.iterator().next();
        this.timerAddress = PathAddress.pathAddress(address, PathElement.pathElement("timer", this.timerId));
        return this.timerAddress;
    }

    protected boolean isActive(final ModelNode timerDetails) {
        return timerDetails.get("active").asString().equalsIgnoreCase("true");
    }

    protected boolean isCalendarTimer(final ModelNode timerDetails) {
        return timerDetails.get("calendar-timer").asString().equalsIgnoreCase("true");
    }

    protected boolean isPersistent(final ModelNode timerDetails) {
        return timerDetails.get("persistent").asString().equalsIgnoreCase("true");
    }

    protected Serializable getInfo(final ModelNode timerDetails) {
        // TODO this is just wrong
        return timerDetails.get("info").asString();
    }

    protected int getTimeRemaining(final ModelNode timerDetails) {
        return timerDetails.get("time-remaining").asInt();
    }

    protected ModelNode getSchedule(final ModelNode timerDetails) {
        return timerDetails.get("schedule");
    }

    protected void checkCalendardSchedule(final String name, final String expected, final boolean mustBeDefined, final ModelNode schedule ){
        final ModelNode target = schedule.get(name);
        if(mustBeDefined){
            Assert.assertEquals("The '"+name+"' has wrong value!",expected, target.asString());
        } else {
            Assert.assertEquals("The '"+name+"' should be undefined!",ModelType.UNDEFINED, target.getType());
        }
    }

    protected int getDelay() {
        return 3000;
    }

    protected void waitOverTimer() throws Exception {
        Thread.currentThread().sleep(this.getDelay() + 1000);
    }

    protected void assertNoTimers() {
        Assert.assertEquals("No timers should be present!", 0, this.bean.getTimerCount());
    }

    protected abstract Serializable getInfo();

    protected abstract boolean isPersistent();

    protected abstract void assertCalendar(final ModelNode timerDetails);

    protected String getCalendarTimerDetail(){
        return this.bean.getComparableTimerDetail();
    }

    private ModelNode executeForResult(ModelNode operation, boolean useOpForFailureMsg) throws OperationFailedException, IOException {
        final ModelNode response = this.managementClient.getControllerClient().execute(operation);
        if (!Operations.isSuccessfulOutcome(response)) {
            if (useOpForFailureMsg) {
                throw new OperationFailedException("Failed executing " + operation.toString());
            } else {
                throw new OperationFailedException(response.asString());
            }
        }
        return response.get(ModelDescriptionConstants.RESULT);
    }
}

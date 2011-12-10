/*
 *
 *  * JBoss, Home of Professional Open Source.
 *  * Copyright 2011, Red Hat, Inc., and individual contributors
 *  * as indicated by the @author tags. See the copyright.txt file in the
 *  * distribution for a full listing of individual contributors.
 *  *
 *  * This is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU Lesser General Public License as
 *  * published by the Free Software Foundation; either version 2.1 of
 *  * the License, or (at your option) any later version.
 *  *
 *  * This software is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  * Lesser General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public
 *  * License along with this software; if not, write to the Free
 *  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */

package org.jboss.as.ejb3;

import java.io.File;
import java.lang.reflect.Method;

import javax.ejb.Timer;

import org.jboss.as.ejb3.component.entity.EntityBeanComponentInstance;
import org.jboss.as.ejb3.component.stateful.StatefulSessionComponentInstance;
import org.jboss.as.ejb3.timerservice.CalendarTimer;
import org.jboss.as.ejb3.timerservice.TimerImpl;
import org.jboss.as.ejb3.timerservice.spi.MultiTimeoutMethodTimedObjectInvoker;
import org.jboss.ejb.client.EJBLocator;
import org.jboss.ejb.client.SessionID;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * Date: 19.10.2011
 *
 * @author <a href="mailto:Flemming.Harms@gmail.com">Flemming Harms</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface EjbLogger extends BasicLogger {

    /**
     * Default root level logger with the package name for he category.
     */
    EjbLogger ROOT_LOGGER = Logger.getMessageLogger(EjbLogger.class, EjbLogger.class.getPackage().getName());

    EjbLogger EJB3_LOGGER = Logger.getMessageLogger(EjbLogger.class, "org.jboss.ejb3");

    /**
     * logger use to log EJB invocation eorrors
     */
    EjbLogger EJB3_INVOCATION_LOGGER = Logger.getMessageLogger(EjbLogger.class, "org.jboss.ejb3.invocation");

    /**
     * Logs an error message indicating an exception occurred while removing the an inactive bean.
     *
     * @param id    the session id that could not be removed
     * @param cause the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 14100, value = "Exception removing stateful bean %s")
    void errorRemovingStatefulBean(SessionID id, @Cause Throwable cause);

    /**
     * Logs an warning message indicating the it could not find a EJB for the specific id
     *
     * @param id the session id that could not be released
     */
    @LogMessage(level = TRACE)
    @Message(id = 14101, value = "Could not find stateful bean to release %s")
    void couldNotFindStatefulBean(SessionID id);

    /**
     * Logs an error message indicating an exception occurred while executing an invocation
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 14102, value = "Asynchronous invocation failed")
    void asyncInvocationFailed(@Cause Throwable cause);

    /**
     * Logs an error message indicating an exception occurred while getting status
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 14103, value = "failed to get tx manager status; ignoring")
    void getTxManagerStatusFailed(@Cause Throwable cause);

    /**
     * Logs an error message indicating an exception occurred while calling setRollBackOnly
     *
     * @param se the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 14104, value = "failed to set rollback only; ignoring")
    void setRollbackOnlyFailed(@Cause Throwable se);

    /**
     * Logs an warning message indicating ActivationConfigProperty will be ignored since it is not allowed by resource adapter
     *
     * @param propName
     * @param resourceAdapterName
     */
    @LogMessage(level = WARN)
    @Message(id = 14105, value = "ActivationConfigProperty %s will be ignored since it is not allowed by resource adapter: %s")
    void activationConfigPropertyIgnored(Object propName, String resourceAdapterName);

    /**
     * Logs an error message indicating Discarding stateful component instance due to exception
     *
     * @param component the discarded instance
     * @param t         the cause of error
     */
    @LogMessage(level = ERROR)
    @Message(id = 14106, value = "Discarding stateful component instance: %s due to exception")
    void discardingStatefulComponent(StatefulSessionComponentInstance component, @Cause Throwable t);

    /**
     * Logs an error message indicating it failed to remove bean with the specified session id
     *
     * @param componentName
     * @param sessionId
     * @param t             the cause of error
     */
    @LogMessage(level = ERROR)
    @Message(id = 14107, value = "Failed to remove bean: %s with session id %s")
    void failedToRemoveBean(String componentName, SessionID sessionId, @Cause Throwable t);

    /**
     * Logs an info message indicating it could not find stateful session bean instance with id
     *
     * @param sessionId     the id of the session bean
     * @param componentName
     */
    @LogMessage(level = INFO)
    @Message(id = 14108, value = "Could not find stateful session bean instance with id: %s for bean: %s during destruction. Probably already removed")
    void failToFindSfsbWithId(SessionID sessionId, String componentName);

    /**
     * Logs an warning message indicating Default interceptor class is not listed in the <interceptors> section of ejb-jar.xml and will not be applied"
     */
    @LogMessage(level = WARN)
    @Message(id = 14110, value = "Default interceptor class %s is not listed in the <interceptors> section of ejb-jar.xml and will not be applied")
    void defaultInterceptorClassNotListed(String clazz);

    /**
     * Logs an warning message indicating No method found on EJB while processing exclude-list element in ejb-jar.xml
     */
    @LogMessage(level = WARN)
    @Message(id = 14111, value = "No method named: %s found on EJB: %s while processing exclude-list element in ejb-jar.xml")
    void noMethodFoundOnEjbExcludeList(String methodName, String ejbName);

    /**
     * Logs an warning message indicating No method with param types found on EJB while processing exclude-list element in ejb-jar.xml
     */
    @LogMessage(level = WARN)
    @Message(id = 14112, value = "No method named: %s with param types: %s found on EJB: %s while processing exclude-list element in ejb-jar.xml")
    void noMethodFoundOnEjbWithParamExcludeList(String methodName, String s, String ejbName);

    /**
     * Logs an warning message indicating no method named found on EJB while processing method-permission element in ejb-jar.xml
     */
    @LogMessage(level = WARN)
    @Message(id = 14113, value = "No method named: %s found on EJB: %s while processing method-permission element in ejb-jar.xml")
    void noMethodFoundOnEjbPermission(String methodName, String ejbName);

    /**
     * Logs an warning message indicating No method with param type found on EJB while processing method-permission element in ejb-jar.xml
     */
    @LogMessage(level = WARN)
    @Message(id = 14114, value = "No method named: %s with param types: %s found on EJB: %s while processing method-permission element in ejb-jar.xml")
    void noMethodFoundWithParamOnEjbMethodPermission(String methodName, String s, String ejbName);


    /**
     * Logs an warning message indicating Unknown timezone id found in schedule expression. Ignoring it and using server's timezone
     */
    @LogMessage(level = WARN)
    @Message(id = 14115, value = "Unknown timezone id: %s found in schedule expression. Ignoring it and using server's timezone: %s")
    void unknownTimezoneId(String timezoneId, String id);

    /**
     * Logs an warning message indicating the timer persistence is not enabled, persistent timers will not survive JVM restarts
     */
    @LogMessage(level = WARN)
    @Message(id = 14116, value = "Timer persistence is not enabled, persistent timers will not survive JVM restarts")
    void timerPersistenceNotEnable();

    /**
     * Logs an info message indicating the next expiration is null. No tasks will be scheduled for timer
     */
    @LogMessage(level = INFO)
    @Message(id = 14117, value = "Next expiration is null. No tasks will be scheduled for timer %S")
    void nextExpirationIsNull(TimerImpl timer);

    /**
     * Logs an error message indicating Ignoring exception during setRollbackOnly
     */
    @LogMessage(level = ERROR)
    @Message(id = 14118, value = "Ignoring exception during setRollbackOnly")
    void ignoringException(@Cause Throwable e);

    /**
     * Logs an warning message indicating the unregistered an already registered Timerservice with id %s and a new instance will be registered
     */
    @LogMessage(level = WARN)
    @Message(id = 14119, value = "Unregistered an already registered Timerservice with id %s and a new instance will be registered")
    void UnregisteredRegisteredTimerService(String timedObjectId);

    /**
     * Logs an error message indicating an error invoking timeout for timer
     */
    @LogMessage(level = ERROR)
    @Message(id = 14120, value = "Error invoking timeout for timer: %s")
    void errorInvokeTimeout(Timer timer, @Cause Throwable e);

    /**
     * Logs an info message indicating timer will be retried
     */
    @LogMessage(level = INFO)
    @Message(id = 14121, value = "Timer: %s will be retried")
    void timerRetried(Timer timer);

    /**
     * Logs an error message indicating an error during retyring timeout for timer
     */
    @LogMessage(level = ERROR)
    @Message(id = 14122, value = "Error during retyring timeout for timer: %s")
    void errorDuringRetryTimeout(Timer timer, @Cause Throwable e);

    /**
     * Logs an info message indicating retrying timeout for timer
     */
    @LogMessage(level = INFO)
    @Message(id = 14123, value = "Retrying timeout for timer: %s")
    void retryingTimeout(Timer timer);

    /**
     * Logs an info message indicating timer is not active, skipping retry of timer
     */
    @LogMessage(level = INFO)
    @Message(id = 14124, value = "Timer is not active, skipping retry of timer: %s")
    void timerNotActive(Timer timer);

    /**
     * Logs an error message indicating Cannot invoke timeout method because timer is an auto timer,
     * but invoker is not of type specified
     */
    @LogMessage(level = ERROR)
    @Message(id = 14125, value = "Cannot invoke timeout method because timer: %s is an auto timer, but invoker is not of type %s")
    void failToInvokeTimeout(CalendarTimer calendarTimer, Class<MultiTimeoutMethodTimedObjectInvoker> multiTimeoutMethodTimedObjectInvokerClass);

    /**
     * Logs an warning message indicating could not read timer information for EJB component
     */
    @LogMessage(level = WARN)
    @Message(id = 14126, value = "Could not read timer information for EJB component %s")
    void failToReadTimerInformation(String componentName);

    /**
     * Logs an error message indicating it could not remove persistent timer
     */
    @LogMessage(level = ERROR)
    @Message(id = 14127, value = "Could not remove persistent timer %s")
    void failedToRemovePersistentTimer(File file);

    /**
     * Logs an error message indicating it's not a directory, could not restore timers"
     */
    @LogMessage(level = ERROR)
    @Message(id = 14128, value = "%s is not a directory, could not restore timers")
    void failToRestoreTimers(File file);

    /**
     * Logs an error message indicating it could not restore timer from file
     */
    @LogMessage(level = ERROR)
    @Message(id = 14129, value = "Could not restore timer from %s")
    void failToRestoreTimersFromFile(File timerFile, @Cause Throwable e);

    /**
     * Logs an error message indicating error closing file
     */
    @LogMessage(level = ERROR)
    @Message(id = 14130, value = "error closing file ")
    void failToCloseFile(@Cause Throwable e);

    /**
     * Logs an error message indicating Could not restore timers for specified id
     */
    @LogMessage(level = ERROR)
    @Message(id = 14131, value = "Could not restore timers for %s")
    void failToRestoreTimersForObjectId(String timedObjectId, @Cause Throwable e);

    /**
     * Logs an error message indicating Could not restore timers for specified id
     */
    @LogMessage(level = ERROR)
    @Message(id = 14132, value = "Could not create directory %s to persist EJB timers.")
    void failToCreateDirectoryForPersistTimers(File file);

    /**
     * Logs an error message indicating it discarding entity component instance
     */
    @LogMessage(level = ERROR)
    @Message(id = 14133, value = "Discarding entity component instance: %s due to exception")
    void discardingEntityComponent(EntityBeanComponentInstance instance, @Cause Throwable t);

    /**
     * Logs an error message indicating that an invocation failred
     */
    @LogMessage(level = ERROR)
    @Message(id = 14134, value = "EJB Invocation failed on component %s for method %s")
    void invocationFailed(String component, Method method, @Cause Throwable t);

    /**
     * Logs an error message indicating that an ejb client proxy could not be swapped out in a RMI invocation
     */
    @LogMessage(level = WARN)
    @Message(id = 14135, value = "Could not find EJB for locator %s, EJB client proxy will not be replaced")
    void couldNotFindEjbForLocatorIIOP(EJBLocator locator);


    /**
     * Logs an error message indicating that an ejb client proxy could not be swapped out in a RMI invocation
     */
    @LogMessage(level = WARN)
    @Message(id = 14136, value = "EJB %s is not being replaced with a Stub as it is not exposed over IIOP")
    void ejbNotExposedOverIIOP(EJBLocator locator);


    /**
     * Logs an error message indicating that dynamic stub creation failed
     */
    @LogMessage(level = ERROR)
    @Message(id = 14137, value = "Dynamic stub creation failed for class %s")
    void dynamicStubCreationFailed(String clazz, @Cause Throwable t);


    /**
     */
    @LogMessage(level = ERROR)
    @Message(id = 14138, value = "Exception releasing entity")
    void exceptionReleasingEntity(@Cause Throwable t);

}

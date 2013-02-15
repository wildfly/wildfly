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

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.io.File;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.Date;

import javax.ejb.Timer;

import org.jboss.as.ejb3.component.entity.EntityBeanComponentInstance;
import org.jboss.as.ejb3.component.stateful.StatefulSessionComponentInstance;
import org.jboss.as.ejb3.deployment.DeploymentModuleIdentifier;
import org.jboss.as.ejb3.subsystem.deployment.InstalledComponent;
import org.jboss.as.ejb3.timerservice.TimerImpl;
import org.jboss.ejb.client.EJBLocator;
import org.jboss.ejb.client.SessionID;
import org.jboss.ejb.client.XidTransactionID;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.MessageInputStream;

/**
 * This module is using message IDs in the range 14100-14599. This file is using the subset 14100-14299 for
 * logger messages. See http://community.jboss.org/docs/DOC-16810 for the full list of currently reserved
 * JBAS message id blocks.
 *
 * @author <a href="mailto:Flemming.Harms@gmail.com">Flemming Harms</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface EjbLogger extends BasicLogger {

    /**
     * Default root level logger with the package name for he category.
     */
    EjbLogger ROOT_LOGGER = Logger.getMessageLogger(EjbLogger.class, EjbLogger.class.getPackage().getName());

    EjbLogger EJB3_LOGGER = Logger.getMessageLogger(EjbLogger.class, "org.jboss.as.ejb3");

    /**
     * logger use to log EJB invocation errors
     */
    EjbLogger EJB3_INVOCATION_LOGGER = Logger.getMessageLogger(EjbLogger.class, "org.jboss.as.ejb3.invocation");

    /**
     * Logs an error message indicating an exception occurred while removing the an inactive bean.
     *
     * @param id the session id that could not be removed
     */
    @LogMessage(level = ERROR)
    @Message(id = 14100, value = "Failed to remove %s from cache")
    void cacheRemoveFailed(Object id);

    /**
     * Logs an warning message indicating the it could not find a EJB for the specific id
     *
     * @param id the session id that could not be released
     */
    @LogMessage(level = INFO)
    @Message(id = 14101, value = "Failed to find SFSB instance with session ID %s in cache")
    void cacheEntryNotFound(Object id);

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
    @Message(id = 14122, value = "Error during retrying timeout for timer: %s")
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
     * Logs an error message indicating that an invocation failed
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

    /**
     * Log message indicating that a unsupported client marshalling strategy was received from a remote client
     *
     * @param strategy The client marshalling strategy
     * @param channel  The channel on which the client marshalling strategy was received
     */
    @LogMessage(level = INFO)
    @Message(id = 14139, value = "Unsupported client marshalling strategy %s received on channel %s ,no further communication will take place")
    void unsupportedClientMarshallingStrategy(String strategy, Channel channel);


    /**
     * Log message indicating that some error caused a channel to be closed
     *
     * @param channel The channel being closed
     * @param t       The cause
     */
    @LogMessage(level = ERROR)
    @Message(id = 14140, value = "Closing channel %s due to an error")
    void closingChannel(Channel channel, @Cause Throwable t);

    /**
     * Log message indicating that a {@link Channel.Receiver#handleEnd(org.jboss.remoting3.Channel)} notification
     * was received and the channel is being closed
     *
     * @param channel The channel for which the {@link Channel.Receiver#handleEnd(org.jboss.remoting3.Channel)} notification
     *                was received
     */
    @LogMessage(level = ERROR)
    @Message(id = 14141, value = "Channel end notification received, closing channel %s")
    void closingChannelOnChannelEnd(Channel channel);

    /**
     * Logs a message which includes the resource adapter name and the destination on which a message driven bean
     * is listening
     *
     * @param mdbName The message driven bean name
     * @param raName  The resource adapter name
     */
    @LogMessage(level = INFO)
    @Message(id = 14142, value = "Started message driven bean '%s' with '%s' resource adapter")
    void logMDBStart(final String mdbName, final String raName);

    /**
     * Logs a waring message indicating an overlapped invoking timeout for timer
     */
    @LogMessage(level = WARN)
    @Message(id = 14143, value = "A previous execution of timer [%s %s] is still in progress, skipping this overlapping scheduled execution at: %s")
    void skipOverlappingInvokeTimeout(String timedObjectId, String timerId, Date scheduledTime);

    // NOTE: messages 14144 to 14149 were moved to message bundle, do not reuse the ids

    @LogMessage(level = WARN)
    @Message(id = 14150, value = "Failed to parse property %s due to %s")
    void failedToCreateOptionForProperty(String propertyName, String reason);

    // NOTE: messages 14151 to 14163 were moved to message bundle, do not reuse the ids

    @LogMessage(level = ERROR)
    @Message(id = 14164, value = "Failed to set transaction for rollback only")
    void failedToSetRollbackOnly(@Cause Exception e);

    // NOTE: messages 14165 to 14210 were moved to message bundle, do not reuse the ids

    @LogMessage(level = INFO)
    @Message(id = 14211, value = "No jndi bindings will be created for EJB %s since no views are exposed")
    void noJNDIBindingsForSessionBean(String beanName);

    @LogMessage(level = WARN)
    @Message(id = 14212, value = "Could not send cluster formation message to the client on channel %s")
    void failedToSendClusterFormationMessageToClient(@Cause Exception e, Channel channel);

    @LogMessage(level = WARN)
    @Message(id = 14213, value = "Could not send module availability notification of module %s on channel %s")
    void failedToSendModuleAvailabilityMessageToClient(@Cause Exception e, DeploymentModuleIdentifier deploymentId, Channel channel);

    @LogMessage(level = WARN)
    @Message(id = Message.INHERIT, value = "Could not send initial module availability report to channel %s")
    void failedToSendModuleAvailabilityMessageToClient(@Cause Exception e, Channel channel);

    @LogMessage(level = WARN)
    @Message(id = 14214, value = "Could not send module un-availability notification of module %s on channel %s")
    void failedToSendModuleUnavailabilityMessageToClient(@Cause Exception e, DeploymentModuleIdentifier deploymentId, Channel channel);

    @LogMessage(level = WARN)
    @Message(id = 14215, value = "Could not send a cluster formation message for cluster: %s to the client on channel %s")
    void failedToSendClusterFormationMessageToClient(@Cause Exception e, String clusterName, Channel channel);

    @LogMessage(level = WARN)
    @Message(id = 14216, value = "Could not write a new cluster node addition message to channel %s")
    void failedToSendClusterNodeAdditionMessageToClient(@Cause Exception e, Channel channel);

    @LogMessage(level = WARN)
    @Message(id = 14217, value = "Could not write a cluster node removal message to channel %s")
    void failedToSendClusterNodeRemovalMessageToClient(@Cause Exception e, Channel channel);

    @LogMessage(level = WARN)
    @Message(id = 14218, value = "[EJB3.1 spec, section 4.9.2] Session bean implementation class MUST NOT be a interface - %s is an interface, hence won't be considered as a session bean")
    void sessionBeanClassCannotBeAnInterface(String className);

    @LogMessage(level = WARN)
    @Message(id = 14219, value = "[EJB3.1 spec, section 4.9.2] Session bean implementation class MUST be public, not abstract and not final - %s won't be considered as a session bean, since it doesn't meet that requirement")
    void sessionBeanClassMustBePublicNonAbstractNonFinal(String className);

    @LogMessage(level = WARN)
    @Message(id = 14220, value = "[EJB3.1 spec, section 5.6.2] Message driven bean implementation class MUST NOT be a interface - %s is an interface, hence won't be considered as a message driven bean")
    void mdbClassCannotBeAnInterface(String className);

    @LogMessage(level = WARN)
    @Message(id = 14221, value = "[EJB3.1 spec, section 5.6.2] Message driven bean implementation class MUST be public, not abstract and not final - %s won't be considered as a message driven bean, since it doesn't meet that requirement")
    void mdbClassMustBePublicNonAbstractNonFinal(String className);

    @LogMessage(level = WARN)
    @Message(id = 14222, value = "Method %s was a async method but the client could not be informed about the same. This will mean that the client might block till the method completes")
    void failedToSendAsyncMethodIndicatorToClient(@Cause Throwable t, Method invokedMethod);

    @LogMessage(level = WARN)
    @Message(id = 14223, value = "Asynchronous invocations are only supported on session beans. Bean class %s is not a session bean, invocation on method %s will have no asynchronous semantics")
    void asyncMethodSupportedOnlyForSessionBeans(Class beanClass, Method invokedMethod);

    @LogMessage(level = INFO)
    @Message(id = 14224, value = "Cannot add cluster node %s to cluster %s since none of the client mappings matched for address %s")
    void cannotAddClusterNodeDueToUnresolvableClientMapping(final String nodeName, final String clusterName, final InetAddress bindAddress);

    // Note that 14225-14240 is used in EjbMessages

    @LogMessage(level = ERROR)
    @Message(id = 14241, value = "Exception calling deployment added listener")
    void deploymentAddListenerException(@Cause Throwable cause);

    @LogMessage(level = ERROR)
    @Message(id = 14242, value = "Exception calling deployment removal listener")
    void deploymentRemoveListenerException(@Cause Throwable cause);

    @LogMessage(level = ERROR)
    @Message(id = 14243, value = "Failed to remove management resources for %s -- %s")
    void failedToRemoveManagementResources(InstalledComponent component, String cause);

    @LogMessage(level = INFO)
    @Message(id = 14244, value = "CORBA interface repository for %s: %s")
    void cobraInterfaceRepository(String repo, String object);

    @LogMessage(level = ERROR)
    @Message(id = 14245, value = "Cannot unregister EJBHome from CORBA naming service")
    void cannotUnregisterEJBHomeFromCobra(@Cause Throwable cause);

    @LogMessage(level = ERROR)
    @Message(id = 14246, value = "Cannot deactivate home servant")
    void cannotDeactivateHomeServant(@Cause Throwable cause);

    @LogMessage(level = ERROR)
    @Message(id = 14247, value = "Cannot deactivate bean servant")
    void cannotDeactivateBeanServant(@Cause Throwable cause);

    @LogMessage(level = ERROR)
    @Message(id = 14248, value = "Exception on channel %s from message %s")
    void exceptionOnChannel(@Cause Throwable cause, Channel channel, MessageInputStream inputStream);

    @LogMessage(level = ERROR)
    @Message(id = 14249, value = "Error invoking method %s on bean named %s for appname %s modulename %s distinctname %s")
    void errorInvokingMethod(@Cause Throwable cause, Method invokedMethod, String beanName, String appName, String moduleName, String distinctName);

    @LogMessage(level = ERROR)
    @Message(id = 14250, value = "Could not write method invocation failure for method %s on bean named %s for appname %s modulename %s distinctname %s due to")
    void couldNotWriteMethodInvocation(@Cause Throwable cause, Method invokedMethod, String beanName, String appName, String moduleName, String distinctName);

    @LogMessage(level = ERROR)
    @Message(id = 14251, value = "IOException while generating session id for invocation id: %s on channel %s")
    void exceptionGeneratingSessionId(@Cause Throwable cause, short invocationId, Channel channel);

    @LogMessage(level = ERROR)
    @Message(id = 14252, value = "Could not write out message to channel due to")
    void couldNotWriteOutToChannel(@Cause Throwable cause);

    @LogMessage(level = ERROR)
    @Message(id = 14253, value = "Could not write out invocation success message to channel due to")
    void couldNotWriteInvocationSuccessMessage(@Cause Throwable cause);

    @LogMessage(level = WARN)
    @Message(id = 14254, value = "Received unsupported message header 0x%s on channel %s")
    void unsupportedMessageHeader(String header, Channel channel);

    @LogMessage(level = ERROR)
    @Message(id = 14255, value = "Error during transaction management of transaction id %s")
    void errorDuringTransactionManagement(@Cause Throwable cause, XidTransactionID id);

    @LogMessage(level = WARN)
    @Message(id = 14256, value = "%s retrying %d")
    void retrying(String message, int count);

    @LogMessage(level = ERROR)
    @Message(id = 14257, value = "Failed to get status")
    void failedToGetStatus(@Cause Throwable cause);

    @LogMessage(level = ERROR)
    @Message(id = 14258, value = "Failed to rollback")
    void failedToRollback(@Cause Throwable cause);

    @LogMessage(level = ERROR)
    @Message(id = 14259, value = "BMT stateful bean '%s' did not complete user transaction properly status=%s")
    void transactionNotComplete(String componentName, String status);

    @LogMessage(level = ERROR)
    @Message(id = 14260, value = "Cannot delete cache %s %s, will be deleted on exit")
    void cannotDeleteCacheFile(String fileType, String fileName);

    @LogMessage(level = WARN)
    @Message(id = 14261, value = "Failed to reinstate timer '%s' (id=%s) from its persistent state")
    void timerReinstatementFailed(String timedObjectId, String timerId, @Cause Throwable cause);

    /**
     * Logs a waring message indicating an overlapped invoking timeout for timer
     */
    @LogMessage(level = WARN)
    @Message(id = 14262, value = "A previous execution of timer [%s %s] is being retried, skipping this scheduled execution at: %s")
    void skipInvokeTimeoutDuringRetry(String timedObjectId, String timerId, Date scheduledTime);


    // Don't add message ids greater that 14299!!! If you need more first check what EjbMessages is
    // using and take more (lower) numbers from the available range for this module. If the range for the module is
    // all used, go to https://community.jboss.org/docs/DOC-16810 and allocate another block for this subsystem

}

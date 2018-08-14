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

package org.jboss.as.ejb3.logging;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ejb.ConcurrentAccessTimeoutException;
import javax.ejb.EJBAccessException;
import javax.ejb.EJBException;
import javax.ejb.EJBTransactionRequiredException;
import javax.ejb.EJBTransactionRolledbackException;
import javax.ejb.IllegalLoopbackException;
import javax.ejb.LockType;
import javax.ejb.NoMoreTimeoutsException;
import javax.ejb.NoSuchEJBException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.RemoveException;
import javax.ejb.ScheduleExpression;
import javax.ejb.Timer;
import javax.ejb.TimerHandle;
import javax.ejb.TransactionAttributeType;
import javax.interceptor.InvocationContext;
import javax.naming.Context;
import javax.resource.ResourceException;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Transaction;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentCreateServiceFactory;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ee.component.ResourceInjectionTarget;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.EJBComponentUnavailableException;
import org.jboss.as.ejb3.component.EJBViewDescription;
import org.jboss.as.ejb3.component.singleton.SingletonComponent;
import org.jboss.as.ejb3.component.stateful.StatefulSessionComponentInstance;
import org.jboss.as.ejb3.subsystem.EJB3SubsystemModel;
import org.jboss.as.ejb3.subsystem.deployment.EJBComponentType;
import org.jboss.as.ejb3.subsystem.deployment.InstalledComponent;
import org.jboss.as.ejb3.timerservice.TimerImpl;
import org.jboss.as.ejb3.tx.TimerTransactionRolledBackException;
import org.jboss.as.naming.context.NamespaceContextSelector;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.dmr.ModelNode;
import org.jboss.ejb.client.EJBLocator;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jca.core.spi.rar.NotFoundException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Param;
import org.jboss.logging.annotations.Signature;
import org.jboss.metadata.ejb.spec.MethodParametersMetaData;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:Flemming.Harms@gmail.com">Flemming Harms</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@MessageLogger(projectCode = "WFLYEJB", length = 4)
public interface EjbLogger extends BasicLogger {

    EjbLogger ROOT_LOGGER = Logger.getMessageLogger(EjbLogger.class, "org.jboss.as.ejb3");

    /**
     * A logger with the category {@code org.jboss.as.ejb3.deployment} used for deployment log messages
     */
    EjbLogger DEPLOYMENT_LOGGER = Logger.getMessageLogger(EjbLogger.class, "org.jboss.as.ejb3.deployment");

    /**
     * A logger with the category {@code org.jboss.as.ejb3.remote} used for remote log messages
     */
    EjbLogger REMOTE_LOGGER = Logger.getMessageLogger(EjbLogger.class, "org.jboss.as.ejb3.remote");

    /**
     * logger use to log EJB invocation errors
     */
    EjbLogger EJB3_INVOCATION_LOGGER = Logger.getMessageLogger(EjbLogger.class, "org.jboss.as.ejb3.invocation");

    /**
     * logger use to log EJB timer messages
     */
    EjbLogger EJB3_TIMER_LOGGER = Logger.getMessageLogger(EjbLogger.class, "org.jboss.as.ejb3.timer");

//    /**
//     * Logs an error message indicating an exception occurred while removing an inactive bean.
//     *
//     * @param id the session id that could not be removed
//     */
//    @LogMessage(level = ERROR)
//    @Message(id = 1, value = "Failed to remove %s from cache")
//    void cacheRemoveFailed(Object id);

//    /**
//     * Logs a warning message indicating an EJB for the specific id could not be found
//     *
//     * @param id the session id that could not be released
//     */
//    @LogMessage(level = INFO)
//    @Message(id = 2, value = "Failed to find SFSB instance with session ID %s in cache")
//    void cacheEntryNotFound(Object id);

//    /**
//     * Logs an error message indicating an exception occurred while executing an invocation
//     *
//     * @param cause the cause of the error.
//     */
//    @LogMessage(level = ERROR)
//    @Message(id = 3, value = "Asynchronous invocation failed")
//    void asyncInvocationFailed(@Cause Throwable cause);

    /**
     * Logs an error message indicating an exception occurred while getting status
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 4, value = "failed to get tx manager status; ignoring")
    void getTxManagerStatusFailed(@Cause Throwable cause);

    /**
     * Logs an error message indicating an exception occurred while calling setRollBackOnly
     *
     * @param se the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 5, value = "failed to set rollback only; ignoring")
    void setRollbackOnlyFailed(@Cause Throwable se);

    /**
     * Logs a warning message indicating ActivationConfigProperty will be ignored since it is not allowed by resource adapter
     */
    @LogMessage(level = WARN)
    @Message(id = 6, value = "ActivationConfigProperty %s will be ignored since it is not allowed by resource adapter: %s")
    void activationConfigPropertyIgnored(Object propName, String resourceAdapterName);

    /**
     * Logs an error message indicating Discarding stateful component instance due to exception
     *
     * @param component the discarded instance
     * @param t         the cause of error
     */
    @LogMessage(level = ERROR)
    @Message(id = 7, value = "Discarding stateful component instance: %s due to exception")
    void discardingStatefulComponent(StatefulSessionComponentInstance component, @Cause Throwable t);

//    /**
//     * Logs an error message indicating it failed to remove bean with the specified session id
//     *
//     */
//    @LogMessage(level = ERROR)
//    @Message(id = 8, value = "Failed to remove bean: %s with session id %s")
//    void failedToRemoveBean(String componentName, SessionID sessionId, @Cause Throwable t);

//    /**
//     * Logs an info message indicating it could not find stateful session bean instance with id
//     *
//     * @param sessionId     the id of the session bean
//     * @param componentName
//     */
//    @LogMessage(level = INFO)
//    @Message(id = 9, value = "Could not find stateful session bean instance with id: %s for bean: %s during destruction. Probably already removed")
//    void failToFindSfsbWithId(SessionID sessionId, String componentName);

    /**
     * Logs a warning message indicating Default interceptor class is not listed in the <interceptors> section of ejb-jar.xml and will not be applied"
     */
    @LogMessage(level = WARN)
    @Message(id = 10, value = "Default interceptor class %s is not listed in the <interceptors> section of ejb-jar.xml and will not be applied")
    void defaultInterceptorClassNotListed(String clazz);

//    /**
//     * Logs a warning message indicating No method found on EJB while processing exclude-list element in ejb-jar.xml
//     */
//    @LogMessage(level = WARN)
//    @Message(id = 11, value = "No method named: %s found on EJB: %s while processing exclude-list element in ejb-jar.xml")
//    void noMethodFoundOnEjbExcludeList(String methodName, String ejbName);

//    /**
//     * Logs a warning message indicating No method with param types found on EJB while processing exclude-list element in ejb-jar.xml
//     */
//    @LogMessage(level = WARN)
//    @Message(id = 12, value = "No method named: %s with param types: %s found on EJB: %s while processing exclude-list element in ejb-jar.xml")
//    void noMethodFoundOnEjbWithParamExcludeList(String methodName, String s, String ejbName);

//    /**
//     * Logs a warning message indicating no method named found on EJB while processing method-permission element in ejb-jar.xml
//     */
//    @LogMessage(level = WARN)
//    @Message(id = 13, value = "No method named: %s found on EJB: %s while processing method-permission element in ejb-jar.xml")
//    void noMethodFoundOnEjbPermission(String methodName, String ejbName);

//    /**
//     * Logs a warning message indicating No method with param type found on EJB while processing method-permission element in ejb-jar.xml
//     */
//    @LogMessage(level = WARN)
//    @Message(id = 14, value = "No method named: %s with param types: %s found on EJB: %s while processing method-permission element in ejb-jar.xml")
//    void noMethodFoundWithParamOnEjbMethodPermission(String methodName, String s, String ejbName);


    /**
     * Logs a warning message indicating Unknown timezone id found in schedule expression. Ignoring it and using server's timezone
     */
    @LogMessage(level = WARN)
    @Message(id = 15, value = "Unknown timezone id: %s found in schedule expression. Ignoring it and using server's timezone: %s")
    void unknownTimezoneId(String timezoneId, String id);

    /**
     * Logs a warning message indicating the timer persistence is not enabled, persistent timers will not survive JVM restarts
     */
    @LogMessage(level = WARN)
    @Message(id = 16, value = "Timer persistence is not enabled, persistent timers will not survive JVM restarts")
    void timerPersistenceNotEnable();

    /**
     * Logs an info message indicating the next expiration is null. No tasks will be scheduled for timer
     */
    @LogMessage(level = INFO)
    @Message(id = 17, value = "Next expiration is null. No tasks will be scheduled for timer %S")
    void nextExpirationIsNull(TimerImpl timer);

    /**
     * Logs an error message indicating Ignoring exception during setRollbackOnly
     */
    @LogMessage(level = ERROR)
    @Message(id = 18, value = "Ignoring exception during setRollbackOnly")
    void ignoringException(@Cause Throwable e);

//    /**
//     * Logs a warning message indicating the unregistered an already registered Timerservice with id %s and a new instance will be registered
//     */
//    @LogMessage(level = WARN)
//    @Message(id = 19, value = "Unregistered an already registered Timerservice with id %s and a new instance will be registered")
//    void UnregisteredRegisteredTimerService(String timedObjectId);

    /**
     * Logs an error message indicating an error invoking timeout for timer
     */
    @LogMessage(level = ERROR)
    @Message(id = 20, value = "Error invoking timeout for timer: %s")
    void errorInvokeTimeout(Timer timer, @Cause Throwable e);

    /**
     * Logs an info message indicating timer will be retried
     */
    @LogMessage(level = INFO)
    @Message(id = 21, value = "Timer: %s will be retried")
    void timerRetried(Timer timer);

    /**
     * Logs an error message indicating an error during retyring timeout for timer
     */
    @LogMessage(level = ERROR)
    @Message(id = 22, value = "Error during retrying timeout for timer: %s")
    void errorDuringRetryTimeout(Timer timer, @Cause Throwable e);

    /**
     * Logs an info message indicating retrying timeout for timer
     */
    @LogMessage(level = INFO)
    @Message(id = 23, value = "Retrying timeout for timer: %s")
    void retryingTimeout(Timer timer);

    /**
     * Logs an info message indicating timer is not active, skipping retry of timer
     */
    @LogMessage(level = INFO)
    @Message(id = 24, value = "Timer is not active, skipping retry of timer: %s")
    void timerNotActive(Timer timer);

    /**
     * Logs a warning message indicating could not read timer information for EJB component
     */
    @LogMessage(level = WARN)
    @Message(id = 26, value = "Could not read timer information for EJB component %s")
    void failToReadTimerInformation(String componentName);

//    /**
//     * Logs an error message indicating it could not remove persistent timer
//     */
//    @LogMessage(level = ERROR)
//    @Message(id = 27, value = "Could not remove persistent timer %s")
//    void failedToRemovePersistentTimer(File file);

    /**
     * Logs an error message indicating it's not a directory, could not restore timers"
     */
    @LogMessage(level = ERROR)
    @Message(id = 28, value = "%s is not a directory, could not restore timers")
    void failToRestoreTimers(File file);

    /**
     * Logs an error message indicating it could not restore timer from file
     */
    @LogMessage(level = ERROR)
    @Message(id = 29, value = "Could not restore timer from %s")
    void failToRestoreTimersFromFile(File timerFile, @Cause Throwable e);

    /**
     * Logs an error message indicating error closing file
     */
    @LogMessage(level = ERROR)
    @Message(id = 30, value = "error closing file ")
    void failToCloseFile(@Cause Throwable e);

    /**
     * Logs an error message indicating Could not restore timers for specified id
     */
    @LogMessage(level = ERROR)
    @Message(id = 31, value = "Could not restore timers for %s")
    void failToRestoreTimersForObjectId(String timedObjectId, @Cause Throwable e);

    /**
     * Logs an error message indicating Could not restore timers for specified id
     */
    @LogMessage(level = ERROR)
    @Message(id = 32, value = "Could not create directory %s to persist EJB timers.")
    void failToCreateDirectoryForPersistTimers(File file);

//    /**
//     * Logs an error message indicating it discarding entity component instance
//     */
//    @LogMessage(level = ERROR)
//    @Message(id = 33, value = "Discarding entity component instance: %s due to exception")
//    void discardingEntityComponent(EntityBeanComponentInstance instance, @Cause Throwable t);

    /**
     * Logs an error message indicating that an invocation failed
     */
    @LogMessage(level = ERROR)
    @Message(id = 34, value = "EJB Invocation failed on component %s for method %s")
    void invocationFailed(String component, Method method, @Cause Throwable t);

    /**
     * Logs an error message indicating that an ejb client proxy could not be swapped out in a RMI invocation
     */
    @LogMessage(level = WARN)
    @Message(id = 35, value = "Could not find EJB for locator %s, EJB client proxy will not be replaced")
    void couldNotFindEjbForLocatorIIOP(EJBLocator<?> locator);


    /**
     * Logs an error message indicating that an ejb client proxy could not be swapped out in a RMI invocation
     */
    @LogMessage(level = WARN)
    @Message(id = 36, value = "EJB %s is not being replaced with a Stub as it is not exposed over IIOP")
    void ejbNotExposedOverIIOP(EJBLocator<?> locator);

    /**
     * Logs an error message indicating that dynamic stub creation failed
     */
    @LogMessage(level = ERROR)
    @Message(id = 37, value = "Dynamic stub creation failed for class %s")
    void dynamicStubCreationFailed(String clazz, @Cause Throwable t);


//    /**
//     */
//    @LogMessage(level = ERROR)
//    @Message(id = 38, value = "Exception releasing entity")
//    void exceptionReleasingEntity(@Cause Throwable t);
//
//    /**
//     * Log message indicating that an unsupported client marshalling strategy was received from a remote client
//     *
//     * @param strategy The client marshalling strategy
//     * @param channel  The channel on which the client marshalling strategy was received
//     */
//    @LogMessage(level = INFO)
//    @Message(id = 39, value = "Unsupported client marshalling strategy %s received on channel %s ,no further communication will take place")
//    void unsupportedClientMarshallingStrategy(String strategy, Channel channel);
//
//
//    /**
//     * Log message indicating that some error caused a channel to be closed
//     *
//     * @param channel The channel being closed
//     * @param t       The cause
//     */
//    @LogMessage(level = ERROR)
//    @Message(id = 40, value = "Closing channel %s due to an error")
//    void closingChannel(Channel channel, @Cause Throwable t);
//
//    /**
//     * Log message indicating that a {@link Channel.Receiver#handleEnd(org.jboss.remoting3.Channel)} notification
//     * was received and the channel is being closed
//     *
//     * @param channel The channel for which the {@link Channel.Receiver#handleEnd(org.jboss.remoting3.Channel)} notification
//     *                was received
//     */
//    @LogMessage(level = DEBUG)
//    @Message(id = 41, value = "Channel end notification received, closing channel %s")
//    void closingChannelOnChannelEnd(Channel channel);

    /**
     * Logs a message which includes the resource adapter name and the destination on which a message driven bean
     * is listening
     *
     * @param mdbName The message driven bean name
     * @param raName  The resource adapter name
     */
    @LogMessage(level = INFO)
    @Message(id = 42, value = "Started message driven bean '%s' with '%s' resource adapter")
    void logMDBStart(final String mdbName, final String raName);

    /**
     * Logs a waring message indicating an overlapped invoking timeout for timer
     */
    @LogMessage(level = WARN)
    @Message(id = 43, value = "A previous execution of timer %s is still in progress, skipping this overlapping scheduled execution at: %s.")
    void skipOverlappingInvokeTimeout(Timer timer, Date scheduledTime);

    /**
     * Returns a {@link IllegalStateException} indicating that {@link org.jboss.jca.core.spi.rar.ResourceAdapterRepository}
     * was unavailable
     *
     * @return the exception
     */
    @Message(id = 44, value = "Resource adapter repository is not available")
    IllegalStateException resourceAdapterRepositoryUnAvailable();

    /**
     * Returns a {@link IllegalArgumentException} indicating that no {@link org.jboss.jca.core.spi.rar.Endpoint}
     * could be found for a resource adapter named <code>resourceAdapterName</code>
     *
     * @param resourceAdapterName The name of the resource adapter
     * @param notFoundException   The original exception cause
     * @return the exception
     */
    @Message(id = 45, value = "Could not find an Endpoint for resource adapter %s")
    IllegalArgumentException noSuchEndpointException(final String resourceAdapterName, @Cause NotFoundException notFoundException);

    /**
     * Returns a {@link IllegalStateException} indicating that the {@link org.jboss.jca.core.spi.rar.Endpoint}
     * is not available
     *
     * @param componentName The MDB component name
     * @return the exception
     */
    @Message(id = 46, value = "Endpoint is not available for message driven component %s")
    IllegalStateException endpointUnAvailable(String componentName);

    /**
     * Returns a {@link RuntimeException} indicating that the {@link org.jboss.jca.core.spi.rar.Endpoint}
     * for the message driven component, could not be deactivated
     *
     * @param componentName The message driven component name
     * @param cause         Original cause
     * @return the exception
     */
    @Message(id = 47, value = "Could not deactivate endpoint for message driven component %s")
    RuntimeException failureDuringEndpointDeactivation(final String componentName, @Cause ResourceException cause);

//    @Message(id = 48, value = "")
//    UnsupportedCallbackException unsupportedCallback(@Param Callback current);

    @Message(id = 49, value = "Could not create an instance of cluster node selector %s for cluster %s")
    RuntimeException failureDuringLoadOfClusterNodeSelector(final String clusterNodeSelectorName, final String clusterName, @Cause Exception e);

    @LogMessage(level = WARN)
    @Message(id = 50, value = "Failed to parse property %s due to %s")
    void failedToCreateOptionForProperty(String propertyName, String reason);

    @Message(id = 51, value = "Could not find view %s for EJB %s")
    IllegalStateException viewNotFound(String viewClass, String ejbName);

    @Message(id = 52, value = "Cannot perform asynchronous local invocation for component that is not a session bean")
    RuntimeException asyncInvocationOnlyApplicableForSessionBeans();

    @Message(id = 53, value = "%s is not a Stateful Session bean in app: %s module: %s distinct-name: %s")
    IllegalArgumentException notStatefulSessionBean(String ejbName, String appName, String moduleName, String distinctName);

    @Message(id = 54, value = "Failed to marshal EJB parameters")
    RuntimeException failedToMarshalEjbParameters(@Cause Exception e);

    @Message(id = 55, value = "No matching deployment for EJB: %s")
    NoSuchEJBException unknownDeployment(EJBLocator<?> locator);

    @Message(id = 56, value = "Could not find EJB in matching deployment: %s")
    NoSuchEJBException ejbNotFoundInDeployment(EJBLocator<?> locator);

    @Message(id = 57, value = "%s annotation is only valid on method targets")
    IllegalArgumentException annotationApplicableOnlyForMethods(String annotationName);

    @Message(id = 58, value = "Method %s, on class %s, annotated with @javax.interceptor.AroundTimeout is expected to accept a single param of type javax.interceptor.InvocationContext")
    IllegalArgumentException aroundTimeoutMethodExpectedWithInvocationContextParam(String methodName, String className);

    @Message(id = 59, value = "Method %s, on class %s, annotated with @javax.interceptor.AroundTimeout must return Object type")
    IllegalArgumentException aroundTimeoutMethodMustReturnObjectType(String methodName, String className);

    @Message(id = 60, value = "Wrong tx on thread: expected %s, actual %s")
    IllegalStateException wrongTxOnThread(Transaction expected, Transaction actual);

    @Message(id = 61, value = "Unknown transaction attribute %s on invocation %s")
    IllegalStateException unknownTxAttributeOnInvocation(TransactionAttributeType txAttr, InterceptorContext invocation);

    @Message(id = 62, value = "Transaction is required for invocation %s")
    EJBTransactionRequiredException txRequiredForInvocation(InterceptorContext invocation);

    @Message(id = 63, value = "Transaction present on server in Never call (EJB3 13.6.2.6)")
    EJBException txPresentForNeverTxAttribute();

    @LogMessage(level = ERROR)
    @Message(id = 64, value = "Failed to set transaction for rollback only")
    void failedToSetRollbackOnly(@Cause Throwable e);

    @Message(id = 65, value = "View interface cannot be null")
    IllegalArgumentException viewInterfaceCannotBeNull();

//    @Message(id = 66, value = "Cannot call getEjbObject before the object is associated with a primary key")
//    IllegalStateException cannotCallGetEjbObjectBeforePrimaryKeyAssociation();
//
//    @Message(id = 67, value = "Cannot call getEjbLocalObject before the object is associated with a primary key")
//    IllegalStateException cannotCallGetEjbLocalObjectBeforePrimaryKeyAssociation();

    @Message(id = 68, value = "Could not load view class for component %s")
    RuntimeException failedToLoadViewClassForComponent(@Cause Exception e, String componentName);

//    @Message(id = 69, value = "Entities can not be created for %s bean since no create method is available.")
//    IllegalStateException entityCannotBeCreatedDueToMissingCreateMethod(String beanName);
//
//    @Message(id = 70, value = "%s is not an entity bean component")
//    IllegalArgumentException notAnEntityBean(Component component);
//
//    @Message(id = 71, value = "Instance for PK [%s] already registered")
//    IllegalStateException instanceAlreadyRegisteredForPK(Object primaryKey);
//
//    @Message(id = 72, value = "Instance [%s] not found in cache")
//    IllegalStateException entityBeanInstanceNotFoundInCache(EntityBeanComponentInstance instance);

    @Message(id = 73, value = "Illegal call to EJBHome.remove(Object) on a session bean")
    RemoveException illegalCallToEjbHomeRemove();

    @Message(id = 74, value = "EJB 3.1 FR 13.6.2.8 setRollbackOnly is not allowed with SUPPORTS transaction attribute")
    IllegalStateException setRollbackOnlyNotAllowedForSupportsTxAttr();

    @Message(id = 75, value = "Cannot call getPrimaryKey on a session bean")
    EJBException cannotCallGetPKOnSessionBean();

    @Message(id = 76, value = "Singleton beans cannot have EJB 2.x views")
    RuntimeException ejb2xViewNotApplicableForSingletonBeans();

//    @Message(id = 77, value = "ClassTable %s cannot find a class for class index %d")
//    ClassNotFoundException classNotFoundInClassTable(String classTableName, int index);

    @Message(id = 78, value = "Bean %s does not have an EJBLocalObject")
    IllegalStateException ejbLocalObjectUnavailable(String beanName);

    @Message(id = 79, value = "[EJB 3.1 spec, section 14.1.1] Class: %s cannot be marked as an application exception because it is not of type java.lang.Exception")
    IllegalArgumentException cannotBeApplicationExceptionBecauseNotAnExceptionType(Class<?> klass);

    @Message(id = 80, value = "[EJB 3.1 spec, section 14.1.1] Exception class: %s cannot be marked as an application exception because it is of type java.rmi.RemoteException")
    IllegalArgumentException rmiRemoteExceptionCannotBeApplicationException(Class<?> klass);

    @Message(id = 81, value = "%s annotation is allowed only on classes. %s is not a class")
    RuntimeException annotationOnlyAllowedOnClass(String annotationName, AnnotationTarget incorrectTarget);

    @Message(id = 82, value = "Bean %s specifies @Remote annotation, but does not implement 1 interface")
    DeploymentUnitProcessingException beanWithRemoteAnnotationImplementsMoreThanOneInterface(Class<?> beanClass);

    @Message(id = 83, value = "Bean %s specifies @Local annotation, but does not implement 1 interface")
    DeploymentUnitProcessingException beanWithLocalAnnotationImplementsMoreThanOneInterface(Class<?> beanClass);

    @Message(id = 84, value = "Could not analyze remote interface for %s")
    RuntimeException failedToAnalyzeRemoteInterface(@Cause Exception e, String beanName);

    @Message(id = 85, value = "Exception while parsing %s")
    DeploymentUnitProcessingException failedToParse(@Cause Exception e, String filePath);

    @Message(id = 86, value = "Failed to install management resources for %s")
    DeploymentUnitProcessingException failedToInstallManagementResource(@Cause Exception e, String componentName);

    @Message(id = 87, value = "Could not load view %s")
    RuntimeException failedToLoadViewClass(@Cause Exception e, String viewClassName);

    @Message(id = 88, value = "Could not determine type of ejb-ref %s for injection target %s")
    DeploymentUnitProcessingException couldNotDetermineEjbRefForInjectionTarget(String ejbRefName, ResourceInjectionTarget injectionTarget);

    @Message(id = 89, value = "Could not determine type of ejb-local-ref %s for injection target %s")
    DeploymentUnitProcessingException couldNotDetermineEjbLocalRefForInjectionTarget(String ejbLocalRefName, ResourceInjectionTarget injectionTarget);

    @Message(id = 90, value = "@EJB injection target %s is invalid. Only setter methods are allowed")
    IllegalArgumentException onlySetterMethodsAllowedToHaveEJBAnnotation(MethodInfo methodInfo);

    @Message(id = 91, value = "@EJB attribute 'name' is required for class level annotations. Class: %s")
    DeploymentUnitProcessingException nameAttributeRequiredForEJBAnnotationOnClass(String className);

    @Message(id = 92, value = "@EJB attribute 'beanInterface' is required for class level annotations. Class: %s")
    DeploymentUnitProcessingException beanInterfaceAttributeRequiredForEJBAnnotationOnClass(String className);

    @Message(id = 93, value = "Module hasn't been attached to deployment unit %s")
    IllegalStateException moduleNotAttachedToDeploymentUnit(DeploymentUnit deploymentUnit);

    @Message(id = 94, value = "EJB 3.1 FR 5.4.2 MessageDrivenBean %s does not implement 1 interface nor specifies message listener interface")
    DeploymentUnitProcessingException mdbDoesNotImplementNorSpecifyMessageListener(ClassInfo beanClass);

    @Message(id = 95, value = "Unknown session bean type %s")
    IllegalArgumentException unknownSessionBeanType(String sessionType);

    @Message(id = 96, value = "More than one method found with name %s on %s")
    DeploymentUnitProcessingException moreThanOneMethodWithSameNameOnComponent(String methodName, Class<?> componentClass);

    @Message(id = 97, value = "Unknown EJB locator type %s")
    RuntimeException unknownEJBLocatorType(EJBLocator<?> locator);

    @Message(id = 98, value = "Could not create CORBA object for %s")
    RuntimeException couldNotCreateCorbaObject(@Cause Exception cause, EJBLocator<?> locator);

    @Message(id = 99, value = "Provided locator %s was not for EJB %s")
    IllegalArgumentException incorrectEJBLocatorForBean(EJBLocator<?> locator, String beanName);

    @Message(id = 100, value = "Failed to lookup java:comp/ORB")
    IOException failedToLookupORB();

    @Message(id = 101, value = "%s is not an ObjectImpl")
    IOException notAnObjectImpl(Class<?> type);

    @Message(id = 102, value = "Message endpoint %s has already been released")
    UnavailableException messageEndpointAlreadyReleased(MessageEndpoint messageEndpoint);

//    @Message(id = 103, value = "Cannot handle client version %s")
//    RuntimeException ejbRemoteServiceCannotHandleClientVersion(byte version);
//
//    @Message(id = 104, value = "Could not find marshaller factory for marshaller strategy %s")
//    RuntimeException failedToFindMarshallerFactoryForStrategy(String marshallerStrategy);

    @Message(id = 105, value = "%s is not an EJB component")
    IllegalArgumentException notAnEJBComponent(Component component);

    @Message(id = 106, value = "Could not load method param class %s of timeout method")
    RuntimeException failedToLoadTimeoutMethodParamClass(@Cause Exception cause, String className);

    @Message(id = 107, value = "Timer invocation failed, invoker is not started")
    IllegalStateException timerInvocationFailedDueToInvokerNotBeingStarted();

//    @Message(id = 108, value = "Could not load timer with id %s")
//    NoSuchObjectLocalException timerNotFound(String timerId);

    @Message(id = 109, value = "Invalid value for second: %s")
    IllegalArgumentException invalidValueForSecondInScheduleExpression(String value);

    @Message(id = 110, value = "Timer invocation failed, transaction rolled back")
    TimerTransactionRolledBackException timerInvocationRolledBack();

    @LogMessage(level = INFO)
    @Message(id = 111, value = "No jndi bindings will be created for EJB %s since no views are exposed")
    void noJNDIBindingsForSessionBean(String beanName);

//    @LogMessage(level = WARN)
//    @Message(id = 112, value = "Could not send cluster formation message to the client on channel %s")
//    void failedToSendClusterFormationMessageToClient(@Cause Exception e, Channel channel);
//
//    @LogMessage(level = WARN)
//    @Message(id = 113, value = "Could not send module availability notification of module %s on channel %s")
//    void failedToSendModuleAvailabilityMessageToClient(@Cause Exception e, DeploymentModuleIdentifier deploymentId, Channel channel);
//
//    @LogMessage(level = WARN)
//    @Message(id = Message.INHERIT, value = "Could not send initial module availability report to channel %s")
//    void failedToSendModuleAvailabilityMessageToClient(@Cause Exception e, Channel channel);
//
//    @LogMessage(level = WARN)
//    @Message(id = 114, value = "Could not send module un-availability notification of module %s on channel %s")
//    void failedToSendModuleUnavailabilityMessageToClient(@Cause Exception e, DeploymentModuleIdentifier deploymentId, Channel channel);
//
//    @LogMessage(level = WARN)
//    @Message(id = 115, value = "Could not send a cluster formation message for cluster: %s to the client on channel %s")
//    void failedToSendClusterFormationMessageToClient(@Cause Exception e, String clusterName, Channel channel);
//
//    @LogMessage(level = WARN)
//    @Message(id = 116, value = "Could not write a new cluster node addition message to channel %s")
//    void failedToSendClusterNodeAdditionMessageToClient(@Cause Exception e, Channel channel);
//
//    @LogMessage(level = WARN)
//    @Message(id = 117, value = "Could not write a cluster node removal message to channel %s")
//    void failedToSendClusterNodeRemovalMessageToClient(@Cause Exception e, Channel channel);

    @LogMessage(level = WARN)
    @Message(id = 118, value = "[EJB3.1 spec, section 4.9.2] Session bean implementation class MUST NOT be a interface - %s is an interface, hence won't be considered as a session bean")
    void sessionBeanClassCannotBeAnInterface(String className);

    @LogMessage(level = WARN)
    @Message(id = 119, value = "[EJB3.1 spec, section 4.9.2] Session bean implementation class MUST be public, not abstract and not final - %s won't be considered as a session bean, since it doesn't meet that requirement")
    void sessionBeanClassMustBePublicNonAbstractNonFinal(String className);

    @LogMessage(level = WARN)
    @Message(id = 120, value = "[EJB3.1 spec, section 5.6.2] Message driven bean implementation class MUST NOT be a interface - %s is an interface, hence won't be considered as a message driven bean")
    void mdbClassCannotBeAnInterface(String className);

    @LogMessage(level = WARN)
    @Message(id = 121, value = "[EJB3.1 spec, section 5.6.2] Message driven bean implementation class MUST be public, not abstract and not final - %s won't be considered as a message driven bean, since it doesn't meet that requirement")
    void mdbClassMustBePublicNonAbstractNonFinal(String className);

//    @LogMessage(level = WARN)
//    @Message(id = 122, value = "Method %s was a async method but the client could not be informed about the same. This will mean that the client might block till the method completes")
//    void failedToSendAsyncMethodIndicatorToClient(@Cause Throwable t, Method invokedMethod);
//
//    @LogMessage(level = WARN)
//    @Message(id = 123, value = "Asynchronous invocations are only supported on session beans. Bean class %s is not a session bean, invocation on method %s will have no asynchronous semantics")
//    void asyncMethodSupportedOnlyForSessionBeans(Class<?> beanClass, Method invokedMethod);
//
//    @LogMessage(level = INFO)
//    @Message(id = 124, value = "Cannot add cluster node %s to cluster %s since none of the client mappings matched for addresses %s")
//    void cannotAddClusterNodeDueToUnresolvableClientMapping(final String nodeName, final String clusterName, final Object bindings);

    @Message(id = 125, value = "Could not create an instance of deployment node selector %s")
    DeploymentUnitProcessingException failedToCreateDeploymentNodeSelector(@Cause Exception e, String deploymentNodeSelectorClassName);

//    @Message(id = 126, value = "Could not lookup service %s")
//    IllegalStateException serviceNotFound(ServiceName serviceName);

    @Message(id = 127, value = "EJB %s of type %s must have public default constructor")
    DeploymentUnitProcessingException ejbMustHavePublicDefaultConstructor(String componentName, String componentClassName);

    @Message(id = 128, value = "EJB %s of type %s must not be inner class")
    DeploymentUnitProcessingException ejbMustNotBeInnerClass(String componentName, String componentClassName);

    @Message(id = 129, value = "EJB %s of type %s must be declared public")
    DeploymentUnitProcessingException ejbMustBePublicClass(String componentName, String componentClassName);

    @Message(id = 130, value = "EJB %s of type %s must not be declared final")
    DeploymentUnitProcessingException ejbMustNotBeFinalClass(String componentName, String componentClassName);

//    @Message(id = 131, value = "EJB client context selector failed due to unavailability of %s service")
//    IllegalStateException ejbClientContextSelectorUnableToFunctionDueToMissingService(ServiceName serviceName);

    @Message(id = 132, value = "@PostConstruct method of EJB singleton %s of type %s has been recursively invoked")
    IllegalStateException reentrantSingletonCreation(String componentName, String componentClassName);

//    @Message(id = 133, value = "Failed to read EJB info")
//    IOException failedToReadEjbInfo(@Cause Throwable e);
//
//    @Message(id = 134, value = "Failed to read EJB Locator")
//    IOException failedToReadEJBLocator(@Cause Throwable e);
//
//    @Message(id = 135, value = "default-security-domain was defined")
//    String rejectTransformationDefinedDefaultSecurityDomain();
//
//    @Message(id = 136, value = "default-missing-method-permissions-deny-access was set to true")
//    String rejectTransformationDefinedDefaultMissingMethodPermissionsDenyAccess();

    @Message(id = 137, value = "Only session and message-driven beans with bean-managed transaction demarcation are allowed to access UserTransaction")
    IllegalStateException unauthorizedAccessToUserTransaction();

//    @Message(id = 138, value = "More than one timer found in database with id %s")
//    RuntimeException moreThanOneTimerFoundWithId(String id);

    @Message(id = 139, value = "The timer service has been disabled. Please add a <timer-service> entry into the ejb section of the server configuration to enable it.")
    String timerServiceIsNotActive();

    @Message(id = 140, value = "This EJB does not have any timeout methods")
    String ejbHasNoTimerMethods();

    @LogMessage(level = ERROR)
    @Message(id = 141, value = "Exception calling deployment added listener")
    void deploymentAddListenerException(@Cause Throwable cause);

    @LogMessage(level = ERROR)
    @Message(id = 142, value = "Exception calling deployment removal listener")
    void deploymentRemoveListenerException(@Cause Throwable cause);

    @LogMessage(level = ERROR)
    @Message(id = 143, value = "Failed to remove management resources for %s -- %s")
    void failedToRemoveManagementResources(InstalledComponent component, String cause);

    @LogMessage(level = INFO)
    @Message(id = 144, value = "CORBA interface repository for %s: %s")
    void cobraInterfaceRepository(String repo, String object);

    @LogMessage(level = ERROR)
    @Message(id = 145, value = "Cannot unregister EJBHome from CORBA naming service")
    void cannotUnregisterEJBHomeFromCobra(@Cause Throwable cause);

    @LogMessage(level = ERROR)
    @Message(id = 146, value = "Cannot deactivate home servant")
    void cannotDeactivateHomeServant(@Cause Throwable cause);

    @LogMessage(level = ERROR)
    @Message(id = 147, value = "Cannot deactivate bean servant")
    void cannotDeactivateBeanServant(@Cause Throwable cause);

//    @LogMessage(level = ERROR)
//    @Message(id = 148, value = "Exception on channel %s from message %s")
//    void exceptionOnChannel(@Cause Throwable cause, Channel channel, MessageInputStream inputStream);
//
//    @LogMessage(level = ERROR)
//    @Message(id = 149, value = "Error invoking method %s on bean named %s for appname %s modulename %s distinctname %s")
//    void errorInvokingMethod(@Cause Throwable cause, Method invokedMethod, String beanName, String appName, String moduleName, String distinctName);

    @LogMessage(level = ERROR)
    @Message(id = 150, value = "Could not write method invocation failure for method %s on bean named %s for appname %s modulename %s distinctname %s due to")
    void couldNotWriteMethodInvocation(@Cause Throwable cause, Method invokedMethod, String beanName, String appName, String moduleName, String distinctName);

    @LogMessage(level = ERROR)
    @Message(id = 151, value = "Exception while generating session id for component %s with invocation %s")
    void exceptionGeneratingSessionId(@Cause Throwable cause, String componentName, Object invocationInformation);

//    @LogMessage(level = ERROR)
//    @Message(id = 152, value = "Could not write out message to channel due to")
//    void couldNotWriteOutToChannel(@Cause Throwable cause);
//
//    @LogMessage(level = ERROR)
//    @Message(id = 153, value = "Could not write out invocation success message to channel due to")
//    void couldNotWriteInvocationSuccessMessage(@Cause Throwable cause);
//
//    @LogMessage(level = WARN)
//    @Message(id = 154, value = "Received unsupported message header 0x%x on channel %s")
//    void unsupportedMessageHeader(int header, Channel channel);
//
//    @LogMessage(level = ERROR)
//    @Message(id = 155, value = "Error during transaction management of transaction id %s")
//    void errorDuringTransactionManagement(@Cause Throwable cause, XidTransactionID id);
//
//    @LogMessage(level = WARN)
//    @Message(id = 156, value = "%s retrying %d")
//    void retrying(String message, int count);

    @LogMessage(level = ERROR)
    @Message(id = 157, value = "Failed to get status")
    void failedToGetStatus(@Cause Throwable cause);

    @LogMessage(level = ERROR)
    @Message(id = 158, value = "Failed to rollback")
    void failedToRollback(@Cause Throwable cause);

    @LogMessage(level = ERROR)
    @Message(id = 159, value = "BMT stateful bean '%s' did not complete user transaction properly status=%s")
    void transactionNotComplete(String componentName, String status);

//    @LogMessage(level = ERROR)
//    @Message(id = 160, value = "Cannot delete cache %s %s, will be deleted on exit")
//    void cannotDeleteCacheFile(String fileType, String fileName);

    @LogMessage(level = WARN)
    @Message(id = 161, value = "Failed to reinstate timer '%s' (id=%s) from its persistent state")
    void timerReinstatementFailed(String timedObjectId, String timerId, @Cause Throwable cause);

    /**
     * Logs a waring message indicating an overlapped invoking timeout for timer
     */
    @LogMessage(level = WARN)
    @Message(id = 162, value = "A previous execution of timer %s is being retried, skipping this scheduled execution at: %s")
    void skipInvokeTimeoutDuringRetry(Timer timer, Date scheduledTime);

    @LogMessage(level = ERROR)
    @Message(id = 163, value = "Cannot create table for timer persistence")
    void couldNotCreateTable(@Cause SQLException e);

    @LogMessage(level = ERROR)
    @Message(id = 164, value = "Exception running timer task for timer %s on EJB %s")
    void exceptionRunningTimerTask(Timer timer, String timedObjectId, @Cause  Exception e);

//    @LogMessage(level = ERROR)
//    @Message(id = 165, value = "Error during transaction recovery")
//    void errorDuringTransactionRecovery(@Cause Throwable cause);

    @LogMessage(level = WARN)
    @Message(id = 166, value = "The @%s annotation is deprecated and will be ignored.")
    void deprecatedAnnotation(String annotation);

    @LogMessage(level = WARN)
    @Message(id = 167, value = "The <%2$s xmlns=\"%1$s\"/> element will be ignored.")
    void deprecatedNamespace(String namespace, String element);

    /**
     * Creates an exception indicating it could not find the EJB with specific id
     *
     * @param sessionId Session id
     * @return a {@link NoSuchEJBException} for the error.
     */
    @Message(id = 168, value = "Could not find EJB with id %s")
    NoSuchEJBException couldNotFindEjb(String sessionId);

    /**
     * Creates an exception indicating it a component was not set on the InterceptorContext
     *
     * @param context the context.
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 169, value = "Component not set in InterceptorContext: %s")
    IllegalStateException componentNotSetInInterceptor(InterceptorContext context);

    /**
     * Creates an exception indicating the method was called with null in the name
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 170, value = "Method name cannot be null")
    IllegalArgumentException methodNameIsNull();

    /**
     * Creates an exception indicating the bean home interface was not set
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 171, value = "Bean %s does not have a Home interface")
    IllegalStateException beanHomeInterfaceIsNull(String componentName);

    /**
     * Creates an exception indicating the bean local home interface was not set
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 172, value = "Bean %s does not have a Local Home interface")
    IllegalStateException beanLocalHomeInterfaceIsNull(String componentName);

    /**
     * Creates an exception indicating the getRollBackOnly was called on none container-managed transaction
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 173, value = "EJB 3.1 FR 13.6.1 Only beans with container-managed transaction demarcation " +
            "can use getRollbackOnly.")
    IllegalStateException failToCallgetRollbackOnly();

    /**
     * Creates an exception indicating the getRollBackOnly not allowed without a transaction
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 174, value = "getRollbackOnly() not allowed without a transaction.")
    IllegalStateException failToCallgetRollbackOnlyOnNoneTransaction();

    /**
     * Creates an exception indicating the call getRollBackOnly not allowed after transaction is completed
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 175, value = "getRollbackOnly() not allowed after transaction is completed (EJBTHREE-1445)")
    IllegalStateException failToCallgetRollbackOnlyAfterTxcompleted();

//    /**
//     * Creates an exception indicating the call isBeanManagedTransaction is not allowed without bean-managed transaction
//     *
//     * @return a {@link IllegalStateException} for the error.
//     */
//    @Message(id = 176, value = "EJB 3.1 FR 4.3.3 & 5.4.5 Only beans with bean-managed transaction demarcation can use this method.")
//    IllegalStateException failToCallIsBeanManagedTransaction();

    /**
     * Creates an exception indicating the call lookup was call with an empty jndi name
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 177, value = "jndi name cannot be null during lookup")
    IllegalArgumentException jndiNameCannotBeNull();

    /**
     * Creates an exception indicating the NamespaceContextSelector was not set
     *
     * @param name the jndi name
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 178, value = "No NamespaceContextSelector available, cannot lookup %s")
    IllegalArgumentException noNamespaceContextSelectorAvailable(String name);

    /**
     * Creates an exception indicating the NamespaceContextSelector was not set
     *
     * @param name the jndi name
     * @param e    cause of the exception
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 179, value = " Could not lookup jndi name: %s")
    RuntimeException failToLookupJNDI(String name, @Cause Throwable e);

    /**
     * Creates an exception indicating the namespace was wrong
     *
     * @param name the jndi name
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 180, value = "Cannot lookup jndi name: %s since it" +
            " doesn't belong to java:app, java:module, java:comp or java:global namespace")
    IllegalArgumentException failToLookupJNDINameSpace(String name);

    /**
     * Creates an exception indicating it failed to lookup the namespace
     *
     * @param namespaceContextSelector selector for the namespace context
     * @param jndiContext              the jndi context it was looked up on
     * @param ne                       cause of the exception
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 181, value = "Could not lookup jndi name: %s in context: %s")
    IllegalArgumentException failToLookupStrippedJNDI(NamespaceContextSelector namespaceContextSelector, Context jndiContext, @Cause Throwable ne);

    /**
     * Creates an exception indicating setRollBackOnly was called on none CMB
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 182, value = "EJB 3.1 FR 13.6.1 Only beans with container-managed transaction demarcation " +
            "can use setRollbackOnly.")
    IllegalStateException failToCallSetRollbackOnlyOnNoneCMB();

    /**
     * Creates an exception indicating setRollBackOnly was without a transaction
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 183, value = "setRollbackOnly() not allowed without a transaction.")
    IllegalStateException failToCallSetRollbackOnlyWithNoTx();

    /**
     * Creates an exception indicating EjbJarConfiguration cannot be null
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 184, value = "EjbJarConfiguration cannot be null")
    IllegalArgumentException EjbJarConfigurationIsNull();

    /**
     * Creates an exception indicating the security roles is null
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 185, value = "Cannot set security roles to null")
    IllegalArgumentException SecurityRolesIsNull();

//    /**
//     * Creates an exception indicating the classname was null or empty
//     *
//     * @return a {@link IllegalArgumentException} for the error.
//     */
//    @Message(id = 186, value = "Classname cannot be null or empty: %s")
//    IllegalArgumentException classnameIsNull(String className);
//
//    /**
//     * Creates an exception indicating it can't set null roles for the class
//     *
//     * @return a {@link IllegalArgumentException} for the error.
//     */
//    @Message(id = 187, value = "Cannot set null roles for class %s")
//    IllegalArgumentException setRolesForClassIsNull(String className);
//
//    /**
//     * Creates an exception indicating EJB method identifier cannot be null while setting roles on method
//     *
//     * @return a {@link IllegalArgumentException} for the error.
//     */
//    @Message(id = 188, value = "EJB method identifier cannot be null while setting roles on method")
//    IllegalArgumentException ejbMethodIsNull();
//
//    /**
//     * Creates an exception indicating roles cannot be null while setting roles on method
//     *
//     * @return a {@link IllegalArgumentException} for the error.
//     */
//    @Message(id = 189, value = "Roles cannot be null while setting roles on method: %s")
//    IllegalArgumentException rolesIsNull(EJBMethodIdentifier ejbMethodIdentifier);

//    /**
//     * Creates an exception indicating EJB method identifier cannot be null while setting roles on view type
//     *
//     * @return a {@link IllegalArgumentException} for the error.
//     */
//    @Message(id = 190, value = "EJB method identifier cannot be null while setting roles on view type: %s")
//    IllegalArgumentException ejbMethodIsNullForViewType(MethodIntf viewType);

//    /**
//     * Creates an exception indicating roles cannot be null while setting roles on view type
//     *
//     * @param viewType
//     * @return a {@link IllegalArgumentException} for the error.
//     */
//    @Message(id = 191, value = "Roles cannot be null while setting roles on view type: %s")
//    IllegalArgumentException rolesIsNullOnViewType(final MethodIntf viewType);

//    /**
//     * Creates an exception indicating roles cannot be null while setting roles on view type and method"
//     *
//     * @param viewType
//     * @return a {@link IllegalArgumentException} for the error.
//     */
//    @Message(id = 192, value = "Roles cannot be null while setting roles on view type: %s and method: %s")
//    IllegalArgumentException rolesIsNullOnViewTypeAndMethod(MethodIntf viewType, EJBMethodIdentifier ejbMethodIdentifier);

    /**
     * Creates an exception indicating it cannot link from a null or empty security role
     *
     * @param fromRole role it link from
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 193, value = "Cannot link from a null or empty security role: %s")
    IllegalArgumentException failToLinkFromEmptySecurityRole(String fromRole);

    /**
     * Creates an exception indicating it cannot link to a null or empty security role:
     *
     * @param toRole role it link to
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 194, value = "Cannot link to a null or empty security role: %s")
    IllegalArgumentException failToLinkToEmptySecurityRole(String toRole);

    /**
     * Creates an exception indicating that the EjbJarConfiguration was not found as an attachment in deployment unit
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 195, value = "EjbJarConfiguration not found as an attachment in deployment unit: %s")
    DeploymentUnitProcessingException ejbJarConfigNotFound(DeploymentUnit deploymentUnit);

    /**
     * Creates an exception indicating the component view instance is not available in interceptor context
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 196, value = "ComponentViewInstance not available in interceptor context: %s")
    IllegalStateException componentViewNotAvailableInContext(InterceptorContext context);

//    /**
//     * Creates an exception indicating it fail to call the timeout method
//     *
//     * @param method
//     * @return a {@link RuntimeException} for the error.
//     */
//    @Message(id = 197, value = "Unknown timeout method %s")
//    RuntimeException failToCallTimeOutMethod(Method method);
//
//    /**
//     * Creates an exception indicating timeout method was not set for the component
//     *
//     * @param componentName name of the component
//     * @return a {@link IllegalArgumentException} for the error.
//     */
//    @Message(id = 198, value = "Component %s does not have a timeout method")
//    IllegalArgumentException componentTimeoutMethodNotSet(String componentName);

    /**
     * Creates an exception indicating no resource adapter registered with resource adapter name
     *
     * @param resourceAdapterName name of the RA
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 199, value = "No resource adapter registered with resource adapter name %s")
    IllegalStateException unknownResourceAdapter(String resourceAdapterName);

//    /**
//     * Creates an exception indicating multiple resource adapter was registered
//     *
//     * @param resourceAdapterName name of the RA
//     * @return a {@link IllegalStateException} for the error.
//     */
//    @Message(id = 200, value = "found more than one RA registered as %s")
//    IllegalStateException multipleResourceAdapterRegistered(String resourceAdapterName);

    /**
     * Creates an exception indicating security is not enabled
     *
     * @return a {@link UnsupportedOperationException} for the error.
     */
    @Message(id = 201, value = "Security is not enabled")
    UnsupportedOperationException securityNotEnabled();

    /**
     * Creates an exception indicating it fail to complete task before time out
     *
     * @return a {@link TimeoutException} for the error.
     */
    @Message(id = 202, value = "Task did not complete in %s  %S")
    TimeoutException failToCompleteTaskBeforeTimeOut(long timeout, TimeUnit unit);

    /**
     * Creates an exception indicating the task was cancelled
     *
     * @return a {@link TimeoutException} for the error.
     */
    @Message(id = 203, value = "Task was cancelled")
    CancellationException taskWasCancelled();

//    /**
//     * Creates an exception indicating that it could not resolve ejbRemove method for interface method on EJB
//     *
//     * @return a {@link DeploymentUnitProcessingException} for the error.
//     */
//    @Message(id = 204, value = "Could not resolve ejbRemove method for interface method on EJB %s")
//    DeploymentUnitProcessingException failToResolveEjbRemoveForInterface(String ejbName);
//
//    /**
//     * Creates an exception indicating that it could not resolve corresponding method for home interface method on EJB
//     *
//     * @return a {@link DeploymentUnitProcessingException} for the error.
//     */
//    @Message(id = 205, value = "Could not resolve corresponding %s for home interface method %s on EJB %s")
//    DeploymentUnitProcessingException failToResolveMethodForHomeInterface(String ejbMethodName, Method method, String ejbName);

    /**
     * Creates an exception indicating the method is not implemented
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 206, value = "Not implemented yet")
    IllegalStateException methodNotImplemented();

//    /**
//     * Creates an exception indicating a class was attached to a view that is not an EJBObject or an EJBLocalObject
//     *
//     * @param aClass the attached class
//     * @return a {@link RuntimeException} for the error.
//     */
//    @Message(id = 207, value = "%s was attached to a view that is not an EJBObject or an EJBLocalObject")
//    RuntimeException classAttachToViewNotEjbObject(Class<?> aClass);
//
//    /**
//     * Creates an exception indicating invocation was not associated with an instance, primary key was null, instance may have been removed
//     *
//     * @return a {@link NoSuchEJBException} for the error.
//     */
//    @Message(id = 208, value = "Invocation was not associated with an instance, primary key was null, instance may have been removed")
//    NoSuchEJBException invocationNotAssociated();
//
//    /**
//     * Creates an exception indicating could not re-acquire lock for non-reentrant instance
//     *
//     * @return a {@link EJBException} for the error.
//     */
//    @Message(id = 209, value = "Could not re-acquire lock for non-reentrant instance %s")
//    EJBException failToReacquireLockForNonReentrant(ComponentInstance privateData);
//
//    /**
//     * Creates an exception indicating could not Could not find entity from method
//     *
//     * @return a {@link ObjectNotFoundException} for the error.
//     */
//    @Message(id = 210, value = "Could not find entity from %s with params %s")
//    ObjectNotFoundException couldNotFindEntity(Method finderMethod, String s);
//
//
//    /**
//     * Creates an exception indicating an invocation was not associated with an instance, primary key was null, instance may have been removed
//     *
//     * @return a {@link NoSuchEJBException} for the error.
//     */
//    @Message(id = 211, value = "Invocation was not associated with an instance, primary key was null, instance may have been removed")
//    NoSuchEJBException primaryKeyIsNull();
//
//    /**
//     * Creates an exception indicating an instance has been removed
//     *
//     * @return a {@link NoSuchEJBException} for the error.
//     */
//    @Message(id = 212, value = "Instance of %s with primary key %s has been removed")
//    NoSuchEntityException instanceWasRemoved(String componentName, Object primaryKey);

    /**
     * Creates an exception indicating unexpected component
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 213, value = "Unexpected component: %s component Expected %s")
    IllegalStateException unexpectedComponent(Component component, Class<?> entityBeanComponentClass);

    /**
     * Creates an exception indicating EjbJarConfiguration hasn't been set
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 214, value = "EjbJarConfiguration hasn't been set in %s Cannot create component create service for EJB %S")
    IllegalStateException ejbJarConfigNotBeenSet(ComponentCreateServiceFactory serviceFactory, String componentName);

//    /**
//     * Creates an exception indicating cannot find any resource adapter service for resource adapter
//     *
//     * @return a {@link IllegalStateException} for the error.
//     */
//    @Message(id = 215, value = "Cannot find any resource adapter service for resource adapter %s")
//    IllegalStateException failToFindResourceAdapter(String resourceAdapterName);
//
//    /**
//     * Creates an exception indicating No resource-adapter has been specified
//     *
//     * @return a {@link IllegalStateException} for the error.
//     */
//    @Message(id = 216, value = "No resource-adapter has been specified for %s")
//    IllegalStateException resourceAdapterNotSpecified(MessageDrivenComponent messageDrivenComponent);
//
//    /**
//     * Creates an exception indicating poolConfig cannot be null
//     *
//     * @return a {@link IllegalStateException} for the error.
//     */
//    @Message(id = 217, value = "PoolConfig cannot be null")
//    IllegalArgumentException poolConfigIsNull();

    /**
     * Creates an exception indicating poolConfig cannot be null or empty
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 218, value = "PoolConfig cannot be null or empty")
    IllegalStateException poolConfigIsEmpty();

//    /**
//     * Creates an exception indicating cannot invoke method in a session bean lifecycle method"
//     *
//     * @return a {@link IllegalStateException} for the error.
//     */
//    @Message(id = 219, value = "Cannot invoke %s in a session bean lifecycle method")
//    IllegalStateException failToInvokeMethodInSessionBeanLifeCycle(String method);

    /**
     * Creates an exception indicating can't add view class as local view since it's already marked as remote view for bean
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 220, value = "[EJB 3.1 spec, section 4.9.7] - Can't add view class: %s as local view since it's already marked as remote view for bean: %s")
    IllegalStateException failToAddClassToLocalView(String viewClassName, String ejbName);

    /**
     * Creates an exception indicating business interface type cannot be null
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 221, value = "Business interface type cannot be null")
    IllegalStateException businessInterfaceIsNull();

    /**
     * Creates an exception indicating Bean component does not have an ejb object
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 222, value = "Bean %s does not have an %s")
    IllegalStateException beanComponentMissingEjbObject(String componentName, String ejbLocalObject);

    /**
     * Creates an exception indicating EJB 3.1 FR 13.6.2.9 getRollbackOnly is not allowed with SUPPORTS attribute
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 223, value = "EJB 3.1 FR 13.6.2.9 getRollbackOnly is not allowed with SUPPORTS attribute")
    IllegalStateException getRollBackOnlyIsNotAllowWithSupportsAttribute();

    /**
     * Creates an exception indicating not a business method. Do not call non-public methods on EJB's
     *
     * @return a {@link EJBException} for the error.
     */
    @Message(id = 224, value = "Not a business method %s. Do not call non-public methods on EJB's")
    EJBException failToCallBusinessOnNonePublicMethod(Method method);

    /**
     * Creates an exception indicating component instance isn't available for invocation
     *
     * @return a {@link Exception} for the error.
     */
    @Message(id = 225, value = "Component instance isn't available for invocation: %s")
    Exception componentInstanceNotAvailable(InterceptorContext interceptorContext);

//    /**
//     * Creates an exception indicating Component with component class isn't a singleton component
//     *
//     * @return a {@link IllegalArgumentException} for the error.
//     */
//    @Message(id = 226, value = "Component %s with component class: %s isn't a singleton component")
//    IllegalArgumentException componentNotSingleton(Component component, Class<?> componentClass);
//
//    /**
//     * Creates an exception indicating a SingletonComponent cannot be null
//     *
//     * @return a {@link IllegalArgumentException} for the error.
//     */
//    @Message(id = 227, value = "SingletonComponent cannot be null")
//    IllegalArgumentException singletonComponentIsNull();

    /**
     * Creates an exception indicating could not obtain lock within the specified time
     *
     * @return a {@link ConcurrentAccessTimeoutException} for the error.
     */
    @Message(id = 228, value = "EJB 3.1 FR 4.3.14.1 concurrent access timeout on %s - could not obtain lock within %s %s")
    ConcurrentAccessTimeoutException failToObtainLock(String ejb, long value, TimeUnit timeUnit);

//    /**
//     * Creates an exception indicating it was unable to find method
//     *
//     * @return a {@link RuntimeException} for the error.
//     */
//    @Message(id = 229, value = "Unable to find method %s %s")
//    RuntimeException failToFindMethod(String name, String s);
//
//    /**
//     * Creates an exception indicating the timerService is not supported for Stateful session bean
//     *
//     * @return a {@link IllegalStateException} for the error.
//     */
//    @Message(id = 230, value = "TimerService is not supported for Stateful session bean %s")
//    IllegalStateException timerServiceNotSupportedForSFSB(String componentName);
//
//    /**
//     * Creates an exception indicating session id cannot be null
//     *
//     * @return a {@link IllegalArgumentException} for the error.
//     */
//    @Message(id = 231, value = "Session id cannot be null")
//    IllegalArgumentException sessionIdIsNull();
//
//    /**
//     * Creates an exception indicating stateful component cannot be null
//     *
//     * @return a {@link IllegalArgumentException} for the error.
//     */
//    @Message(id = 232, value = "Stateful component cannot be null")
//    IllegalArgumentException statefulComponentIsNull();
//
//    /**
//     * Creates an exception indicating it could not create session for Stateful bean
//     *
//     * @return a {@link RuntimeException} for the error.
//     */
//    @Message(id = 233, value = "Could not create session for Stateful bean %s")
//    RuntimeException failToCreateStatefulSessionBean(String beanName, @Cause Throwable e);

    /**
     * Creates an exception indicating session id hasn't been set for stateful component
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 234, value = "Session id hasn't been set for stateful component: %s")
    IllegalStateException statefulSessionIdIsNull(String componentName);

    /**
     * Creates an exception indicating @Remove method cannot be null
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 235, value = "@Remove method identifier cannot be null")
    IllegalArgumentException removeMethodIsNull();

    /**
     * Creates an exception indicating Component with component specified class: isn't a stateful component
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 236, value = "Component %s with component class: %s%n isn't a %s component")
    IllegalArgumentException componentNotInstanceOfSessionComponent(Component component, Class<?> componentClass, String type);

    /**
     * Creates an exception indicating both methodIntf and className are set
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 237, value = "both methodIntf and className are set on %s")
    IllegalArgumentException bothMethodIntAndClassNameSet(String componentName);

    /**
     * Creates an exception indicating EJB 3.1 PFD2 4.8.5.1.1 upgrading from read to write lock is not allowed
     *
     * @return a {@link IllegalLoopbackException} for the error.
     */
    @Message(id = 238, value = "EJB 3.1 PFD2 4.8.5.1.1 upgrading from read to write lock is not allowed")
    IllegalLoopbackException failToUpgradeToWriteLock();

    /**
     * Creates an exception indicating component cannot be null
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 239, value = "%s cannot be null")
    IllegalArgumentException componentIsNull(String name);

    /**
     * Creates an exception indicating Invocation context cannot be processed because it's not applicable for a method invocation
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 240, value = "Invocation context: %s cannot be processed because it's not applicable for a method invocation")
    IllegalArgumentException invocationNotApplicableForMethodInvocation(InvocationContext invocationContext);

    /**
     * Creates an exception EJB 3.1 PFD2 4.8.5.5.1 concurrent access timeout on invocation - could not obtain lock within
     *
     * @return a {@link ConcurrentAccessTimeoutException} for the error.
     */
    @Message(id = 241, value = "EJB 3.1 PFD2 4.8.5.5.1 concurrent access timeout on %s - could not obtain lock within %s")
    ConcurrentAccessTimeoutException concurrentAccessTimeoutException(String ejb, String s);

    /**
     * Creates an exception indicating Illegal lock type for component
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 242, value = "Illegal lock type %s on %s for component %s")
    IllegalStateException failToObtainLockIllegalType(LockType lockType, Method method, SingletonComponent lockableComponent);

    /**
     * Creates an exception indicating the inability to call the method as something is missing for the invocation.
     *
     * @param methodName the name of the method.
     * @param missing    the missing type.
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 243, value = "Cannot call %s, no %s is present for this invocation")
    IllegalStateException cannotCall(String methodName, String missing);


    /**
     * Creates an exception indicating No asynchronous invocation in progress
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 244, value = "No asynchronous invocation in progress")
    IllegalStateException noAsynchronousInvocationInProgress();

//    /**
//     * Creates an exception indicating method call is not allowed while dependency injection is in progress
//     *
//     * @return a {@link IllegalStateException} for the error.
//     */
//    @Message(id = 245, value = "%s is not allowed while dependency injection is in progress")
//    IllegalStateException callMethodNotAllowWhenDependencyInjectionInProgress(String method);


    /**
     * Creates an exception indicating the method is deprecated
     *
     * @return a {@link UnsupportedOperationException} for the error.
     */
    @Message(id = 246, value = "%s is deprecated")
    UnsupportedOperationException isDeprecated(String getEnvironment);

//    /**
//     * Creates an exception indicating getting parameters is not allowed on lifecycle callbacks
//     *
//     * @return a {@link IllegalStateException} for the error.
//     */
//    @Message(id = 247, value = "Getting parameters is not allowed on lifecycle callbacks")
//    IllegalStateException gettingParametersNotAllowLifeCycleCallbacks();
//
//    /**
//     * Creates an exception indicating method is not allowed in lifecycle callbacks (EJB 3.1 FR 4.6.1, 4.7.2, 4.8.6, 5.5.1)
//     *
//     * @return a {@link IllegalStateException} for the error.
//     */
//    @Message(id = 248, value = "%s is not allowed in lifecycle callbacks (EJB 3.1 FR 4.6.1, 4.7.2, 4.8.6, 5.5.1)")
//    IllegalStateException notAllowedInLifecycleCallbacks(String name);
//
//    /**
//     * Creates an exception indicating Setting parameters is not allowed on lifecycle callbacks
//     *
//     * @return a {@link IllegalStateException} for the error.
//     */
//    @Message(id = 249, value = "Setting parameters is not allowed on lifecycle callbacks")
//    IllegalStateException setParameterNotAllowOnLifeCycleCallbacks();
//
//    /**
//     * Creates an exception indicating Got wrong number of arguments
//     *
//     * @return a {@link IllegalArgumentException} for the error.
//     */
//    @Message(id = 250, value = "Got wrong number of arguments, expected %s, got %s on %s")
//    IllegalArgumentException wrongNumberOfArguments(int length, int length1, Method method);
//
//    /**
//     * Creates an exception indicating parameter has the wrong type
//     *
//     * @return a {@link IllegalArgumentException} for the error.
//     */
//    @Message(id = 251, value = "Parameter %s has the wrong type, expected %, got %s on %s")
//    IllegalArgumentException wrongParameterType(int i, Class<?> expectedType, Class<?> actualType, Method method);
//
//    /**
//     * Creates an exception indicating No current invocation context available
//     *
//     * @return a {@link IllegalStateException} for the error.
//     */
//    @Message(id = 252, value = "No current invocation context available")
//    IllegalStateException noCurrentContextAvailable();

    /**
     * Creates an exception indicating the method should be overridden
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 253, value = "Should be overridden")
    IllegalStateException shouldBeOverridden();

//    /**
//     * Creates an exception indicating could not find session bean with name
//     *
//     * @return a {@link DeploymentUnitProcessingException} for the error.
//     */
//    @Message(id = 254, value = "Could not find session bean with name %s")
//    DeploymentUnitProcessingException couldNotFindSessionBean(String beanName);

    /**
     * Creates an exception indicating <role-name> cannot be null or empty in <security-role-ref> for bean
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 255, value = "<role-name> cannot be null or empty in <security-role-ref>%nfor bean: %s")
    DeploymentUnitProcessingException roleNamesIsNull(String ejbName);

    /**
     * Creates an exception indicating Default interceptors cannot specify a method to bind to in ejb-jar.xml
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 256, value = "Default interceptors cannot specify a method to bind to in ejb-jar.xml")
    DeploymentUnitProcessingException defaultInterceptorsNotBindToMethod();

//    /**
//     * Creates an exception indicating Could not load component class
//     *
//     * @return a {@link DeploymentUnitProcessingException} for the error.
//     */
//    @Message(id = 257, value = "Could not load component class %s")
//    DeploymentUnitProcessingException failToLoadComponentClass(String componentClassName);

    /**
     * Creates an exception indicating Two ejb-jar.xml bindings for %s specify an absolute order
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 258, value = "Two ejb-jar.xml bindings for %s specify an absolute order")
    DeploymentUnitProcessingException twoEjbBindingsSpecifyAbsoluteOrder(String component);

    /**
     * Creates an exception indicating Could not find method specified referenced in ejb-jar.xml
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 259, value = "Could not find method %s.%s referenced in ejb-jar.xml")
    DeploymentUnitProcessingException failToFindMethodInEjbJarXml(String name, String methodName);

    /**
     * Creates an exception indicating More than one method found on class referenced in ejb-jar.xml. Specify the parameter types to resolve the ambiguity
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 260, value = "More than one method %s found on class %s referenced in ejb-jar.xml. Specify the parameter types to resolve the ambiguity")
    DeploymentUnitProcessingException multipleMethodReferencedInEjbJarXml(String methodName, String name);

    /**
     * Creates an exception indicating could not find method with parameter types referenced in ejb-jar.xml
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 261, value = "Could not find method %s.%s with parameter types %s referenced in ejb-jar.xml")
    DeploymentUnitProcessingException failToFindMethodWithParameterTypes(String name, String methodName, MethodParametersMetaData methodParams);

    /**
     * Creates an exception indicating could not load component class
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 262, value = "Could not load component class for component %s")
    DeploymentUnitProcessingException failToLoadComponentClass(@Cause Throwable t, String componentName);

    /**
     * Creates an exception indicating Could not load EJB view class
     *
     * @return a {@link RuntimeException} for the error.
     */
//    @Message(id = 263, value = "Could not load EJB view class ")
//    RuntimeException failToLoadEjbViewClass(@Cause Throwable e);


    /**
     * Creates an exception indicating Could not merge data
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 264, value = "Could not merge data for %s")
    DeploymentUnitProcessingException failToMergeData(String componentName, @Cause Throwable e);

    /**
     * Creates an exception indicating it could not load EJB class
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 265, value = "Could not load EJB class %s")
    DeploymentUnitProcessingException failToLoadEjbClass(String ejbClassName, @Cause Throwable e);

    /**
     * Creates an exception indicating only one annotation method is allowed on bean
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 266, value = "Only one %s method is allowed on bean %s")
    RuntimeException multipleAnnotationsOnBean(String annotationType, String ejbClassName);

    /**
     * Creates an exception indicating it could not determine type of corresponding implied EJB 2.x local interface (see EJB 3.1 21.4.5)
     * due to  multiple create* methods with different return types on home
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 267, value = "Could not determine type of corresponding implied EJB 2.x local interface (see EJB 3.1 21.4.5)%n due to multiple create* methods with different return types on home %s")
    DeploymentUnitProcessingException multipleCreateMethod(Class<?> localHomeClass);

    /**
     * Creates an exception indicating it Could not find EJB referenced by @DependsOn annotation
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 268, value = "Could not find EJB %s referenced by @DependsOn annotation in %s")
    DeploymentUnitProcessingException failToFindEjbRefByDependsOn(String annotationValue, String componentClassName);

    /**
     * Creates an exception indicating more than one EJB called referenced by @DependsOn annotation in Components
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 269, value = "More than one EJB called %s referenced by @DependsOn annotation in %s Components:%s")
    DeploymentUnitProcessingException failToCallEjbRefByDependsOn(String annotationValue, String componentClassName, Set<ComponentDescription> components);

    /**
     * Creates an exception indicating Async method does not return void or Future
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 270, value = "Async method %s does not return void or Future")
    DeploymentUnitProcessingException wrongReturnTypeForAsyncMethod(Method method);

    /**
     * Creates an exception indicating it could not load application exception class %s in ejb-jar.xml
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 271, value = "Could not load application exception class %s in ejb-jar.xml")
    DeploymentUnitProcessingException failToLoadAppExceptionClassInEjbJarXml(String exceptionClassName, @Cause Throwable e);

    /**
     * Creates an exception indicating the EJB entity bean implemented TimedObject but has a different
     * timeout method specified either via annotations or via the deployment descriptor.
     *
     * @return an {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 272, value = "EJB %s entity bean %s implemented TimedObject, but has a different timeout " +
            "method specified either via annotations or via the deployment descriptor")
    DeploymentUnitProcessingException invalidEjbEntityTimeout(String versionId, Class<?> componentClass);

    /**
     * Creates an exception indicating component does not have an EJB 2.x local interface
     *
     * @return an {@link RuntimeException} for the error.
     */
    @Message(id = 273, value = "%s does not have an EJB 2.x local interface")
    RuntimeException invalidEjbLocalInterface(String componentName);

    /**
     * Creates an exception indicating Local Home not allowed
     *
     * @return an {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 274, value = "Local Home not allowed for %s")
    DeploymentUnitProcessingException localHomeNotAllow(EJBComponentDescription description);

    /**
     * Creates an exception indicating Could not resolve corresponding ejbCreate or @Init method for home interface method on EJB
     *
     * @return an {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 275, value = "Could not resolve corresponding ejbCreate or @Init method for home interface method %s on EJB %s")
    DeploymentUnitProcessingException failToCallEjbCreateForHomeInterface(Method method, String ejbClassName);

    /**
     * Creates an exception indicating EJBComponent has not been set in the current invocation context
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 276, value = "EJBComponent has not been set in the current invocation context %s")
    IllegalStateException failToGetEjbComponent(InterceptorContext currentInvocationContext);

    /**
     * Creates an exception indicating Value cannot be null
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 277, value = "Value cannot be null")
    IllegalArgumentException valueIsNull();

    /**
     * Creates an exception indicating Cannot create class from a null schedule expression
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 278, value = "Cannot create %s from a null schedule expression")
    IllegalArgumentException invalidScheduleExpression(String name);

    /**
     * Creates an exception indicating second cannot be null in schedule expression
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 279, value = "Second cannot be null in schedule expression %s")
    IllegalArgumentException invalidScheduleExpressionSecond(ScheduleExpression schedule);

    /**
     * Creates an exception indicating Minute cannot be null in schedule expression
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 280, value = "Minute cannot be null in schedule expression %s")
    IllegalArgumentException invalidScheduleExpressionMinute(ScheduleExpression schedule);

    /**
     * Creates an exception indicating hour cannot be null in schedule expression
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 281, value = "Hour cannot be null in schedule expression %s")
    IllegalArgumentException invalidScheduleExpressionHour(ScheduleExpression schedule);

    /**
     * Creates an exception indicating day-of-month cannot be null in schedule expression
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 282, value = "day-of-month cannot be null in schedule expression %s")
    IllegalArgumentException invalidScheduleExpressionDayOfMonth(ScheduleExpression schedule);

    /**
     * Creates an exception indicating day-of-week cannot be null in schedule expression
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 283, value = "day-of-week cannot be null in schedule expression %s")
    IllegalArgumentException invalidScheduleExpressionDayOfWeek(ScheduleExpression schedule);

    /**
     * Creates an exception indicating Month cannot be null in schedule expression
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 284, value = "Month cannot be null in schedule expression %s")
    IllegalArgumentException invalidScheduleExpressionMonth(ScheduleExpression schedule);

    /**
     * Creates an exception indicating Year cannot be null in schedule expression
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 285, value = "Year cannot be null in schedule expression %s")
    IllegalArgumentException invalidScheduleExpressionYear(ScheduleExpression schedule);

    /**
     * Creates an exception indicating Invalid range value
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 286, value = "Invalid range value: %s")
    IllegalArgumentException invalidRange(String range);

    /**
     * Creates an exception indicating Invalid list expression
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 287, value = "Invalid list expression: %s")
    IllegalArgumentException invalidListExpression(String list);

    /**
     * Creates an exception indicating Invalid increment value
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 288, value = "Invalid increment value: %s")
    IllegalArgumentException invalidIncrementValue(String value);

    /**
     * Creates an exception indicating there are no valid seconds for expression
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 289, value = "There are no valid seconds for expression: %s")
    IllegalStateException invalidExpressionSeconds(String origValue);

    /**
     * Creates an exception indicating there are no valid minutes for expression
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 290, value = "There are no valid minutes for expression: %s")
    IllegalStateException invalidExpressionMinutes(String origValue);

    /**
     * Creates an exception indicating Invalid value it doesn't support values of specified types
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 291, value = "Invalid value: %s since %s doesn't support values of types %s")
    IllegalArgumentException invalidScheduleExpressionType(String value, String name, String type);

    /**
     * Creates an exception indicating A list value can only contain either a range or an individual value
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 292, value = "A list value can only contain either a range or an individual value. Invalid value: %s")
    IllegalArgumentException invalidListValue(String listItem);

    /**
     * Creates an exception indicating it could not parse schedule expression
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 293, value = "Could not parse: %s in schedule expression")
    IllegalArgumentException couldNotParseScheduleExpression(String origValue);

    /**
     * Creates an exception indicating invalid value range
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 294, value = "Invalid value: %s Valid values are between %s and %s")
    IllegalArgumentException invalidValuesRange(Integer value, int min, int max);

    /**
     * Creates an exception indicating invalid value for day-of-month
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 295, value = "Invalid value for day-of-month: %s")
    IllegalArgumentException invalidValueDayOfMonth(Integer value);

    /**
     * Creates an exception indicating relative day-of-month cannot be null or empty
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 296, value = "Relative day-of-month cannot be null or empty")
    IllegalArgumentException relativeDayOfMonthIsNull();

    /**
     * Creates an exception indicating is not relative value day-of-month
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 297, value = "%s is not a relative value")
    IllegalArgumentException invalidRelativeValue(String relativeDayOfMonth);

    /**
     * Creates an exception indicating value is null, cannot determine if it's relative
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 298, value = "Value is null, cannot determine if it's relative")
    IllegalArgumentException relativeValueIsNull();

//    /**
//     * Creates an exception indicating null timerservice cannot be registered"
//     *
//     * @return an {@link IllegalArgumentException} for the error.
//     */
//    @Message(id = 299, value = "null timerservice cannot be registered")
//    IllegalArgumentException timerServiceNotRegistered();
//
//    /**
//     * Creates an exception indicating the timer service is already registered
//     *
//     * @return an {@link IllegalArgumentException} for the error.
//     */
//    @Message(id = 300, value = "Timer service with timedObjectId: %s is already registered")
//    IllegalStateException timerServiceAlreadyRegistered(String timedObjectId);
//
//    /**
//     * Creates an exception indicating the null timedObjectId cannot be used for unregistering timerservice
//     *
//     * @return an {@link IllegalStateException} for the error.
//     */
//    @Message(id = 301, value = "null timedObjectId cannot be used for unregistering timerservice")
//    IllegalStateException timedObjectIdIsNullForUnregisteringTimerService();
//
//    /**
//     * Creates an exception indicating cannot unregister timer service because it's not registered"
//     *
//     * @return an {@link IllegalStateException} for the error.
//     */
//    @Message(id = 302, value = "Cannot unregister timer service with timedObjectId: %s because it's not registered")
//    IllegalStateException failToUnregisterTimerService(String timedObjectId);

    /**
     * Creates an exception indicating the invoker cannot be null
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 303, value = "Invoker cannot be null")
    IllegalArgumentException invokerIsNull();

//    /**
//     * Creates an exception indicating the transaction manager cannot be null
//     *
//     * @return an {@link IllegalArgumentException} for the error.
//     */
//    @Message(id = 304, value = "Transaction manager cannot be null")
//    IllegalArgumentException transactionManagerIsNull();

    /**
     * Creates an exception indicating the Executor cannot be null
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 305, value = "Executor cannot be null")
    IllegalArgumentException executorIsNull();

    /**
     * Creates an exception indicating the initialExpiration cannot be null while creating a timer
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 306, value = "initialExpiration cannot be null while creating a timer")
    IllegalArgumentException initialExpirationIsNullCreatingTimer();

    /**
     * Creates an exception indicating the value cannot be negative while creating a timer
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 307, value = "%s cannot be negative while creating a timer")
    IllegalArgumentException invalidInitialExpiration(String type);

    /**
     * Creates an exception indicating the expiration cannot be null while creating a single action timer
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 308, value = "expiration cannot be null while creating a single action timer")
    IllegalArgumentException expirationIsNull();

    /**
     * Creates an exception indicating the expiration.getTime() cannot be negative while creating a single action timer
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 309, value = "expiration.getTime() cannot be negative while creating a single action timer")
    IllegalArgumentException invalidExpirationActionTimer();

    /**
     * Creates an exception indicating duration cannot be negative while creating single action timer
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 310, value = "duration cannot be negative while creating single action timer")
    IllegalArgumentException invalidDurationActionTimer();

    /**
     * Creates an exception indicating Duration cannot negative while creating the timer
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 311, value = "Duration cannot negative while creating the timer")
    IllegalArgumentException invalidDurationTimer();

    /**
     * Creates an exception indicating the expiration date cannot be null while creating a timer
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 312, value = "Expiration date cannot be null while creating a timer")
    IllegalArgumentException expirationDateIsNull();

    /**
     * Creates an exception indicating the expiration.getTime() cannot be negative while creating a timer
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 313, value = "expiration.getTime() cannot be negative while creating a timer")
    IllegalArgumentException invalidExpirationTimer();

    /**
     * Creates an exception indicating the initial duration cannot be negative while creating timer
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 314, value = "Initial duration cannot be negative while creating timer")
    IllegalArgumentException invalidInitialDurationTimer();

    /**
     * Creates an exception indicating the interval cannot be negative while creating timer
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 315, value = "Interval cannot be negative while creating timer")
    IllegalArgumentException invalidIntervalTimer();

    /**
     * Creates an exception indicating the initial expiration date cannot be null while creating a timer
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 316, value = "initial expiration date cannot be null while creating a timer")
    IllegalArgumentException initialExpirationDateIsNull();

    /**
     * Creates an exception indicating the interval duration cannot be negative while creating timer
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 317, value = "interval duration cannot be negative while creating timer")
    IllegalArgumentException invalidIntervalDurationTimer();

    /**
     * Creates an exception indicating the creation of timers is not allowed during lifecycle callback of non-singleton EJBs
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 318, value = "Creation of timers is not allowed during lifecycle callback of non-singleton EJBs")
    IllegalStateException failToCreateTimerDoLifecycle();

    /**
     * Creates an exception indicating initial expiration is null
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 319, value = "initial expiration is null")
    IllegalArgumentException initialExpirationIsNull();

    /**
     * Creates an exception indicating the interval duration is negative
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 320, value = "interval duration is negative")
    IllegalArgumentException invalidIntervalDuration();

    /**
     * Creates an exception indicating the schedule is null
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 321, value = "schedule is null")
    IllegalArgumentException scheduleIsNull();

//    /**
//     * Creates an exception indicating it could not start transaction
//     *
//     * @return an {@link RuntimeException} for the error.
//     */
//    @Message(id = 322, value = "Could not start transaction")
//    RuntimeException failToStartTransaction(@Cause Throwable t);
//
//    /**
//     * Creates an exception indicating the transaction cannot be ended since no transaction is in progress
//     *
//     * @return an {@link IllegalStateException} for the error.
//     */
//    @Message(id = 323, value = "Transaction cannot be ended since no transaction is in progress")
//    IllegalStateException noTransactionInProgress();
//
//    /**
//     * Creates an exception indicating could not end transaction
//     *
//     * @return an {@link RuntimeException} for the error.
//     */
//    @Message(id = 324, value = "Could not end transaction")
//    RuntimeException failToEndTransaction(@Cause Throwable e);

    /**
     * Creates an exception indicating it cannot invoke timer service methods in lifecycle callback of non-singleton beans
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 325, value = "Cannot invoke timer service methods in lifecycle callback of non-singleton beans")
    IllegalStateException failToInvokeTimerServiceDoLifecycle();

    /**
     * Creates an exception indicating timer cannot be null
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 326, value = "Timer cannot be null")
    IllegalStateException timerIsNull();

    /**
     * Creates an exception indicating timer handles are only available for persistent timers
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 327, value = "%s Timer handles are only available for persistent timers.")
    IllegalStateException invalidTimerHandlersForPersistentTimers(String s);

    /**
     * Creates an exception indicating no more timeouts for timer
     *
     * @return an {@link NoMoreTimeoutsException} for the error.
     */
    @Message(id = 328, value = "No more timeouts for timer %s")
    NoMoreTimeoutsException noMoreTimeoutForTimer(TimerImpl timer);

    /**
     * Creates an exception indicating the timer is not a calendar based timer"
     *
     * @return an {@link IllegalStateException for the error.
     */
    @Message(id = 329, value = "Timer %s is not a calendar based timer")
    IllegalStateException invalidTimerNotCalendarBaseTimer(final TimerImpl timer);

    /**
     * Creates an exception indicating the Timer has expired
     *
     * @return an {@link NoSuchObjectLocalException} for the error.
     */
    @Message(id = 330, value = "Timer has expired")
    NoSuchObjectLocalException timerHasExpired();

    /**
     * Creates an exception indicating the timer was canceled
     *
     * @return an {@link NoSuchObjectLocalException} for the error.
     */
    @Message(id = 331, value = "Timer was canceled")
    NoSuchObjectLocalException timerWasCanceled();

//    /**
//     * Creates an exception indicating the timer is not persistent
//     *
//     * @return an {@link IllegalStateException} for the error.
//     */
//    @Message(id = 332, value = "Timer %s is not persistent")
//    IllegalStateException failToPersistTimer(TimerImpl timer);
//
//    /**
//     * Creates an exception indicating it could not register with tx for timer cancellation
//     *
//     * @return an {@link RuntimeException} for the error.
//     */
//    @Message(id = 333, value = "Could not register with tx for timer cancellation")
//    RuntimeException failToRegisterWithTxTimerCancellation(@Cause Throwable e);
//
//    /**
//     * Creates an exception indicating it could not deserialize info in timer
//     *
//     * @return an {@link RuntimeException} for the error.
//     */
//    @Message(id = 334, value = "Could not deserialize info in timer ")
//    RuntimeException failToDeserializeInfoInTimer(@Cause Throwable e);

    /**
     * Creates an exception indicating the Id cannot be null
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 335, value = "Id cannot be null")
    IllegalArgumentException idIsNull();

    /**
     * Creates an exception indicating Timed objectid cannot be null
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 336, value = "Timed objectid cannot be null")
    IllegalArgumentException timedObjectNull();

    /**
     * Creates an exception indicating the timer service cannot be null
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 337, value = "Timer service cannot be null")
    IllegalArgumentException timerServiceIsNull();

    /**
     * Creates an exception indicating the timerservice with timedObjectId is not registered
     *
     * @return an {@link EJBException} for the error.
     */
    @Message(id = 338, value = "Timerservice with timedObjectId: %s is not registered")
    EJBException timerServiceWithIdNotRegistered(String timedObjectId);

    /**
     * Creates an exception indicating the timer for handle is not active"
     *
     * @return an {@link NoSuchObjectLocalException} for the error.
     */
    @Message(id = 339, value = "Timer for handle: %s is not active")
    NoSuchObjectLocalException timerHandleIsNotActive(TimerHandle timerHandle);

//    /**
//     * Creates an exception indicating it could not find timeout method
//     *
//     * @return an {@link IllegalStateException} for the error.
//     */
//    @Message(id = 340, value = "Could not find timeout method: %s")
//    IllegalStateException failToFindTimeoutMethod(TimeoutMethod timeoutMethodInfo);

    /**
     * Creates an exception indicating it cannot invoke getTimeoutMethod on a timer which is not an auto-timer
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 341, value = "Cannot invoke getTimeoutMethod on a timer which is not an auto-timer")
    IllegalStateException failToInvokegetTimeoutMethod();

    /**
     * Creates an exception indicating it could not load declared class of timeout method
     *
     * @return an {@link RuntimeException} for the error.
     */
    @Message(id = 342, value = "Could not load declaring class: %s of timeout method")
    RuntimeException failToLoadDeclaringClassOfTimeOut(String declaringClass);

    /**
     * Creates an exception indicating it cannot invoke timeout method
     *
     * @return an {@link RuntimeException} for the error.
     */
    @Message(id = 343, value = "Cannot invoke timeout method because method %s is not a timeout method")
    RuntimeException failToInvokeTimeout(Method method);

    /**
     * Creates an exception indicating it could not create timer file store directory
     *
     * @return an {@link RuntimeException} for the error.
     */
    @Message(id = 344, value = "Could not create timer file store directory %s")
    RuntimeException failToCreateTimerFileStoreDir(File baseDir);

    /**
     * Creates an exception indicating timer file store directory does not exist"
     *
     * @return an {@link RuntimeException} for the error.
     */
    @Message(id = 345, value = "Timer file store directory %s does not exist")
    RuntimeException timerFileStoreDirNotExist(File baseDir);

    /**
     * Creates an exception indicating the timer file store directory is not a directory
     *
     * @return an {@link RuntimeException} for the error.
     */
    @Message(id = 346, value = "Timer file store directory %s is not a directory")
    RuntimeException invalidTimerFileStoreDir(File baseDir);

    /**
     * Creates an exception indicating EJB is enabled for security but doesn't have a security domain set
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 347, value = "EJB %s is enabled for security but doesn't have a security domain set")
    IllegalStateException invalidSecurityForDomainSet(String componentName);

    /**
     * Creates an exception indicating component configuration is not an EJB component"
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 348, value = "%s is not an EJB component")
    IllegalArgumentException invalidComponentConfiguration(String componentName);

    /**
     * Creates an exception indicating it could not load view class for ejb
     *
     * @return an {@link RuntimeException} for the error.
     */
    @Message(id = 349, value = "Could not load view class for ejb %s")
    RuntimeException failToLoadViewClassEjb(String beanName, @Cause Throwable e);

    /**
     * Creates an exception indicating the component named with component class is not an EJB component
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 350, value = "Component named %s with component class %s is not an EJB component")
    IllegalArgumentException invalidEjbComponent(String componentName, Class<?> componentClass);

//    /**
//     * Creates an exception indicating no timed object invoke for component
//     *
//     * @return an {@link StartException} for the error.
//     */
//    @Message(id = 351, value = "No timed object invoke for %s")
//    StartException failToInvokeTimedObject(EJBComponent component);
//
//    /**
//     * Creates an exception indicating TimerService is not started
//     *
//     * @return an {@link IllegalStateException} for the error.
//     */
//    @Message(id = 352, value = "TimerService is not started")
//    IllegalStateException failToStartTimerService();

    /**
     * Creates an exception indicating resourceBundle based descriptions are not supported
     *
     * @return an {@link UnsupportedOperationException} for the error.
     */
    @Message(id = 353, value = "ResourceBundle based descriptions of %s are not supported")
    UnsupportedOperationException resourceBundleDescriptionsNotSupported(String name);

    /**
     * Creates an exception indicating a runtime attribute is not marshallable
     *
     * @return an {@link UnsupportedOperationException} for the error.
     */
    @Message(id = 354, value = "Runtime attribute %s is not marshallable")
    UnsupportedOperationException runtimeAttributeNotMarshallable(String name);

//    /**
//     * Creates an exception indicating an invalid value for the specified element
//     *
//     * @return an {@link String} for the error.
//     */
//    @Message(id = 355, value = "Invalid value: %s for '%s' element %s")
//    String invalidValueForElement(String value, String element, Location location);

    /**
     * Creates an exception indicating EJB component type does not support pools
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 356, value = "EJB component type %s does not support pools")
    IllegalStateException invalidComponentType(String simpleName);

    /**
     * Creates an exception indicating Unknown EJBComponent type
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 357, value = "Unknown EJBComponent type %s")
    IllegalStateException unknownComponentType(EJBComponentType ejbComponentType);

//    /**
//     * Creates an exception indicating Method for view shouldn't be
//     * marked for both @PermitAll and @DenyAll at the same time
//     *
//     * @return an {@link IllegalStateException} for the error.
//     */
//    @Message(id = 358, value = "Method %s for view %s shouldn't be marked for both %s and %s at the same time")
//    IllegalStateException invalidSecurityAnnotation(Method componentMethod, String viewClassName, final String s, final String s1);
//
//    /**
//     * Creates an exception indicating method named with params not found on component class
//     *
//     * @return an {@link RuntimeException} for the error.
//     */
//    @Message(id = 359, value = "Method named %s with params %s not found on component class %s")
//    RuntimeException failToFindComponentMethod(String name, String s, Class<?> componentClass);

    /**
     * Creates an exception indicating the EJB method security metadata cannot be null
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 360, value = "EJB method security metadata cannot be null")
    IllegalArgumentException ejbMethodSecurityMetaDataIsNull();

    /**
     * Creates an exception indicating the view classname cannot be null or empty
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 361, value = "View classname cannot be null or empty")
    IllegalArgumentException viewClassNameIsNull();

    /**
     * Creates an exception indicating View method cannot be null
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 362, value = "View method cannot be null")
    IllegalArgumentException viewMethodIsNull();

    /**
     * Creates an exception indicating class cannot handle method of view class
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 363, value = "%s cannot handle method %s of view class %s.Expected view method to be %s on view class %s")
    IllegalStateException failProcessInvocation(String name, final Method invokedMethod, String viewClassOfInvokedMethod, Method viewMethod, String viewClassName);

    /**
     * Creates an exception indicating the Invocation on method is not allowed
     *
     * @return an {@link EJBAccessException} for the error.
     */
    @Message(id = 364, value = "Invocation on method: %s of bean: %s is not allowed")
    EJBAccessException invocationOfMethodNotAllowed(Method invokedMethod, String componentName);

    /**
     * Creates an exception indicating an unknown EJB Component description type
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 365, value = "Unknown EJB Component description type %s")
    IllegalArgumentException unknownComponentDescriptionType(Class<?> aClass);

    /**
     * Creates an exception indicating unknown attribute
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 366, value = "Unknown attribute %s")
    IllegalStateException unknownAttribute(String attributeName);

    /**
     * Creates an exception indicating Unknown operation
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 367, value = "Unknown operation %s")
    IllegalStateException unknownOperations(String opName);

    /**
     * Creates an exception indicating no EJB component registered for address
     *
     * @return an {@link String} for the error.
     */
    @Message(id = 368, value = "No EJB component registered for address %s")
    String noComponentRegisteredForAddress(PathAddress operationAddress);

    /**
     * Creates an exception indicating No EJB component is available for address
     *
     * @return an {@link String} for the error.
     */
    @Message(id = 369, value = "No EJB component is available for address %s")
    String noComponentAvailableForAddress(PathAddress operationAddress);

    /**
     * Creates an exception indicating EJB component for specified address is in invalid state
     *
     * @return an {@link String} for the error.
     */
    @Message(id = 370, value = "EJB component for address %s is in %n state %s, must be in state %s")
    String invalidComponentState(PathAddress operationAddress, ServiceController.State controllerState, ServiceController.State up);


//    /**
//     * Creates an exception indicating specified components is not an EJB component"
//     *
//     * @param componentName
//     * @return an {@link IllegalArgumentException} for the error.
//     */
//    @Message(id = 371, value = "%s is not an EJB component")
//    IllegalArgumentException invalidComponentIsNotEjbComponent(final String componentName);

    /**
     * Creates an exception indicating Component class has multiple @Timeout annotations
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 372, value = "Component class %s has multiple @Timeout annotations")
    DeploymentUnitProcessingException componentClassHasMultipleTimeoutAnnotations(Class<?> componentClass);

    /**
     * Creates an exception indicating the current component is not an EJB.
     *
     * @param component the component.
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 373, value = "Current component is not an EJB %s")
    IllegalStateException currentComponentNotAEjb(ComponentInstance component);

    /**
     * Creates an exception indicating the method invocation is not allowed in lifecycle methods.
     *
     * @param methodName the name of the method.
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 374, value = "%s not allowed in lifecycle methods")
    IllegalStateException lifecycleMethodNotAllowed(String methodName);

//    @Message(id = 375, value = "%s is not allowed in lifecycle methods of stateless session beans")
//    IllegalStateException lifecycleMethodNotAllowedFromStatelessSessionBean(String methodName);

    /**
     * Creates an exception indicating Cannot call getInvokedBusinessInterface when invoking through ejb object
     *
     * @param name type of object
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 376, value = "Cannot call %s when invoking through %s or %s")
    IllegalStateException cannotCall(String methodName, String name, String localName);

    @Message(id = 377, value = "%s is not allowed from stateful beans")
    IllegalStateException notAllowedFromStatefulBeans(String method);

    @Message(id = 378, value = "Failed to acquire a permit within %s %s")
    EJBException failedToAcquirePermit(long timeout, TimeUnit timeUnit);

    @Message(id = 379, value = "Acquire semaphore was interrupted")
    EJBException acquireSemaphoreInterrupted();


    /**
     * Creates an exception indicating the method is deprecated
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 380, value = "%s is deprecated")
    IllegalStateException isDeprecatedIllegalState(String getEnvironment);

//    @Message(id = 381, value = "Could not find method %s on entity bean")
//    RuntimeException couldNotFindEntityBeanMethod(String method);

    @Message(id = 382, value = "Could not determine ClassLoader for stub %s")
    RuntimeException couldNotFindClassLoaderForStub(String stub);

    /**
     * Creates an exception indicating that there was no message listener of the expected type
     * in the resource adapter
     *
     * @param messageListenerType The message listener type
     * @param resourceAdapterName The resource adapter name
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 383, value = "No message listener of type %s found in resource adapter %s")
    IllegalStateException unknownMessageListenerType(String resourceAdapterName, String messageListenerType);

    /**
     * Thrown when an EJB 2 EJB does not implement a method on an EJB 2
     *
     * @param method    The method
     * @param viewClass The view
     * @param ejb       The ejb
     */
    @Message(id = 384, value = "Could not find method %s from view %s on EJB class %s")
    DeploymentUnitProcessingException couldNotFindViewMethodOnEjb(final Method method, String viewClass, String ejb);

    /**
     * Creates and returns an exception indicating that the param named <code>paramName</code> cannot be null
     * or empty string.
     *
     * @param paramName The param name
     * @return an {@link IllegalArgumentException} for the exception
     */
    @Message(id = 385, value = "%s cannot be null or empty")
    IllegalArgumentException stringParamCannotBeNullOrEmpty(final String paramName);

    /**
     * Exception that is thrown when invoking remove while an EJB is in a transaction
     */
    @Message(id = 386, value = "EJB 4.6.4 Cannot remove EJB via EJB 2.x remove() method while participating in a transaction")
    RemoveException cannotRemoveWhileParticipatingInTransaction();

    @Message(id = 387, value = "Transaction propagation over IIOP is not supported")
    RemoteException transactionPropagationNotSupported();

    @Deprecated
    @Message(id = 388, value = "Cannot call method %s in afterCompletion callback")
    IllegalStateException cannotCallMethodInAfterCompletion(String methodName);

    /**
     * Exception thrown if a method cannot be invoked at the given time
     */
    @Message(id = 389, value = "Cannot call %s when state is %s")
    IllegalStateException cannotCallMethod(String methodName, String state);

    @Deprecated
    @Message(id = 390, value = "%s is already associated with serialization group %s")
    IllegalStateException existingSerializationGroup(Object key, Object group);

    @Deprecated
    @Message(id = 391, value = "%s is not compatible with serialization group %s")
    IllegalStateException incompatibleSerializationGroup(Object object, Object group);

    @Deprecated
    @Message(id = 392, value = "Cache entry %s is in use")
    IllegalStateException cacheEntryInUse(Object entry);

    @Deprecated
    @Message(id = 393, value = "Cache entry %s is not in use")
    IllegalStateException cacheEntryNotInUse(Object entry);

    @Deprecated
    @Message(id = 394, value = "Failed to acquire lock on %s")
    RuntimeException lockAcquisitionInterrupted(@Cause Throwable cause, Object id);

    @Deprecated
    @Message(id = 395, value = "%s is already a member of serialization group %s")
    IllegalStateException duplicateSerializationGroupMember(Object id, Object groupId);

    @Deprecated
    @Message(id = 396, value = "%s is not a member of serialization group %s")
    IllegalStateException missingSerializationGroupMember(Object id, Object groupId);

    @Deprecated
    @Message(id = 397, value = "%s already exists in cache")
    IllegalStateException duplicateCacheEntry(Object id);

    @Deprecated
    @Message(id = 398, value = "%s is missing from cache")
    IllegalStateException missingCacheEntry(Object id);

    @Message(id = 399, value = "Incompatible cache implementations in nested hierarchy")
    IllegalStateException incompatibleCaches();

    @Deprecated
    @Message(id = 400, value = "Failed to passivate %s")
    RuntimeException passivationFailed(@Cause Throwable cause, Object id);

    @Deprecated
    @Message(id = 401, value = "Failed to activate %s")
    RuntimeException activationFailed(@Cause Throwable cause, Object id);

    @Deprecated
    @Message(id = 402, value = "Failed to create passivation directory: %s")
    RuntimeException passivationDirectoryCreationFailed(String path);

    @Deprecated
    @Message(id = 403, value = "Failed to create passivation directory: %s")
    RuntimeException passivationPathNotADirectory(String path);

    @Deprecated
    @Message(id = 404, value = "Group creation context already exists")
    IllegalStateException groupCreationContextAlreadyExists();

    @Message(id = 405, value = "No EJB found with interface of type '%s' and name '%s' for binding %s")
    String ejbNotFound(String typeName, String beanName, String binding);

    @Message(id = 406, value = "No EJB found with interface of type '%s' for binding %s")
    String ejbNotFound(String typeName, String binding);

    @Message(id = 407, value = "More than one EJB found with interface of type '%s' and name '%s' for binding %s. Found: %s")
    String moreThanOneEjbFound(String typeName, String beanName, String binding, Set<EJBViewDescription> componentViews);

    @Message(id = 408, value = "More than one EJB found with interface of type '%s' for binding %s. Found: %s")
    String moreThanOneEjbFound(String typeName, String binding, Set<EJBViewDescription> componentViews);

    /**
     * Returns a {@link DeploymentUnitProcessingException} to indicate that the {@link org.jboss.ejb3.annotation.Clustered}
     * annotation cannot be used on a message driven bean
     *
     * @param unit               The deployment unit
     * @param componentName      The MDB component name
     * @param componentClassName The MDB component class name
     * @return the exception
     */
    @Deprecated
    @Message(id = 409, value = "@Clustered annotation cannot be used with message driven beans. %s failed since %s bean is marked with @Clustered on class %s")
    DeploymentUnitProcessingException clusteredAnnotationIsNotApplicableForMDB(final DeploymentUnit unit, final String componentName, final String componentClassName);

    /**
     * Returns a {@link DeploymentUnitProcessingException} to indicate that the {@link org.jboss.ejb3.annotation.Clustered}
     * annotation cannot be used on an entity bean
     *
     * @param unit               The deployment unit
     * @param componentName      The entity bean component name
     * @param componentClassName The entity bean component class name
     * @return the exception
     */
    @Deprecated
    @Message(id = 410, value = "@Clustered annotation cannot be used with entity beans. %s failed since %s bean is marked with @Clustered on class %s")
    DeploymentUnitProcessingException clusteredAnnotationIsNotApplicableForEntityBean(final DeploymentUnit unit, final String componentName, final String componentClassName);

    /**
     * Returns a {@link DeploymentUnitProcessingException} to indicate that the {@link org.jboss.ejb3.annotation.Clustered}
     * annotation is <b>currently</b> not supported on singleton EJB.
     *
     * @param unit               The deployment unit
     * @param componentName      The singleton bean component name
     * @param componentClassName The singleton bean component class name
     * @return  the exception
     */
    @Deprecated
    @Message(id = 411, value = "@Clustered annotation is currently not supported for singleton EJB. %s failed since %s bean is marked with @Clustered on class %s")
    DeploymentUnitProcessingException clusteredAnnotationNotYetImplementedForSingletonBean(final DeploymentUnit unit, final String componentName, final String componentClassName);

    /**
     * Returns a {@link DeploymentUnitProcessingException} to indicate that the {@link org.jboss.ejb3.annotation.Clustered}
     * annotation cannot be used on the EJB component represented by <code>componentName</code>
     *
     * @param unit               The deployment unit
     * @param componentName      The component name
     * @param componentClassName The component class name
     * @return the exception
     */
    @Deprecated
    @Message(id = 412, value = "%s failed since @Clustered annotation cannot be used for %s bean on class %s")
    DeploymentUnitProcessingException clusteredAnnotationIsNotApplicableForBean(final DeploymentUnit unit, final String componentName, final String componentClassName);




    /**
     * Exception thrown if the session-type of a session bean is not specified
     */
    @Message(id = 413, value = "<session-type> not specified for ejb %s. This must be present in ejb-jar.xml")
    DeploymentUnitProcessingException sessionTypeNotSpecified(String bean);


    /**
     * Creates an exception indicating Default interceptors specify an absolute ordering
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 414, value = "Default interceptors cannot specify an <interceptor-order> element in ejb-jar.xml")
    DeploymentUnitProcessingException defaultInterceptorsNotSpecifyOrder();

//    /**
//     * Creates an returns a {@link IllegalStateException} to indicate that a cache is not clustered
//     *
//     * @return the exception
//     */
//    @Message(id = 415, value = "Cache is not clustered")
//    IllegalStateException cacheIsNotClustered();

    /**
     * Creates and returns an exception indicating that the param named <code>paramName</code> cannot be null
     *
     * @param paramName The param name
     * @return an {@link IllegalArgumentException} for the exception
     */
    @Message(id = 416, value = "%s cannot be null")
    IllegalArgumentException paramCannotBeNull(final String paramName);

//    @Message(id = 417, value = "A GroupMembershipNotifier is already registered by the name of %s")
//    IllegalArgumentException groupMembershipNotifierAlreadyRegistered(final String groupName);
//
//    @Message(id = 418, value = "No GroupMembershipNotifier registered by the name of %s")
//    IllegalArgumentException groupMembershipNotifierNotRegistered(final String groupName);

    /**
     * Creates and returns an exception indicating that the pool name configured for a bean cannot be an empty string
     *
     * @param ejbName The EJB name
     * @return an {@link IllegalArgumentException} for the exception
     */
    @Message(id = 419, value = "Pool name cannot be empty string for bean %s")
    IllegalArgumentException poolNameCannotBeEmptyString(final String ejbName);

    /**
     * The user attempts to look up the ejb context in a war when no ejb context is active
     */
    @Message(id = 420, value = "No EjbContext available as no EJB invocation is active")
    IllegalStateException noEjbContextAvailable();
    @Message(id = 421, value = "Invocation cannot proceed as component is shutting down")
    EJBComponentUnavailableException componentIsShuttingDown();

//    @Message(id = 422, value = "Could not open message outputstream for writing to Channel")
//    IOException failedToOpenMessageOutputStream(@Cause Throwable e);

    @Message(id = 423, value = "Could not create session for stateful bean %s")
    RuntimeException failedToCreateSessionForStatefulBean(@Cause Exception e, String beanName);

//    @Message(id = 424, value = "No thread context classloader available")
//    IllegalStateException tcclNotAvailable();
//
//    @Message(id = 425, value = "Cannot write to null DataOutput")
//    IllegalArgumentException cannotWriteToNullDataOutput();
//
//    @Message(id = 426, value = "No client-mapping entries found for node %s in cluster %s")
//    IllegalStateException clientMappingMissing(String nodeName, String clusterName);
//
//    @Message(id = 427, value = "Could not load class")
//    RuntimeException classNotFoundException(@Cause ClassNotFoundException cnfe);
//
//    @Message(id = 428, value = "EJB module identifiers cannot be null")
//    IllegalArgumentException ejbModuleIdentifiersCannotBeNull();
//
//    @Message(id = 429, value = "MessageInputStream cannot be null")
//    IllegalArgumentException messageInputStreamCannotBeNull();
//
//    @Message(id = 430, value = "Unknown transaction request type %s")
//    IllegalArgumentException unknownTransactionRequestType(String txRequestType);
//
//    @Message(id = 431, value = "Could not close channel")
//    RuntimeException couldNotCloseChannel(@Cause IOException ioe);
//
//    @Message(id = 432, value = "No subordinate transaction present for xid %s")
//    RuntimeException noSubordinateTransactionPresentForXid(Xid xid);
//
//    @Message(id = 433, value = "Failed to register transaction synchronization")
//    RuntimeException failedToRegisterTransactionSynchronization(@Cause Exception e);
//
//    @Message(id = 434, value = "Failed to get current transaction")
//    RuntimeException failedToGetCurrentTransaction(@Cause Exception e);
//
//    @Message(id = 435, value = "Could not obtain lock on %s to passivate %s")
//    IllegalStateException couldNotObtainLockForGroup(String groupId, String groupMember);

    @Message(id = 436, value = "Unknown channel creation option type %s")
    IllegalArgumentException unknownChannelCreationOptionType(String optionType);

    @Message(id = 437, value = "Could not determine remote interface from home interface %s for bean %s")
    DeploymentUnitProcessingException couldNotDetermineRemoteInterfaceFromHome(final String homeClass, final String beanName);

    @Message(id = 438, value = "Could not determine local interface from local home interface %s for bean %s")
    DeploymentUnitProcessingException couldNotDetermineLocalInterfaceFromLocalHome(final String localHomeClass, final String beanName);

//    @Message(id = 439, value = "Unsupported marshalling version: %d")
//    IllegalArgumentException unsupportedMarshallingVersion(int version);
//
//    @Message(id = 440, value = "%s method %s must be public")
//    DeploymentUnitProcessingException ejbMethodMustBePublic(final String type, final Method method);

    @Message(id = 441, value = "EJB business method %s must be public")
    DeploymentUnitProcessingException ejbBusinessMethodMustBePublic(final Method method);

    @Message(id = 442, value = "Unexpected Error")
    @Signature(String.class)
    EJBException unexpectedError(@Cause Throwable cause);

    @Message(id = 443, value = "EJB 3.1 FR 13.3.3: BMT bean %s should complete transaction before returning.")
    String transactionNotComplete(String componentName);

    @Message(id = 444, value = "Timer service resource %s is not suitable for the target. Only a configuration with a single file-store and no other configured data-store is supported on target")
    String untransformableTimerService(PathAddress address);

    @Deprecated
    @Message(id = 445, value = "Detected asymmetric usage of cache")
    IllegalStateException asymmetricCacheUsage();

    /**
     * Creates an exception indicating that timer is active.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 446, value = "The timer %s is already active.")
    IllegalStateException timerIsActive(Timer timer);

    @Message(id = 447, value = "Transaction '%s' was already rolled back")
    RollbackException transactionAlreadyRolledBack(Transaction tx);

    @Message(id = 448, value = "Transaction '%s' is in unexpected state (%s)")
    EJBException transactionInUnexpectedState(Transaction tx, String txStatus);

    @Message(id = 449, value = "Timerservice API is not allowed on stateful session bean %s")
    String timerServiceMethodNotAllowedForSFSB(final String ejbComponent);

    @Message(id = 450, value = "Entity Beans are no longer supported, beans %s cannot be deployed")
    DeploymentUnitProcessingException entityBeansAreNotSupported(String beanName);

    @Message(id = 451, value = "Attribute '%s' is not supported on current version servers; it is only allowed if its value matches '%s'")
    OperationFailedException inconsistentAttributeNotSupported(String attributeName, String mustMatch);

//    @Message(id = 452, value = "Unexpected end of document")
//    XMLStreamException unexpectedEndOfDocument(@Param Location location);

    @LogMessage(level = ERROR)
    @Message(id = 453, value = "Failed to persist timer %s")
    void failedToPersistTimer(Timer timerid, @Cause Exception e);

    @Message(id = 454, value = "Only one instance on <container-transaction> with an ejb-name of * can be present.")
    DeploymentUnitProcessingException mustOnlyBeSingleContainerTransactionElementWithWildcard();

    @Message(id = 455, value = "<container-transaction> elements that use the wildcard EJB name * can only use a method name of *")
    DeploymentUnitProcessingException wildcardContainerTransactionElementsMustHaveWildcardMethodName();

    @LogMessage(level = ERROR)
    @Message(id = 456, value = "Failed to refresh timers for %s")
    void failedToRefreshTimers(String timedObjectId);

    @Message(id = 457, value = "Unexpected Error")
    @Signature(String.class)
    EJBTransactionRolledbackException unexpectedErrorRolledBack(@Cause Error error);

    //@LogMessage(level = ERROR)
    //@Message(id = 458, value = "Failure in caller transaction.")
    //void failureInCallerTransaction(@Cause Throwable cause);

    @Message(id = 459, value = "Module %s containing bean %s is not deployed in ear but it specifies resource adapter name '%s' in a relative format.")
    DeploymentUnitProcessingException relativeResourceAdapterNameInStandaloneModule(String module, String bean, String adapterName);

    /**
     * Logs a waring message that the current datasource configuration does not ensure consistency in a clustered environment.
     */
    @LogMessage(level = WARN)
    @Message(id = 460, value = "The transaction isolation need to be equal or stricter than READ_COMMITTED to ensure that the timer run once-and-only-once")
    void wrongTransactionIsolationConfiguredForTimer();

    /**
     * Transaction rollback after problems not successful
     */
    @LogMessage(level = ERROR)
    @Message(id = 461, value = "Update timer failed and it was not possible to rollback the transaction!")
    void timerUpdateFailedAndRollbackNotPossible(@Cause Throwable rbe);

    /**
     * Logs a warning message indicating that the database dialect can not detected automatically
     */
    @LogMessage(level = WARN)
    @Message(id = 462, value = "Unable to detect database dialect from connection metadata or JDBC driver name. Please configure this manually using the 'datasource' property in your configuration.  Known database dialect strings are %s")
    void jdbcDatabaseDialectDetectionFailed(String validDialects);

    @LogMessage(level = WARN)
    @Message(id = 463, value = "Invalid transaction attribute type %s on SFSB lifecycle method %s of class %s, valid types are REQUIRES_NEW and NOT_SUPPORTED. Method will be treated as NOT_SUPPORTED.")
    void invalidTransactionTypeForSfsbLifecycleMethod(TransactionAttributeType txAttr, MethodIdentifier method, Class<?> clazz);

    @Message(id = 464, value = "The \"" + EJB3SubsystemModel.DISABLE_DEFAULT_EJB_PERMISSIONS + "\" attribute may not be set to true")
    OperationFailedException disableDefaultEjbPermissionsCannotBeTrue();

    @Message(id = 465, value = "Invalid client descriptor configuration: 'profile' and 'remoting-ejb-receivers' cannot be used together")
    DeploymentUnitProcessingException profileAndRemotingEjbReceiversUsedTogether();

    @Message(id = 466, value = "Failed to process business interfaces for EJB class %s")
    DeploymentUnitProcessingException failedToProcessBusinessInterfaces(Class<?> ejbClass, @Cause Exception e);

    @Message(id = 467, value = "The request was rejected as the container is suspended")
    EJBComponentUnavailableException containerSuspended();

    @Message(id = 468, value = "Timer invocation failed")
    OperationFailedException timerInvocationFailed(@Cause Exception e);

    @Message(id = 469, value = "Indexed child resources can only be registered if the parent resource supports ordered children. The parent of '%s' is not indexed")
    IllegalStateException indexedChildResourceRegistrationNotAvailable(PathElement address);

//    @LogMessage(level = INFO)
//    @Message(id = 470, value = "Could not create a connection for cluster node %s in cluster %s")
//    void couldNotCreateClusterConnection(@Cause Throwable cause, String nodeName, String clusterName);

    @Message(id = 471, value = "RMI/IIOP Violation: %s%n")
    RuntimeException rmiIiopVoliation(String violation);

    @Message(id = 472, value = "Cannot obtain exception repository id for %s:%n%s")
    RuntimeException exceptionRepositoryNotFound(String name, String message);

    @LogMessage(level = INFO)
    @Message(id = 473, value = "JNDI bindings for session bean named '%s' in deployment unit '%s' are as follows:%s")
    void jndiBindings(final String ejbName, final DeploymentUnit deploymentUnit, final StringBuilder bindings);

    @LogMessage(level = ERROR)
    @Message(id = 474, value = "Attribute '%s' is not supported on current version servers; it is only allowed if its value matches '%s'. This attribute should be removed.")
    void logInconsistentAttributeNotSupported(String attributeName, String mustMatch);

    @LogMessage(level = INFO)
    @Message(id = 475, value = "MDB delivery started: %s,%s")
    void mdbDeliveryStarted(String appName, String componentName);

    @LogMessage(level = INFO)
    @Message(id = 476, value = "MDB delivery stopped: %s,%s")
    void mdbDeliveryStopped(String appName, String componentName);

    @Message(id = 477, value = "MDB delivery group is missing: %s")
    DeploymentUnitProcessingException missingMdbDeliveryGroup(String deliveryGroupName);

    @LogMessage(level = ERROR)
    @Message(id = 480, value = "Loaded timer (%s) for EJB (%s) and this node that is marked as being in a timeout. The original timeout may not have been processed. Please use graceful shutdown to ensure timeout tasks are finished before shutting down.")
    void loadedPersistentTimerInTimeout(String timer, String timedObject);

    @LogMessage(level = INFO)
    @Message(id = 481, value = "Strict pool %s is using a max instance size of %d (per class), which is derived from thread worker pool sizing.")
    void strictPoolDerivedFromWorkers(String name, int max);

    @LogMessage(level = INFO)
    @Message(id = 482, value = "Strict pool %s is using a max instance size of %d (per class), which is derived from the number of CPUs on this host.")
    void strictPoolDerivedFromCPUs(String name, int max);

    @Message(id = 483, value = "Attributes are mutually exclusive: %s, %s")
    XMLStreamException mutuallyExclusiveAttributes(@Param Location location, String attribute1, String attribute2);

//    @LogMessage(level = WARN)
//    @Message(id = 484, value = "Could not send a cluster removal message for cluster: (%s) to the client on channel %s")
//    void couldNotSendClusterRemovalMessage(@Cause Throwable cause, Group group, Channel channel);

    @LogMessage(level = WARN)
    @Message(id = 485, value = "Transaction type %s is unspecified for the %s method of the %s message-driven bean. It will be handled as NOT_SUPPORTED.")
    void invalidTransactionTypeForMDB(TransactionAttributeType transactionAttributeType, String methond, String componentName);

    @LogMessage(level = INFO)
    @Message(id = 486, value = "Parameter 'default-clustered-sfsb-cache' was defined for the 'add' operation for resource '%s'. " +
            "This parameter is deprecated and its previous behavior has been remapped to attribute 'default-sfsb-cache'. " +
            "As a result the 'default-sfsb-cache' attribute has been set to '%s' and the " +
            "'default-sfsb-passivation-disabled-cache' attribute has been set to '%s'.")
    void remappingCacheAttributes(String address, ModelNode defClustered, ModelNode passivationDisabled);

    @LogMessage(level = ERROR)
    @Message(id = 487, value = "Unexpected invocation state %s")
    void unexpectedInvocationState(int state);

    @Message(id = 488, value = "Unauthenticated (anonymous) access to this EJB method is not authorized")
    SecurityException ejbAuthenticationRequired();

    @LogMessage(level = ERROR)
    @Message(id = 489, value = "Timer %s not running as transaction could not be started")
    void timerNotRunning(@Cause  NotSupportedException e, TimerImpl timer);

    @Message(id = 490, value = "Multiple security domains not supported")
    DeploymentUnitProcessingException multipleSecurityDomainsDetected();

    @Message(id = 491, value = "The transaction begin request was rejected as the container is suspended")
    EJBException cannotBeginUserTransaction();

    @LogMessage(level = INFO)
    @Message(id = 492, value = "EJB subsystem suspension waiting for active transactions, %d transaction(s) remaining")
    void suspensionWaitingActiveTransactions(int activeTransactionCount);

    @LogMessage(level = INFO)
    @Message(id = 493, value = "EJB subsystem suspension complete")
    void suspensionComplete();

    @Message(id = 494, value = "Failed to obtain SSLContext")
    RuntimeException failedToObtainSSLContext(@Cause Exception cause);

    @LogMessage(level = WARN)
    @Message(id = 495, value = "Ignoring the persisted start or end date for scheduled expression of timer ID:%s as it is not valid : %s.")
    void scheduleExpressionDateFromTimerPersistenceInvalid(String timerId, String parserMessage);

    @Message(id = 496, value = "Could not create an instance of EJB client interceptor %s")
    DeploymentUnitProcessingException failedToCreateEJBClientInterceptor(@Cause Exception e, String ejbClientInterceptorClassName);

    @LogMessage(level = WARN)
    @Message(id = 497, value = "Failed to persist timer %s on startup. This is likely due to another cluster member making the same change, and should not affect operation.")
    void failedToPersistTimerOnStartup(TimerImpl activeTimer, @Cause  Exception e);

    @Message(id = 499, value = "Cannot read derived size - service %s unreachable")
    OperationFailedException cannotReadStrictMaxPoolDerivedSize(ServiceName serviceName);

    @LogMessage(level = ERROR)
    @Message(id = 500, value = "Legacy org.jboss.security.annotation.SecurityDomain annotation is used in class: %s, please use org.jboss.ejb3.annotation.SecurityDomain instead.")
    void legacySecurityDomainAnnotationIsUsed(String cls);

    @Message(id = 501, value = "Failed to activate MDB %s")
    RuntimeException failedToActivateMdb(String componentName, @Cause Exception e);

    @LogMessage(level = ERROR)
    @Message(id = 502, value = "Exception checking if timer %s should run")
    void exceptionCheckingIfTimerShouldRun(Timer timer, @Cause Exception e);

    @LogMessage(level = WARN)
    @Message(id = 503, value = "[EJB3.2 spec, section 5.6.4] Message Driven Bean 'onMessage' method can not be final (MDB: %s).")
    void mdbOnMessageMethodCantBeFinal(String className);

    @LogMessage(level = WARN)
    @Message(id = 504, value = "[EJB3.2 spec, section 5.6.4] Message Driven Bean 'onMessage' method can not be private (MDB: %s).")
    void mdbOnMessageMethodCantBePrivate(String className);

    @LogMessage(level = WARN)
    @Message(id = 505, value = "[EJB3.2 spec, section 5.6.4] Message Driven Bean 'onMessage' method can not be static (MDB: %s).")
    void mdbOnMessageMethodCantBeStatic(String className);

    @LogMessage(level = WARN)
    @Message(id = 506, value = "[EJB3.2 spec, section 5.6.2] Message Driven Bean can not have a 'finalize' method. (MDB: %s)")
    void mdbCantHaveFinalizeMethod(String className);

    @LogMessage(level = ERROR)
    @Message(id = 507, value = "Failed to persist timer's state %s. Timer has to be restored manually")
    void exceptionPersistPostTimerState(Timer timer, @Cause Exception e);

    @LogMessage(level = WARN)
    @Message(id = 508, value = "Failed to persist timer's state %s due to %s")
    void exceptionPersistTimerState(Timer timer, Exception e);
}

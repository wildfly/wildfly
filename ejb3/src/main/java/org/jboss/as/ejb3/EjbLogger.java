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

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ResourceInjectionTarget;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.entity.EntityBeanComponentInstance;
import org.jboss.as.ejb3.component.stateful.StatefulSessionComponentInstance;
import org.jboss.as.ejb3.deployment.DeploymentModuleIdentifier;
import org.jboss.as.ejb3.timerservice.TimerImpl;
import org.jboss.as.ejb3.tx.TimerTransactionRolledBackException;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.ejb.client.EJBLocator;
import org.jboss.ejb.client.SessionID;
import org.jboss.invocation.InterceptorContext;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jca.core.spi.rar.NotFoundException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;
import org.jboss.logging.Param;
import org.jboss.metadata.ejb.spec.SessionType;
import org.jboss.remoting3.Channel;

import javax.ejb.EJBException;
import javax.ejb.EJBTransactionRequiredException;
import javax.ejb.EJBTransactionRolledbackException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.RemoveException;
import javax.ejb.Timer;
import javax.ejb.TransactionAttributeType;
import javax.resource.ResourceException;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.transaction.RollbackException;
import javax.transaction.Transaction;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Date;

import static org.jboss.logging.Logger.Level.*;

/**
 * This module is using message IDs in the range 14100-14599. This file is using the subset 14100-14149 for
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
    @Message(id = 14143, value = "Timer %s is still active, skipping overlapping scheduled execution at: %s")
    void skipOverlappingInvokeTimeout(String id, Date scheduledTime);

    /**
     * Returns a {@link IllegalStateException} indicating that {@link org.jboss.jca.core.spi.rar.ResourceAdapterRepository}
     * was unavailable
     *
     * @return
     */
    @Message(id = 14144, value = "Resource adapter repository is not available")
    IllegalStateException resourceAdapterRepositoryUnAvailable();

    /**
     * Returns a {@link IllegalArgumentException} indicating that no {@link org.jboss.jca.core.spi.rar.Endpoint}
     * could be found for a resource adapter named <code>resourceAdapterName</code>
     *
     * @param resourceAdapterName The name of the resource adapter
     * @param notFoundException   The original exception cause
     * @return
     */
    @Message(id = 14145, value = "Could not find an Endpoint for resource adapter %s")
    IllegalArgumentException noSuchEndpointException(final String resourceAdapterName, @Cause NotFoundException notFoundException);

    /**
     * Returns a {@link IllegalStateException} indicating that the {@link org.jboss.jca.core.spi.rar.Endpoint}
     * is not available
     *
     * @param componentName The MDB component name
     * @return
     */
    @Message(id = 14146, value = "Endpoint is not available for message driven component %s")
    IllegalStateException endpointUnAvailable(String componentName);

    /**
     * Returns a {@link RuntimeException} indicating that the {@link org.jboss.jca.core.spi.rar.Endpoint}
     * for the message driven component, could not be deactivated
     *
     * @param componentName The message driven component name
     * @param cause         Original cause
     * @return
     */
    @Message(id = 14147, value = "Could not deactive endpoint for message driven component %s")
    RuntimeException failureDuringEndpointDeactivation(final String componentName, @Cause ResourceException cause);

    @Message(id = 14148, value = "")
    UnsupportedCallbackException unsupportedCallback(@Param Callback current);

    @Message(id = 14149, value = "Could not create an instance of cluster node selector %s for cluster %s")
    RuntimeException failureDuringLoadOfClusterNodeSelector(final String clusterNodeSelectorName, final String clusterName, @Cause Exception e);

    @LogMessage(level = WARN)
    @Message(id = 14150, value = "Failed to parse property %s due to %s")
    void failedToCreateOptionForProperty(String propertyName, String reason);

    @Message(id = 14151, value = "Could not find view %s for EJB %s")
    IllegalStateException viewNotFound(String viewClass, String ejbName);

    @Message(id = 14152, value = "Cannot perform asynchronous local invocation for component that is not a session bean")
    RuntimeException asyncInvocationOnlyApplicableForSessionBeans();

    @Message(id = 14153, value = "%s is not a Stateful Session bean in app: %s module: %s distinct-name: %s")
    IllegalArgumentException notStatefulSessionBean(String ejbName, String appName, String moduleName, String distinctName);

    @Message(id = 14154, value = "Failed to marshal EJB parameters")
    RuntimeException failedToMarshalEjbParameters(@Cause Exception e);

    @Message(id = 14155, value = "Unknown deployment - app name: %s module name: %s distinct name: %s")
    IllegalArgumentException unknownDeployment(String appName, String moduleName, String distinctName);

    @Message(id = 14156, value = "Could not find EJB %s in deployment [app: %s module: %s distinct-name: %s]")
    IllegalArgumentException ejbNotFoundInDeployment(String ejbName, String appName, String moduleName, String distinctName);

    @Message(id = 14157, value = "%s annotation is only valid on method targets")
    IllegalArgumentException annotationApplicableOnlyForMethods(String annotationName);

    @Message(id = 14158, value = "Method %s, on class %s, annotated with @javax.interceptor.AroundTimeout is expected to accept a single param of type javax.interceptor.InvocationContext")
    IllegalArgumentException aroundTimeoutMethodExpectedWithInvocationContextParam(String methodName, String className);

    @Message(id = 14159, value = "Method %s, on class %s, annotated with @javax.interceptor.AroundTimeout must return Object type")
    IllegalArgumentException aroundTimeoutMethodMustReturnObjectType(String methodName, String className);

    @Message(id = 14160, value = "Wrong tx on thread: expected %s, actual %s")
    IllegalStateException wrongTxOnThread(Transaction expected, Transaction actual);

    @Message(id = 14161, value = "Unknown transaction attribute %s on invocation %s")
    IllegalStateException unknownTxAttributeOnInvocation(TransactionAttributeType txAttr, InterceptorContext invocation);

    @Message(id = 14162, value = "Transaction is required for invocation %s")
    EJBTransactionRequiredException txRequiredForInvocation(InterceptorContext invocation);

    @Message(id = 14163, value = "Transaction present on server in Never call (EJB3 13.6.2.6)")
    EJBException txPresentForNeverTxAttribute();

    @LogMessage(level = ERROR)
    @Message(id = 14164, value = "Failed to set transaction for rollback only")
    void failedToSetRollbackOnly(@Cause Exception e);

    @Message(id = 14165, value = "View interface cannot be null")
    IllegalArgumentException viewInterfaceCannotBeNull();

    @Message(id = 14166, value = "Cannot call getEjbObject before the object is associated with a primary key")
    IllegalStateException cannotCallGetEjbObjectBeforePrimaryKeyAssociation();

    @Message(id = 14167, value = "Cannot call getEjbLocalObject before the object is associated with a primary key")
    IllegalStateException cannotCallGetEjbLocalObjectBeforePrimaryKeyAssociation();

    @Message(id = 14168, value = "Could not load view class for component %s")
    RuntimeException failedToLoadViewClassForComponent(@Cause Exception e, String componentName);

    @Message(id = 14169, value = "Entities can not be created for %s bean since no create method is available.")
    IllegalStateException entityCannotBeCreatedDueToMissingCreateMethod(String beanName);

    @Message(id = 14170, value = "%s is not an entity bean component")
    IllegalArgumentException notAnEntityBean(Component component);

    @Message(id = 14171, value = "Instance for PK [%s] already registered")
    IllegalStateException instanceAlreadyRegisteredForPK(Object primaryKey);

    @Message(id = 14172, value = "Instance [%s] not found in cache")
    IllegalStateException entityBeanInstanceNotFoundInCache(EntityBeanComponentInstance instance);

    @Message(id = 14173, value = "Illegal call to EJBHome.remove(Object) on a session bean")
    RemoveException illegalCallToEjbHomeRemove();

    @Message(id = 14174, value = "EJB 3.1 FR 13.6.2.8 setRollbackOnly is not allowed with SUPPORTS transaction attribute")
    IllegalStateException setRollbackOnlyNotAllowedForSupportsTxAttr();

    @Message(id = 14175, value = "Cannot call getPrimaryKey on a session bean")
    EJBException cannotCallGetPKOnSessionBean();

    @Message(id = 14176, value = "Singleton beans cannot have EJB 2.x views")
    RuntimeException ejb2xViewNotApplicableForSingletonBeans();

    @Message(id = 14177, value = "ClassTable %s cannot find a class for class index %d")
    ClassNotFoundException classNotFoundInClassTable(String classTableName, int index);

    @Message(id = 14178, value = "Bean %s does not have an EJBLocalObject")
    IllegalStateException ejbLocalObjectUnavailable(String beanName);

    @Message(id = 14179, value = "[EJB 3.1 spec, section 14.1.1] Class: %s cannot be marked as an application exception because it is not of type java.lang.Exception")
    IllegalArgumentException cannotBeApplicationExceptionBecauseNotAnExceptionType(Class klass);

    @Message(id = 14180, value = "[EJB 3.1 spec, section 14.1.1] Exception class: %s cannot be marked as an application exception because it is of type java.rmi.RemoteException")
    IllegalArgumentException rmiRemoteExceptionCannotBeApplicationException(Class klass);

    @Message(id = 14181, value = "%s annotation is allowed only on classes. %s is not a class")
    RuntimeException annotationOnlyAllowedOnClass(String annotationName, AnnotationTarget incorrectTarget);

    @Message(id = 14182, value = "Bean %s specifies @Remote annotation, but does not implement 1 interface")
    DeploymentUnitProcessingException beanWithRemoteAnnotationImplementsMoreThanOneInterface(Class beanClass);

    @Message(id = 14183, value = "Bean %s specifies @Local annotation, but does not implement 1 interface")
    DeploymentUnitProcessingException beanWithLocalAnnotationImplementsMoreThanOneInterface(Class beanClass);

    @Message(id = 14184, value = "Could not analyze remote interface for %s")
    RuntimeException failedToAnalyzeRemoteInterface(@Cause Exception e, String beanName);

    @Message(id = 14185, value = "Exception while parsing %s")
    DeploymentUnitProcessingException failedToParse(@Cause Exception e, String filePath);

    @Message(id = 14186, value = "Failed to install management resources for %s")
    DeploymentUnitProcessingException failedToInstallManagementResource(@Cause Exception e, String componentName);

    @Message(id = 14187, value = "Could not load view %s")
    RuntimeException failedToLoadViewClass(@Cause Exception e, String viewClassName);

    @Message(id = 14188, value = "Could not determine type of ejb-ref %s for injection target %s")
    DeploymentUnitProcessingException couldNotDetermineEjbRefForInjectionTarget(String ejbRefName, ResourceInjectionTarget injectionTarget);

    @Message(id = 14189, value = "Could not determine type of ejb-local-ref %s for injection target %s")
    DeploymentUnitProcessingException couldNotDetermineEjbLocalRefForInjectionTarget(String ejbLocalRefName, ResourceInjectionTarget injectionTarget);

    @Message(id = 14190, value = "@EJB injection target %s is invalid. Only setter methods are allowed")
    IllegalArgumentException onlySetterMethodsAllowedToHaveEJBAnnotation(MethodInfo methodInfo);

    @Message(id = 14191, value = "@EJB attribute 'name' is required for class level annotations. Class: %s")
    DeploymentUnitProcessingException nameAttributeRequiredForEJBAnnotationOnClass(String className);

    @Message(id = 14192, value = "@EJB attribute 'beanInterface' is required for class level annotations. Class: %s")
    DeploymentUnitProcessingException beanInterfaceAttributeRequiredForEJBAnnotationOnClass(String className);

    @Message(id = 14193, value = "Module hasn't been attached to deployment unit %s")
    IllegalStateException moduleNotAttachedToDeploymentUnit(DeploymentUnit deploymentUnit);

    @Message(id = 14194, value = "EJB 3.1 FR 5.4.2 MessageDrivenBean %s does not implement 1 interface nor specifies message listener interface")
    DeploymentUnitProcessingException mdbDoesNotImplementNorSpecifyMessageListener(ClassInfo beanClass);

    @Message(id = 14195, value = "Unknown session bean type %s")
    IllegalArgumentException unknownSessionBeanType(String sessionType);

    @Message(id = 14196, value = "More than one method found with name %s on %s")
    DeploymentUnitProcessingException moreThanOneMethodWithSameNameOnComponent(String methodName, Class componentClass);

    @Message(id = 14197, value = "Unknown EJB locator type %s")
    RuntimeException unknownEJBLocatorType(EJBLocator locator);

    @Message(id = 14198, value = "Could not create CORBA object for %s")
    RuntimeException couldNotCreateCorbaObject(@Cause Exception cause, EJBLocator locator);

    @Message(id = 14199, value = "Provided locator %s was not for EJB %s")
    IllegalArgumentException incorrectEJBLocatorForBean(EJBLocator locator, String beanName);

    @Message(id = 14200, value = "Failed to lookup java:comp/ORB")
    IOException failedToLookupORB();

    @Message(id = 14201, value = "%s is not an ObjectImpl")
    IOException notAnObjectImpl(Class type);

    @Message(id = 14202, value = "Message endpoint %s has already been released")
    UnavailableException messageEndpointAlreadyReleased(MessageEndpoint messageEndpoint);

    @Message(id = 14203, value = "Cannot handle client version %s")
    RuntimeException ejbRemoteServiceCannotHandleClientVersion(byte version);

    @Message(id = 14204, value = "Could not find marshaller factory for marshaller strategy %s")
    RuntimeException failedToFindMarshallerFactoryForStrategy(String marshallerStrategy);

    @Message(id = 14205, value = "%s is not an EJB component")
    IllegalArgumentException notAnEJBComponent(Component component);

    @Message(id = 14206, value = "Could not load method param class %s of timeout method")
    RuntimeException failedToLoadTimeoutMethodParamClass(@Cause Exception cause, String className);

    @Message(id = 14207, value = "Timer invocation failed, invoker is not started")
    IllegalStateException timerInvocationFailedDueToInvokerNotBeingStarted();

    @Message(id = 14208, value = "Could not load timer with id %s")
    NoSuchObjectLocalException timerNotFound(String timerId);

    @Message(id = 14209, value = "Invalid value for second: %s")
    IllegalArgumentException invalidValueForSecondInScheduleExpression(String value);

    @Message(id = 14210, value = "Timer invocation failed, transaction rolled back")
    TimerTransactionRolledBackException timerInvocationRolledBack();

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

    // Don't add message ids greater that 14299!!! If you need more first check what EjbMessages is
    // using and take more (lower) numbers from the available range for this module. If the range for the module is
    // all used, go to https://community.jboss.org/docs/DOC-16810 and allocate another block for this subsystem

}

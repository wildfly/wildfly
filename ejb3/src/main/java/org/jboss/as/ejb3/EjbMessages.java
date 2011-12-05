package org.jboss.as.ejb3;

import java.io.File;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.ejb.ConcurrentAccessTimeoutException;
import javax.ejb.EJBAccessException;
import javax.ejb.EJBException;
import javax.ejb.IllegalLoopbackException;
import javax.ejb.LockType;
import javax.ejb.NoMoreTimeoutsException;
import javax.ejb.NoSuchEJBException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.ObjectNotFoundException;
import javax.ejb.RemoveException;
import javax.ejb.ScheduleExpression;
import javax.ejb.TimerHandle;
import javax.interceptor.InvocationContext;
import javax.naming.Context;
import javax.xml.stream.Location;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentCreateServiceFactory;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.ejb3.component.messagedriven.MessageDrivenComponent;
import org.jboss.as.ejb3.concurrency.LockableComponent;
import org.jboss.as.ejb3.subsystem.deployment.EJBComponentType;
import org.jboss.as.ejb3.timerservice.CalendarTimer;
import org.jboss.as.ejb3.timerservice.TimerImpl;
import org.jboss.as.ejb3.timerservice.persistence.TimeoutMethod;
import org.jboss.as.ejb3.timerservice.spi.MultiTimeoutMethodTimedObjectInvoker;
import org.jboss.as.naming.context.NamespaceContextSelector;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.ejb.client.SessionID;
import org.jboss.invocation.InterceptorContext;
import org.jboss.logging.Cause;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;
import org.jboss.metadata.ejb.spec.MethodParametersMetaData;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.StartException;

/**
 * Date: 19.10.2011
 *
 * @author <a href="mailto:Flemming.Harms@gmail.com">Flemming Harms</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface EjbMessages {

    /**
     * The default messages.
     */
    EjbMessages MESSAGES = Messages.getBundle(EjbMessages.class);

    /**
     * Creates an exception indicating it could not find the EJB with specific id
     *
     * @param sessionId the name of the integration.
     *
     * @return a {@link NoSuchEJBException} for the error.
     */
    @Message(id = 14300, value = "Could not find EJB with id %s")
    NoSuchEJBException couldNotFindEjb(SessionID sessionId);

    /**
     * Creates an exception indicating it a component was not set on the InterceptorContext
     *
     * @param context the context.
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 14301, value = "Component not set in InterceptorContext: %s")
    IllegalStateException componentNotSetInInterceptor(InterceptorContext context);

    /**
     * Creates an exception indicating the method was called with null in the name
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14302, value = "Method name cannot be null")
    IllegalArgumentException methodeNameIsNull();

    /**
     * Creates an exception indicating the bean home interface was not set
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 14303, value = "Bean %s does not have a Home interface")
    IllegalStateException beanHomeInterfaceIsNull(String componentName);

    /**
     * Creates an exception indicating the bean local home interface was not set
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 14304, value = "Bean %s does not have a Local Home interface")
    IllegalStateException beanLocalHomeInterfaceIsNull(String componentName);

    /**
     * Creates an exception indicating the getRollBackOnly was called on none container-managed transaction
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 14305, value = "EJB 3.1 FR 13.6.1 Only beans with container-managed transaction demarcation " +
            "can use getRollbackOnly.")
    IllegalStateException failToCallgetRollbackOnly();

    /**
     * Creates an exception indicating the getRollBackOnly not allowed without a transaction
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 14306, value = "getRollbackOnly() not allowed without a transaction.")
    IllegalStateException failToCallgetRollbackOnlyOnNoneTransaction();

    /**
     * Creates an exception indicating the call getRollBackOnly not allowed after transaction is completed
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 14307, value = "getRollbackOnly() not allowed after transaction is completed (EJBTHREE-1445)")
    IllegalStateException failToCallgetRollbackOnlyAfterTxcompleted();

    /**
     * Creates an exception indicating the call isBeanManagedTransaction is not allowed without bean-managed transaction
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 14308, value = "EJB 3.1 FR 4.3.3 & 5.4.5 Only beans with bean-managed transaction demarcation can use this method.")
    IllegalStateException failToCallIsBeanManagedTransaction();

    /**
     * Creates an exception indicating the call lookup was call with an empty jndi name
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 14309, value = "jndi name cannot be null during lookup")
    IllegalArgumentException jndiNameCannotBeNull();

    /**
     * Creates an exception indicating the NamespaceContextSelector was not set
     *
     * @param name the jndi name
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 14310, value = "No NamespaceContextSelector available, cannot lookup %s")
    IllegalArgumentException noNamespaceContextSelectorAvailable(String name);

    /**
     * Creates an exception indicating the NamespaceContextSelector was not set
     *
     * @param name the jndi name
     * @param e    cause of the exeception
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 14311, value = " Could not lookup jndi name: %s")
    RuntimeException failToLookupJNDI(String name, @Cause Throwable e);

    /**
     * Creates an exception indicating the namespace was wrong
     *
     * @param name the jndi name
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14312, value = "Cannot lookup jndi name: %s since it" +
            " doesn't belong to java:app, java:module, java:comp or java:global namespace")
    IllegalArgumentException failToLookupJNDINameSpace(String name);

    /**
     * Creates an exception indicating it failed to lookup the namespace
     *
     * @param namespaceContextSelector
     * @param jndiContext              the jndi context it was looked up on
     * @param ne                       cause of the exeception
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14313, value = "Could not lookup jndi name: %s in context: %s")
    IllegalArgumentException failToLookupStrippedJNDI(NamespaceContextSelector namespaceContextSelector, Context jndiContext, @Cause Throwable ne);

    /**
     * Creates an exception indicating setRollBackOnly was called on none CMB
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 14314, value = "EJB 3.1 FR 13.6.1 Only beans with container-managed transaction demarcation " +
            "can use setRollbackOnly.")
    IllegalStateException failToCallSetRollbackOnlyOnNoneCMB();

    /**
     * Creates an exception indicating setRollBackOnly was without a transaction
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 14315, value = "setRollbackOnly() not allowed without a transaction.")
    IllegalStateException failToCallSetRollbackOnlyWithNoTx();

    /**
     * Creates an exception indicating EjbJarConfiguration cannot be null
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14316, value = "EjbJarConfiguration cannot be null")
    IllegalArgumentException EjbJarConfigurationIsNull();

    /**
     * Creates an exception indicating the security roles is null
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14317, value = "Cannot set security roles to null")
    IllegalArgumentException SecurityRolesIsNull();

    /**
     * Creates an exception indicating the classname was null or empty
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14318, value = "Classname cannot be null or empty: %s")
    IllegalArgumentException classnameIsNull(String className);

    /**
     * Creates an exception indicating it can't set null roles for the class
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14319, value = "Cannot set null roles for class %s")
    IllegalArgumentException setRolesForClassIsNull(String className);

    /**
     * Creates an exception indicating EJB method identifier cannot be null while setting roles on method
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14320, value = "EJB method identifier cannot be null while setting roles on method")
    IllegalArgumentException ejbMethodIsNull();

    /**
     * Creates an exception indicating roles cannot be null while setting roles on method
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14321, value = "Roles cannot be null while setting roles on method: %s")
    IllegalArgumentException rolesIsNull(EJBMethodIdentifier ejbMethodIdentifier);

    /**
     * Creates an exception indicating EJB method identifier cannot be null while setting roles on view type
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14322, value = "EJB method identifier cannot be null while setting roles on view type: %s")
    IllegalArgumentException ejbMethodIsNullForViewType(MethodIntf viewType);

    /**
     * Creates an exception indicating roles cannot be null while setting roles on view type
     *
     * @param viewType
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14323, value = "Roles cannot be null while setting roles on view type: %s")
    IllegalArgumentException rolesIsNullOnViewType(final MethodIntf viewType);

    /**
     * Creates an exception indicating roles cannot be null while setting roles on view type and method"
     *
     * @param viewType
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14324, value = "Roles cannot be null while setting roles on view type: %s and method: %s")
    IllegalArgumentException rolesIsNullOnViewTypeAndMethod(MethodIntf viewType, EJBMethodIdentifier ejbMethodIdentifier);

    /**
     * Creates an exception indicating it cannot link from a null or empty security role
     *
     * @param fromRole role it link from
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14325, value = "Cannot link from a null or empty security role: %s")
    IllegalArgumentException failToLinkFromEmptySecurityRole(String fromRole);

    /**
     * Creates an exception indicating it cannot link to a null or empty security role:
     *
     * @param toRole role it link to
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14326, value = "Cannot link to a null or empty security role: %s")
    IllegalArgumentException failToLinkToEmptySecurityRole(String toRole);

    /**
     * Creates an exception indicating that the EjbJarConfiguration was not found as an attachment in deployment unit
     *
     * @param deploymentUnit
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 14327, value = "EjbJarConfiguration not found as an attachment in deployment unit: %s")
    DeploymentUnitProcessingException ejbJarConfigNotFound(DeploymentUnit deploymentUnit);

    /**
     * Creates an exception indicating the component view instance is not available in interceptor context
     *
     * @param context
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 14328, value = "ComponentViewInstance not available in interceptor context: %s")
    IllegalStateException componentViewNotAvailableInContext(InterceptorContext context);

    /**
     * Creates an exception indicating it fail to call the timeout method
     *
     * @param method
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 14329, value = "Unknown timeout method %s")
    RuntimeException failToCallTimeOutMethod(Method method);

    /**
     * Creates an exception indicating timeout method was not set for the component
     *
     * @param componentName
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14330, value = "Component %s does not have a timeout method")
    IllegalArgumentException componentTimeoutMethodNotSet(String componentName);

    /**
     * Creates an exception indicating no resource adapter registered with resource adapter name
     *
     * @param resourceAdapterName
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 14331, value = "No resource adapter registered with resource adapter name %s")
    IllegalStateException unknownResourceAdapter(String resourceAdapterName);

    /**
     * Creates an exception indicating multiple resource adapter was registered
     *
     * @param resourceAdapterName
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 14332, value = "found more than one RA registered as %s")
    IllegalStateException multipleResourceAdapterRegistered(String resourceAdapterName);

    /**
     * Creates an exception indicating security is not enabled
     *
     * @return a {@link UnsupportedOperationException} for the error.
     */
    @Message(id = 14333, value = "Security is not enabled")
    UnsupportedOperationException securityNotEnabled();

    /**
     * Creates an exception indicating it fail to complete task before time out
     *
     * @return a {@link TimeoutException} for the error.
     */
    @Message(id = 14334, value = "Task did not complete in %s  %S")
    TimeoutException failToCompleteTaskBeforeTimeOut(long timeout, TimeUnit unit);

    /**
     * Creates an exception indicating the task was cancelled
     *
     * @return a {@link TimeoutException} for the error.
     */
    @Message(id = 14335, value = "Task was cancelled")
    CancellationException taskWasCancelled();


    /**
     * Creates an exception indicating that it could not resolve ejbRemove method for interface method on EJB
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 14336, value = "Could not resolve ejbRemove method for interface method on EJB %s")
    DeploymentUnitProcessingException failToResolveEjbRemoveForInterface(String ejbName);

    /**
     * Creates an exception indicating that it could not resolve corresponding method for home interface method on EJB
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 14337, value = "Could not resolve corresponding %s for home interface method %s on EJB %s")
    DeploymentUnitProcessingException failToResolveMethodForHomeInterface(String ejbMethodName, Method method, String ejbName);


    /**
     * Creates an exception indicating the method is not implemented
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 14338, value = "Not implemented yet")
    IllegalStateException methodNotImplemented();

    /**
     * Creates an exception indicating a class was attached to a view that is not an EJBObject or a EJBLocalObject
     *
     * @param aClass the attached class
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 14339, value = "%s was attached to a view that is not an EJBObject or a EJBLocalObject")
    RuntimeException classAttachToViewNotEjbObject(Class<?> aClass);

    /**
     * Creates an exception indicating invocation was not associated with an instance, primary key was null, instance may have been removed
     *
     * @return a {@link NoSuchEJBException} for the error.
     */
    @Message(id = 14340, value = "Invocation was not associated with an instance, primary key was null, instance may have been removed")
    NoSuchEJBException invocationNotAssociated();

    /**
     * Creates an exception indicating could not re-acquire lock for non-reentrant instance
     *
     * @return a {@link EJBException} for the error.
     */
    @Message(id = 14341, value = "Could not re-acquire lock for non-reentrant instance %s")
    EJBException failToReacquireLockForNonReentrant(ComponentInstance privateData);

    /**
     * Creates an exception indicating could not Could not find entity from method
     *
     * @return a {@link ObjectNotFoundException} for the error.
     */
    @Message(id = 14342, value = "Could not find entity from %s with params %s")
    ObjectNotFoundException couldNotFindEntity(Method finderMethod, String s);


    /**
     * Creates an exception indicating a invocation was not associated with an instance, primary key was null, instance may have been removed
     *
     * @return a {@link NoSuchEJBException} for the error.
     */
    @Message(id = 14343, value = "Invocation was not associated with an instance, primary key was null, instance may have been removed")
    NoSuchEJBException primaryKeyIsNull();

    /**
     * Creates an exception indicating a instance has been removed
     *
     * @return a {@link NoSuchEJBException} for the error.
     */
    @Message(id = 14344, value = "Instance of %s with primary key %s has been removed")
    NoSuchEJBException instaceWasRemoved(String componentName, Object primaryKey);

    /**
     * Creates an exception indicating unexpected component
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 14345, value = "Unexpected component: %s component Expected %s")
    IllegalStateException unexpectedComponent(Component component, Class<?> entityBeanComponentClass);

    /**
     * Creates an exception indicating EjbJarConfiguration hasn't been set
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 14346, value = "EjbJarConfiguration hasn't been set in %s Cannot create component create service for EJB %S")
    IllegalStateException ejbJarConfigNotBeenSet(ComponentCreateServiceFactory serviceFactory, String componentName);

    /**
     * Creates an exception indicating cannot find any resource adapter service for resource adapter
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 14347, value = "Cannot find any resource adapter service for resource adapter %s")
    IllegalStateException failToFindResourceAdapter(String resourceAdapterName);

    /**
     * Creates an exception indicating No resource-adapter has been specified
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 14348, value = "No resource-adapter has been specified for %s")
    IllegalStateException resourceAdapterNotSpecified(MessageDrivenComponent messageDrivenComponent);

    /**
     * Creates an exception indicating poolConfig cannot be null
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 14349, value = "PoolConfig cannot be null")
    IllegalArgumentException poolConfigIsNull();

    /**
     * Creates an exception indicating poolConfig cannot be null or empty
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 14350, value = "PoolConfig cannot be null or empty")
    IllegalStateException poolConfigIsEmpty();

    /**
     * Creates an exception indicating cannot invoke method in a session bean lifecycle method"
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 14351, value = "Cannot invoke %s in a session bean lifecycle method")
    IllegalStateException failToInvokeMethodInSessionBeanLifeCycle(String method);

    /**
     * Creates an exception indicating can't add view class as local view since it's already marked as remote view for bean
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 14352, value = "[EJB 3.1 spec, section 4.9.7] - Can't add view class: %s as local view since it's already marked as remote view for bean: %s")
    IllegalStateException failToAddClassToLocalView(String viewClassName, String ejbName);

    /**
     * Creates an exception indicating business interface type cannot be null
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 14353, value = "Business interface type cannot be null")
    IllegalStateException businessInterfaceIsNull();

    /**
     * Creates an exception indicating Bean component does not have an ejb object
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 14354, value = "Bean %s does not have an %s")
    IllegalStateException beanComponentMissingEjbObject(String componentName, String ejbLocalObject);

    /**
     * Creates an exception indicating EJB 3.1 FR 13.6.2.9 getRollbackOnly is not allowed with SUPPORTS attribute
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 14355, value = "EJB 3.1 FR 13.6.2.9 getRollbackOnly is not allowed with SUPPORTS attribute")
    IllegalStateException getRollBackOnlyIsNotAllowWithSupportsAttribute();

    /**
     * Creates an exception indicating not a business method. Do not call non-public methods on EJB's
     *
     * @return a {@link EJBException} for the error.
     */
    @Message(id = 14356, value = "Not a business method %s. Do not call non-public methods on EJB's")
    EJBException failToCallBusinessOnNonePublicMethod(Method method);

    /**
     * Creates an exception indicating component instance isn't available for invocation
     *
     * @return a {@link Exception} for the error.
     */
    @Message(id = 14357, value = "Component instance isn't available for invocation: %s")
    Exception componentInstanceNotAvailable(InterceptorContext interceptorContext);

    /**
     * Creates an exception indicating Component with component class isn't a singleton component
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14358, value = "Component %s with component class: %s isn't a singleton component")
    IllegalArgumentException componentNotSingleton(Component component, Class<?> componentClass);

    /**
     * Creates an exception indicating a SingletonComponent cannot be null
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14359, value = "SingletonComponent cannot be null")
    IllegalArgumentException singletonComponentIsNull();

    /**
     * Creates an exception indicating could not obtain lock within the specified time
     *
     * @return a {@link ConcurrentAccessTimeoutException} for the error.
     */
    @Message(id = 14360, value = "EJB 3.1 FR 4.3.14.1 concurrent access timeout on %s - could not obtain lock within %s %s")
    ConcurrentAccessTimeoutException failToObtainLock(InterceptorContext context, long value, TimeUnit timeUnit);

    /**
     * Creates an exception indicating it was unable to find method
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 14361, value = "Unable to find method %s %s")
    RuntimeException failToFindMethod(String name, String s);

    /**
     * Creates an exception indicating the timerService is not supported for Stateful session bean
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 14362, value = "TimerService is not supported for Stateful session bean %s")
    IllegalStateException timerServiceNotSupportedForSFSB(String componentName);

    /**
     * Creates an exception indicating session id cannot be null
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14363, value = "Session id cannot be null")
    IllegalArgumentException sessionIdIsNull();

    /**
     * Creates an exception indicating stateful component cannot be null
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14364, value = "Stateful component cannot be null")
    IllegalArgumentException statefulComponentIsNull();

    /**
     * Creates an exception indicating it could not create session for Stateful bean
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 14365, value = "Could not create session for Stateful bean %s")
    RuntimeException failToCreateStatefulSessionBean(String beanName, @Cause Throwable e);

    /**
     * Creates an exception indicating session id hasn't been set for stateful component
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14366, value = "Session id hasn't been set for stateful component: %s")
    IllegalStateException statefulSessionIdIsNull(String componentName);

    /**
     * Creates an exception indicating @Remove method cannot be null
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14367, value = "@Remove method identifier cannot be null")
    IllegalArgumentException removeMethodIsNull();

    /**
     * Creates an exception indicating Component with component specified class: isn't a stateful component
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14368, value = "Component %s with component class: %s\n isn't a %s component")
    IllegalArgumentException componentNotInstanceOfSessionComponent(Component component, Class<?> componentClass, String type);

    /**
     * Creates an exception indicating both methodIntf and className are set
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14369, value = "both methodIntf and className are set on %s")
    IllegalArgumentException bothMethodIntAndClassNameSet(String componentName);

    /**
     * Creates an exception indicating EJB 3.1 PFD2 4.8.5.1.1 upgrading from read to write lock is not allowed
     *
     * @return a {@link IllegalLoopbackException} for the error.
     */
    @Message(id = 14370, value = "EJB 3.1 PFD2 4.8.5.1.1 upgrading from read to write lock is not allowed")
    IllegalLoopbackException failToUpgradeToWriteLock();

    /**
     * Creates an exception indicating component cannot be null
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14371, value = "%s cannot be null")
    IllegalArgumentException componentIsNull(String name);

    /**
     * Creates an exception indicating Invocation context cannot be processed because it's not applicable for a method invocation
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14372, value = "Invocation context: %s cannot be processed because it's not applicable for a method invocation")
    IllegalArgumentException invocationNotApplicableForMethodInvocation(InvocationContext invocationContext);

    /**
     * Creates an exception EJB 3.1 PFD2 4.8.5.5.1 concurrent access timeout on invocation - could not obtain lock within
     *
     * @return a {@link ConcurrentAccessTimeoutException} for the error.
     */
    @Message(id = 14373, value = "EJB 3.1 PFD2 4.8.5.5.1 concurrent access timeout on %s - could not obtain lock within %s")
    ConcurrentAccessTimeoutException concurrentAccessTimeoutException(InvocationContext invocationContext, String s);

    /**
     * Creates an exception indicating Illegal lock type for component
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 14374, value = "Illegal lock type %s on %s for component %s")
    IllegalStateException failToObtainLockIllegalType(LockType lockType, Method method, LockableComponent lockableComponent);

    /**
     * Creates an exception indicating the inability to call the method as something is missing for the invocation.
     *
     * @param methodName the name of the method.
     * @param missing    the missing type.
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 14377, value = "Cannot call %s, no %s is present for this invocation")
    IllegalStateException cannotCall(String methodName, String missing);


    /**
     * Creates an exception indicating No asynchronous invocation in progress
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 14379, value = "No asynchronous invocation in progress")
    IllegalStateException noAsynchronousInvocationInProgress();

    /**
     * Creates an exception indicating method call is not allowed while dependency injection is in progress
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 14380, value = "%s is not allowed while dependency injection is in progress")
    IllegalStateException callMethodNotAllowWhenDependencyInjectionInProgress(String method);


    /**
     * Creates an exception indicating the method is deprecated
     *
     * @return a {@link UnsupportedOperationException} for the error.
     */
    @Message(id = 14384, value = "%s is deprecated")
    UnsupportedOperationException isDeprecated(String getEnvironment);

    /**
     * Creates an exception indicating getting parameters is not allowed on lifecycle callbacks
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 14385, value = "Getting parameters is not allowed on lifecycle callbacks")
    IllegalStateException gettingParametersNotAllowLifeCycleCallbacks();

    /**
     * Creates an exception indicating method is not allowed in lifecycle callbacks (EJB 3.1 FR 4.6.1, 4.7.2, 4.8.6, 5.5.1)
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 14386, value = "%s is not allowed in lifecycle callbacks (EJB 3.1 FR 4.6.1, 4.7.2, 4.8.6, 5.5.1)")
    IllegalStateException notAllowedInLifecycleCallbacks(String name);

    /**
     * Creates an exception indicating Setting parameters is not allowed on lifecycle callbacks
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 14387, value = "Setting parameters is not allowed on lifecycle callbacks")
    IllegalStateException setParameterNotAllowOnLifeCycleCallbacks();

    /**
     * Creates an exception indicating Got wrong number of arguments
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14388, value = "Got wrong number of arguments, expected %s, got %s on %s")
    IllegalArgumentException wrongNumberOfArguments(int length, int length1, Method method);

    /**
     * Creates an exception indicating parameter has the wrong type
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14389, value = "Parameter %s has the wrong type, expected %, got %s on %s")
    IllegalArgumentException wrongParameterType(int i, Class<?> expectedType, Class<?> actualType, Method method);

    /**
     * Creates an exception indicating No current invocation context available
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 14390, value = "No current invocation context available")
    IllegalStateException noCurrentContextAvailable();

    /**
     * Creates an exception indicating the method should be overridden
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 14391, value = "Should be overridden")
    IllegalStateException shouldBeOverridden();

    /**
     * Creates an exception indicating could not find session bean with name
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 14392, value = "Could not find session bean with name %s")
    DeploymentUnitProcessingException couldNotFindSessionBean(String beanName);

    /**
     * Creates an exception indicating <role-name> cannot be null or empty in <security-role-ref> for bean
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 14393, value = "<role-name> cannot be null or empty in <security-role-ref>\nfor bean: %s")
    DeploymentUnitProcessingException roleNamesIsNull(String ejbName);

    /**
     * Creates an exception indicating Default interceptors cannot specify a method to bind to in ejb-jar.xml
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 14394, value = "Default interceptors cannot specify a method to bind to in ejb-jar.xml")
    DeploymentUnitProcessingException defaultInterceptorsNotBindToMethod();

    /**
     * Creates an exception indicating Could not load component class
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 14395, value = "Could not load component class %s")
    DeploymentUnitProcessingException failToLoadComponentClass(String componentClassName);

    /**
     * Creates an exception indicating Two ejb-jar.xml bindings for %s specify an absolute order
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 14396, value = "Two ejb-jar.xml bindings for %s specify an absolute order")
    DeploymentUnitProcessingException twoEjbBindingsSpecifyAbsoluteOrder(String component);

    /**
     * Creates an exception indicating Could not find method specified referenced in ejb-jar.xml
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 14397, value = "Could not find method %s.%s referenced in ejb-jar.xml")
    DeploymentUnitProcessingException failToFindMethodInEjbJarXml(String name, String methodName);

    /**
     * Creates an exception indicating More than one method found on class referenced in ejb-jar.xml. Specify the parameter types to resolve the ambiguity
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 14398, value = "More than one method %s found on class %s referenced in ejb-jar.xml. Specify the parameter types to resolve the ambiguity")
    DeploymentUnitProcessingException multipleMethodReferencedInEjbJarXml(String methodName, String name);

    /**
     * Creates an exception indicating could not find method with parameter types referenced in ejb-jar.xml
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 14399, value = "Could not find method %s.%s with parameter types %s referenced in ejb-jar.xml")
    DeploymentUnitProcessingException failToFindMethodWithParameterTypes(String name, String methodName, MethodParametersMetaData methodParams);

    /**
     * Creates an exception indicating Could not load component class
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 14400, value = "Could not load component class")
    DeploymentUnitProcessingException failToLoadComponentClass(@Cause Throwable t);

    /**
     * Creates an exception indicating Could not load EJB view class
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 14401, value = "Could not load EJB view class ")
    RuntimeException failToLoadEjbViewClass(@Cause Throwable e);


    /**
     * Creates an exception indicating Could not merge data
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 14402, value = "Could not merge data for %s")
    DeploymentUnitProcessingException failToMergeData(String componentName, @Cause Throwable e);

    /**
     * Creates an exception indicating it could not load EJB class
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 14403, value = "Could not load EJB class %s")
    DeploymentUnitProcessingException failToLoadEjbClass(String ejbClassName, @Cause Throwable e);

    /**
     * Creates an exception indicating only one annotation method is allowed on bean
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 14404, value = "Only one %s method is allowed on bean %s")
    RuntimeException multipleAnnotationsOnBean(String annotationType, String ejbClassName);

    /**
     * Creates an exception indicating it could not determine type of corresponding implied EJB 2.x local interface (see EJB 3.1 21.4.5)
     * due to  multiple create* methods with different return types on home
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 14405, value = "Could not determine type of corresponding implied EJB 2.x local interface (see EJB 3.1 21.4.5)\n due to multiple create* methods with different return types on home %s")
    DeploymentUnitProcessingException multipleCreateMethod(Class localHomeClass);

    /**
     * Creates an exception indicating it Could not find EJB referenced by @DependsOn annotation
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 14406, value = "Could not find EJB %s referenced by @DependsOn annotation in %s")
    DeploymentUnitProcessingException failToFindEjbRefByDependsOn(String annotationValue, String componentClassName);

    /**
     * Creates an exception indicating more than one EJB called referenced by @DependsOn annotation in Components
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 14407, value = "More than one EJB called %s referenced by @DependsOn annotation in %s Components:%s")
    DeploymentUnitProcessingException failToCallEjbRefByDependsOn(String annotationValue, String componentClassName, Set<ComponentDescription> components);

    /**
     * Creates an exception indicating Async method does not return void or Future
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 14408, value = "Async method %s does not return void or Future")
    DeploymentUnitProcessingException wrongReturnTypeForAsyncMethod(Method method);

    /**
     * Creates an exception indicating it could not load application exception class %s in ejb-jar.xml
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 14409, value = "Could not load application exception class %s in ejb-jar.xml")
    DeploymentUnitProcessingException failToLoadAppExceptionClassInEjbJarXml(String exceptionClassName, @Cause Throwable e);

    /**
     * Creates an exception indicating the EJB entity bean implemented TimedObject but has a different
     * timeout method specified either via annotations or via the deployment descriptor.
     *
     * @return an {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 14410, value = "EJB %s entity bean %s implemented TimedObject, but has a different timeout " +
            "method specified either via annotations or via the deployment descriptor")
    DeploymentUnitProcessingException invalidEjbEntityTimeout(String versionId, Class<?> componentClass);

    /**
     * Creates an exception indicating component does not have a EJB 2.x local interface
     *
     * @return an {@link RuntimeException} for the error.
     */
    @Message(id = 14411, value = "% does not have a EJB 2.x local interface")
    RuntimeException invalidEjbLocalInterface(String componentName);

    /**
     * Creates an exception indicating Local Home not allowed
     *
     * @return an {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 14412, value = "Local Home not allowed for %s")
    DeploymentUnitProcessingException localHomeNotAllow(EJBComponentDescription description);

    /**
     * Creates an exception indicating Could not resolve corresponding ejbCreate or @Init method for home interface method on EJB
     *
     * @return an {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 14413, value = "Could not resolve corresponding ejbCreate or @Init method for home interface method %s on EJB %s")
    DeploymentUnitProcessingException failToCallEjbCreateForHomeInterface(Method method, String ejbClassName);

    /**
     * Creates an exception indicating EJBComponent has not been set in the current invocation context
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14414, value = "EJBComponent has not been set in the current invocation context %s")
    IllegalStateException failToGetEjbComponent(InterceptorContext currentInvocationContext);

    /**
     * Creates an exception indicating Value cannot be null
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14415, value = "Value cannot be null")
    IllegalArgumentException valueIsNull();

    /**
     * Creates an exception indicating Cannot create class from a null schedule expression
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14416, value = "Cannot create %s from a null schedule expression")
    IllegalArgumentException invalidScheduleExpression(String name);

    /**
     * Creates an exception indicating second cannot be null in schedule expression
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14417, value = "Second cannot be null in schedule expression %s")
    IllegalArgumentException invalidScheduleExpressionSecond(ScheduleExpression schedule);

    /**
     * Creates an exception indicating Minute cannot be null in schedule expression
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14418, value = "Minute cannot be null in schedule expression %s")
    IllegalArgumentException invalidScheduleExpressionMinute(ScheduleExpression schedule);

    /**
     * Creates an exception indicating hour cannot be null in schedule expression
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14419, value = "Hour cannot be null in schedule expression %s")
    IllegalArgumentException invalidScheduleExpressionHour(ScheduleExpression schedule);

    /**
     * Creates an exception indicating day-of-month cannot be null in schedule expression
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14420, value = "day-of-month cannot be null in schedule expression %s")
    IllegalArgumentException invalidScheduleExpressionDayOfMonth(ScheduleExpression schedule);

    /**
     * Creates an exception indicating day-of-week cannot be null in schedule expression
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14421, value = "day-of-week cannot be null in schedule expression %s")
    IllegalArgumentException invalidScheduleExpressionDayOfWeek(ScheduleExpression schedule);

    /**
     * Creates an exception indicating Month cannot be null in schedule expression
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14422, value = "Month cannot be null in schedule expression %s")
    IllegalArgumentException invalidScheduleExpressionMonth(ScheduleExpression schedule);

    /**
     * Creates an exception indicating Year cannot be null in schedule expression
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14423, value = "Year cannot be null in schedule expression %s")
    IllegalArgumentException invalidScheduleExpressionYear(ScheduleExpression schedule);

    /**
     * Creates an exception indicating Invalid range value
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14424, value = "Invalid range value: %s")
    IllegalArgumentException invalidRange(String range);

    /**
     * Creates an exception indicating Invalid list expression
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14425, value = "Invalid list expression: %s")
    IllegalArgumentException invalidListExpression(String list);

    /**
     * Creates an exception indicating Invalid increment value
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14426, value = "Invalid increment value: %s")
    IllegalArgumentException invalidIncrementValue(String value);

    /**
     * Creates an exception indicating there are no valid seconds for expression
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14427, value = "There are no valid seconds for expression: %s")
    IllegalStateException invalidExpressionSeconds(String origValue);

    /**
     * Creates an exception indicating there are no valid minutes for expression
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14428, value = "There are no valid minutes for expression: %s")
    IllegalStateException invalidExpressionMinutes(String origValue);

    /**
     * Creates an exception indicating Invalid value it doesn't support values of specified types
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14429, value = "Invalid value: %s since %s doesn't support values of types %s")
    IllegalArgumentException invalidScheduleExpressionType(String value, String name, String type);

    /**
     * Creates an exception indicating A list value can only contain either a range or an individual value
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14430, value = "A list value can only contain either a range or an individual value. Invalid value: %s")
    IllegalArgumentException invalidListValue(String listItem);

    /**
     * Creates an exception indicating it could not parse schedule expression
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14431, value = "Could not parse: %s in schedule expression")
    IllegalArgumentException couldNotParseScheduleExpression(String origValue);

    /**
     * Creates an exception indicating invalid value range
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14432, value = "Invalid value: %s Valid values are between %s and %s")
    IllegalArgumentException invalidValuesRange(Integer value, int min, int max);

    /**
     * Creates an exception indicating invalid value for day-of-month
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14433, value = "Invalid value for day-of-month: %s")
    IllegalArgumentException invalidValueDayOfMonth(Integer value);

    /**
     * Creates an exception indicating relative day-of-month cannot be null or empty
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14434, value = "Relative day-of-month cannot be null or empty")
    IllegalArgumentException relativeDayOfMonthIsNull();

    /**
     * Creates an exception indicating is not relative value day-of-month
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14435, value = "%s is not a relative value")
    IllegalArgumentException invalidRelativeValue(String relativeDayOfMonth);

    /**
     * Creates an exception indicating value is null, cannot determine if it's relative
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14436, value = "Value is null, cannot determine if it's relative")
    IllegalArgumentException relativeValueIsNull();

    /**
     * Creates an exception indicating null timerservice cannot be registered"
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14437, value = "null timerservice cannot be registered")
    IllegalArgumentException timerServiceNotRegistered();

    /**
     * Creates an exception indicating the timer service is already registered
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14438, value = "Timer service with timedObjectId: %s\n is already registered")
    IllegalStateException timerServiceAlreadyRegistered(String timedObjectId);

    /**
     * Creates an exception indicating the null timedObjectId cannot be used for unregistering timerservice
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14439, value = "null timedObjectId cannot be used for unregistering timerservice")
    IllegalStateException timedObjectIdIsNullForUnregisteringTimerService();

    /**
     * Creates an exception indicating cannot unregister timer service because it's not registered"
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14440, value = "Cannot unregister timer service with timedObjectId: %s because it's not registered")
    IllegalStateException failToUnregisterTimerService(String timedObjectId);

    /**
     * Creates an exception indicating the invoker cannot be null
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14441, value = "Invoker cannot be null")
    IllegalArgumentException invokerIsNull();

    /**
     * Creates an exception indicating the transaction manager cannot be null
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14442, value = "Transaction manager cannot be null")
    IllegalArgumentException transactionManagerIsNull();

    /**
     * Creates an exception indicating the Executor cannot be null
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14443, value = "Executor cannot be null")
    IllegalArgumentException executorIsNull();

    /**
     * Creates an exception indicating the initialExpiration cannot be null while creating a timer
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14444, value = "initialExpiration cannot be null while creating a timer")
    IllegalArgumentException initialExpirationIsNullCreatingTimer();

    /**
     * Creates an exception indicating the value cannot be negative while creating a timer
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14445, value = "%s cannot be negative while creating a timer")
    IllegalArgumentException invalidInitialExpiration(String type);

    /**
     * Creates an exception indicating the expiration cannot be null while creating a single action timer
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14446, value = "expiration cannot be null while creating a single action timer")
    IllegalArgumentException expirationIsNull();

    /**
     * Creates an exception indicating the expiration.getTime() cannot be negative while creating a single action timer
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14447, value = "expiration.getTime() cannot be negative while creating a single action timer")
    IllegalArgumentException invalidExpirationActionTimer();

    /**
     * Creates an exception indicating duration cannot be negative while creating single action timer
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14448, value = "duration cannot be negative while creating single action timer")
    IllegalArgumentException invalidDurationActionTimer();

    /**
     * Creates an exception indicating Duration cannot negative while creating the timer
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14449, value = "Duration cannot negative while creating the timer")
    IllegalArgumentException invalidDurationTimer();

    /**
     * Creates an exception indicating the expiration date cannot be null while creating a timer
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14450, value = "Expiration date cannot be null while creating a timer")
    IllegalArgumentException expirationDateIsNull();

    /**
     * Creates an exception indicating the expiration.getTime() cannot be negative while creating a timer
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14451, value = "expiration.getTime() cannot be negative while creating a timer")
    IllegalArgumentException invalidExpirationTimer();

    /**
     * Creates an exception indicating the initial duration cannot be negative while creating timer
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14452, value = "Initial duration cannot be negative while creating timer")
    IllegalArgumentException invalidInitialDurationTimer();

    /**
     * Creates an exception indicating the interval cannot be negative while creating timer
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14453, value = "Interval cannot be negative while creating timer")
    IllegalArgumentException invalidIntervalTimer();

    /**
     * Creates an exception indicating the initial expiration date cannot be null while creating a timer
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14454, value = "initial expiration date cannot be null while creating a timer")
    IllegalArgumentException initialExpirationDateIsNull();

    /**
     * Creates an exception indicating the interval duration cannot be negative while creating timer
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14455, value = "interval duration cannot be negative while creating timer")
    IllegalArgumentException invalidIntervalDurationTimer();

    /**
     * Creates an exception indicating the creation of timers is not allowed during lifecycle callback of non-singleton EJBs
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14456, value = "Creation of timers is not allowed during lifecycle callback of non-singleton EJBs")
    IllegalStateException failToCreateTimerDoLifecycle();

    /**
     * Creates an exception indicating initial expiration is null
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14457, value = "initial expiration is null")
    IllegalArgumentException initialExpirationIsNull();

    /**
     * Creates an exception indicating the interval duration is negative
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14458, value = "interval duration is negative")
    IllegalArgumentException invalidIntervalDuration();

    /**
     * Creates an exception indicating the schedule is null
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14459, value = "schedule is null")
    IllegalArgumentException scheduleIsNull();

    /**
     * Creates an exception indicating it could not start transaction
     *
     * @return an {@link RuntimeException} for the error.
     */
    @Message(id = 14460, value = "Could not start transaction")
    RuntimeException failToStartTransaction(@Cause Throwable t);

    /**
     * Creates an exception indicating the transaction cannot be ended since no transaction is in progress
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14461, value = "Transaction cannot be ended since no transaction is in progress")
    IllegalStateException noTransactionInProgress();

    /**
     * Creates an exception indicating could not end transaction
     *
     * @return an {@link RuntimeException} for the error.
     */
    @Message(id = 14462, value = "Could not end transaction")
    RuntimeException failToEndTransaction(@Cause Throwable e);

    /**
     * Creates an exception indicating it cannot invoke timer service methods in lifecycle callback of non-singleton beans
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14463, value = "Cannot invoke timer service methods in lifecycle callback of non-singleton beans")
    IllegalStateException failToInvokeTimerServiceDoLifecycle();

    /**
     * Creates an exception indicating timer cannot be null
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14464, value = "Timer cannot be null")
    IllegalStateException timerIsNull();

    /**
     * Creates an exception indicating timer handles are only available for persistent timers
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14465, value = "%s Timer handles are only available for persistent timers.")
    IllegalStateException invalidTimerHandlersForPersistentTimers(String s);

    /**
     * Creates an exception indicating no more timeouts for timer
     *
     * @return an {@link NoMoreTimeoutsException} for the error.
     */
    @Message(id = 14466, value = "No more timeouts for timer %s")
    NoMoreTimeoutsException noMoreTimeoutForTimer(TimerImpl timer);

    /**
     * Creates an exception indicating the timer is not a calendar based timer"
     *
     * @return an {@link IllegalStateException for the error.
     */
    @Message(id = 14467, value = "Timer %s is not a calendar based timer")
    IllegalStateException invalidTimerNotCalendarBaseTimer(final TimerImpl timer);

    /**
     * Creates an exception indicating the Timer has expired
     *
     * @return an {@link NoSuchObjectLocalException} for the error.
     */
    @Message(id = 14468, value = "Timer has expired")
    NoSuchObjectLocalException timerHasExpired();

    /**
     * Creates an exception indicating the timer was canceled
     *
     * @return an {@link NoSuchObjectLocalException} for the error.
     */
    @Message(id = 14469, value = "Timer was canceled")
    NoSuchObjectLocalException timerWasCanceled();

    /**
     * Creates an exception indicating the timer is not persistent
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14470, value = "Timer %s is not persistent")
    IllegalStateException failToPersistTimer(TimerImpl timer);

    /**
     * Creates an exception indicating it could not register with tx for timer cancellation
     *
     * @return an {@link RuntimeException} for the error.
     */
    @Message(id = 14471, value = "Could not register with tx for timer cancellation")
    RuntimeException failToRegisterWithTxTimerCancellation(@Cause Throwable e);

    /**
     * Creates an exception indicating it could not deserialize info in timer
     *
     * @return an {@link RuntimeException} for the error.
     */
    @Message(id = 14472, value = "Could not deserialize info in timer ")
    RuntimeException failToDeserializeInfoInTimer(@Cause Throwable e);

    /**
     * Creates an exception indicating the Id cannot be null
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14473, value = "Id cannot be null")
    IllegalArgumentException idIsNull();

    /**
     * Creates an exception indicating Timed objectid cannot be null
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14474, value = "Timed objectid cannot be null")
    IllegalArgumentException timedObjectNull();

    /**
     * Creates an exception indicating the timer service cannot be null
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14475, value = "Timer service cannot be null")
    IllegalArgumentException timerServiceIsNull();

    /**
     * Creates an exception indicating the timerservice with timedObjectId is not registered
     *
     * @return an {@link EJBException} for the error.
     */
    @Message(id = 14476, value = "Timerservice with timedObjectId: %s is not registered")
    EJBException timerServiceWithIdNotRegistered(String timedObjectId);

    /**
     * Creates an exception indicating the timer for handle is not active"
     *
     * @return an {@link NoSuchObjectLocalException} for the error.
     */
    @Message(id = 14477, value = "Timer for handle: %s is not active")
    NoSuchObjectLocalException timerHandleIsNotActive(TimerHandle timerHandle);

    /**
     * Creates an exception indicating it could not find timeout method
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14478, value = "Could not find timeout method: %s")
    IllegalStateException failToFindTimeoutMethod(TimeoutMethod timeoutMethodInfo);

    /**
     * Creates an exception indicating it cannot invoke getTimeoutMethod on a timer which is not an auto-timer
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14479, value = "Cannot invoke getTimeoutMethod on a timer which is not an auto-timer")
    IllegalStateException failToInvokegetTimeoutMethod();

    /**
     * Creates an exception indicating it could not load declared class of timeout method
     *
     * @return an {@link RuntimeException} for the error.
     */
    @Message(id = 14480, value = "Could not load declaring class: %s of timeout method")
    RuntimeException failToLoadDeclaringClassOfTimeOut(String declaringClass);

    /**
     * Creates an exception indicating it cannot invoke timeout method because timer is an auto timer,
     * but invoker is not of type specified"
     *
     * @return an {@link RuntimeException} for the error.
     */
    @Message(id = 14481, value = "Cannot invoke timeout method because timer: %s is an auto timer, but invoker is not of type %s")
    RuntimeException failToInvokeTimeout(CalendarTimer calendarTimer, Class<MultiTimeoutMethodTimedObjectInvoker> multiTimeoutMethodTimedObjectInvokerClass);

    /**
     * Creates an exception indicating it could not create timer file store directory
     *
     * @return an {@link RuntimeException} for the error.
     */
    @Message(id = 14482, value = "Could not create timer file store directory %s")
    RuntimeException failToCreateTimerFileStoreDir(File baseDir);

    /**
     * Creates an exception indicating timer file store directory does not exist"
     *
     * @return an {@link RuntimeException} for the error.
     */
    @Message(id = 14483, value = "Timer file store directory %s does not exist")
    RuntimeException timerFileStoreDirNotExist(File baseDir);

    /**
     * Creates an exception indicating the timer file store directory is not a directory
     *
     * @return an {@link RuntimeException} for the error.
     */
    @Message(id = 14484, value = "Timer file store directory %s is not a directory")
    RuntimeException invalidTimerFileStoreDir(File baseDir);

    /**
     * Creates an exception indicating EJB is enabled for security but doesn't have a security domain set
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14485, value = "EJB %s is enabled for security but doesn't have a security domain set")
    IllegalStateException invalidSecurityForDomainSet(String componentName);

    /**
     * Creates an exception indicating component configuration is not an EJB component"
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14486, value = "%s is not an EJB component")
    IllegalArgumentException invalidComponentConfiguration(String componentName);

    /**
     * Creates an exception indicating it could not load view class for ejb
     *
     * @return an {@link RuntimeException} for the error.
     */
    @Message(id = 14487, value = "Could not load view class for ejb %s")
    RuntimeException failToLoadViewClassEjb(String beanName, @Cause Throwable e);

    /**
     * Creates an exception indicating the component named with component class is not a EJB component
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14488, value = "Component named %s with component class %s is not a EJB component")
    IllegalArgumentException invalidEjbComponent(String componentName, Class<?> componentClass);

    /**
     * Creates an exception indicating no timed object invoke for component
     *
     * @return an {@link StartException} for the error.
     */
    @Message(id = 14489, value = "No timed object invoke for %s")
    StartException failToInvokeTimedObject(EJBComponent component);

    /**
     * Creates an exception indicating TimerService is not started
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14490, value = "TimerService is not started")
    IllegalStateException failToStartTimerService();

    /**
     * Creates an exception indicating resourceBundle based descriptions are not supported
     *
     * @return an {@link UnsupportedOperationException} for the error.
     */
    @Message(id = 14491, value = "ResourceBundle based descriptions of %s are not supported")
    UnsupportedOperationException resourceBundleDescriptionsNotSupported(String name);

    /**
     * Creates an exception indicating a runtime attribute is not marshallable
     *
     * @return an {@link UnsupportedOperationException} for the error.
     */
    @Message(id = 14492, value = "Runtime attribute %s is not marshallable")
    UnsupportedOperationException runtimeAttributeNotMarshallable(String name);

    /**
     * Creates an exception indicating a invalid value for the specified element
     *
     * @return an {@link String} for the error.
     */
    @Message(id = 14493, value = "Invalid value: %s for '%s' element %s")
    String invalidValueForElement(String value, String element, Location location);

    /**
     * Creates an exception indicating EJB component type does not support pools
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14494, value = "EJB component type %s does not support pools")
    IllegalStateException invalidComponentType(String simpleName);

    /**
     * Creates an exception indicating Unknown EJBComponent type
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14495, value = "Unknown EJBComponent type %s")
    IllegalStateException unknownComponentType(EJBComponentType ejbComponentType);

    /**
     * Creates an exception indicating Method for view shouldn't be
     * marked for both @PemitAll and @DenyAll at the same time
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14496, value = "Method %s for view %s shouldn't be marked for both %s and %s at the same time")
    IllegalStateException invalidSecurityAnnotation(Method componentMethod, String viewClassName, final String s, final String s1);

    /**
     * Creates an exception indicating method named with params not found on component class
     *
     * @return an {@link RuntimeException} for the error.
     */
    @Message(id = 14497, value = "Method named %s with params %s not found on component class %s")
    RuntimeException failToFindComponentMethod(String name, String s, Class<?> componentClass);

    /**
     * Creates an exception indicating the EJB method security metadata cannot be null
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14498, value = "EJB method security metadata cannot be null")
    IllegalArgumentException ejbMethodSecurityMetaDataIsNull();

    /**
     * Creates an exception indicating the view classname cannot be null or empty
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14499, value = "View classname cannot be null or empty")
    IllegalArgumentException viewClassNameIsNull();

    /**
     * Creates an exception indicating View method cannot be null
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14500, value = "View method cannot be null")
    IllegalArgumentException viewMethodIsNull();

    /**
     * Creates an exception indicating class cannot handle method of view class
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14501, value = "%s cannot handle method %s of view class %s.Expected view method to be %s on view class %s")
    IllegalStateException failProcessInvocation(String name, final Method invokedMethod, String viewClassOfInvokedMethod, Method viewMethod, String viewClassName);

    /**
     * Creates an exception indicating the Invocation on method is not allowed
     *
     * @return an {@link EJBAccessException} for the error.
     */
    @Message(id = 14502, value = "Invocation on method: %s of bean: %s is not allowed")
    EJBAccessException invocationOfMethodNotAllowed(Method invokedMethod, String componentName);

    /**
     * Creates an exception indicating an unknown EJB Component description type
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14503, value = "Unknown EJB Component description type %s")
    IllegalArgumentException unknownComponentDescriptionType(Class<?> aClass);

    /**
     * Creates an exception indicating unknown attribute
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14504, value = "Unknown attribute %s")
    IllegalStateException unknownAttribute(String attributeName);

    /**
     * Creates an exception indicating Unknown operation
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14505, value = "Unknown operation %s")
    IllegalStateException unknownOperations(String opName);

    /**
     * Creates an exception indicating no EJB component registered for address
     *
     * @return an {@link String} for the error.
     */
    @Message(id = 14506, value = "No EJB component registered for address %s")
    String noComponentRegisteredForAddress(PathAddress operationAddress);

    /**
     * Creates an exception indicating No EJB component is available for address
     *
     * @return an {@link String} for the error.
     */
    @Message(id = 14507, value = "No EJB component is available for address %s")
    String noComponentAvailableForAddress(PathAddress operationAddress);

    /**
     * Creates an exception indicating EJB component for specified address is in invalid state
     *
     * @return an {@link String} for the error.
     */
    @Message(id = 14508, value = "EJB component for address %s is in \n state %s, must be in state %s")
    String invalidComponentState(PathAddress operationAddress, ServiceController.State controllerState, ServiceController.State up);


    /**
     * Creates an exception indicating specified components is not an EJB component"
     *
     * @param componentName
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14509, value = "%s is not an EJB component")
    IllegalArgumentException invalidComponentIsNotEjbComponent(final String componentName);

    /**
     * Creates an exception indicating Component class has multiple @Timeout annotations
     *
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 14510, value = "Component class %s has multiple @Timeout annotations")
    DeploymentUnitProcessingException componentClassHasMultipleTimeoutAnnotations(Class<?> componentClass);

    /**
     * Creates an exception indicating the current component is not an EJB.
     *
     * @param component the component.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14511, value = "Current component is not an EJB %s")
    IllegalStateException currentComponentNotAEjb(ComponentInstance component);

    /**
     * Creates an exception indicating the method invocation is not allowed in lifecycle methods.
     *
     * @param methodName the name of the method.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14512, value = "%s not allowed in lifecycle methods")
    IllegalStateException lifecycleMethodNotAllowed(String methodName);

    @Message(id = 14513, value = "%s is not allowed in lifecycle methods of stateless session beans")
    IllegalStateException lifecycleMethodNotAllowedFromStatelessSessionBean(String methodName);

    /**
     * Creates an exception indicating Cannot call getInvokedBusinessInterface when invoking through ejb object
     *
     * @param name type of object
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 14514, value = "Cannot call %s when invoking through %s or %s")
    IllegalStateException cannotCall(String methodName, String name, String localName);

    @Message(id = 14515, value = "%s is not allowed from stateful beans")
    IllegalStateException notAllowedFromStatefulBeans(String method);

    @Message(id = 14516, value = "Failed to acquire a permit within %s %s")
    EJBException failedToAcquirePermit(long timeout, TimeUnit timeUnit);

    @Message(id = 14517, value = "Acquire semaphore was interrupted")
    EJBException acquireSemaphoreInterrupted();


    /**
     * Creates an exception indicating the method is deprecated
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 14518, value = "%s is deprecated")
    IllegalStateException isDeprecatedIllegalState(String getEnvironment);

    @Message(id=14519, value="Could not find method %s on entity bean")
    RuntimeException couldNotFindEntityBeanMethod(String method);

    @Message(id=14520, value="Could not determine ClassLoader for stub %s")
    RuntimeException couldNotFindClassLoaderForStub(String stub);

    /**
     * Creates an exception indicating that there was no message listener of the expected type
     * in the resource adapter
     *
     * @param messageListenerType The message listener type
     * @param resourceAdapterName The resource adapter name
     *
     * @return a {@link IllegalStateException} for the error.
     */
    @Message(id = 14521, value = "No message listener of type %s found in resource adapter %s")
    IllegalStateException unknownMessageListenerType(String resourceAdapterName, String messageListenerType);

    /**
     * Thrown when a EJB 2 EJB does not implement a method on an EJB 2
     * @param method The method
     * @param viewClass The view
     * @param ejb The ejb
     */
    @Message(id=14522, value = "Could not find method %s from view %s on EJB class %s")
    DeploymentUnitProcessingException couldNotFindViewMethodOnEjb(final Method method, String viewClass, String ejb);

    /**
     * Creates and returns an exception indicating that the param named <code>paramName</code> cannot be null
     * or empty string.
     *
     * @param paramName The param name
     * @return an {@link IllegalArgumentException} for the exception
     */
    @Message(id = 14523, value = "%s cannot be null or empty")
    IllegalArgumentException stringParamCannotBeNullOrEmpty(final String paramName);

    /**
     * Exception that is thrown when invoking remove while an EJB is in a transaction
     */
    @Message(id=14524, value = "EJB 4.6.4 Cannot remove EJB via EJB 2.x remove() method while participating in a transaction")
    RemoveException cannotRemoveWhileParticipatingInTransaction();

    @Message(id=14525, value = "Transaction propagation over IIOP is not supported")
    RemoteException transactionPropagationNotSupported();

}
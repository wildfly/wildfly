/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.jpa.messages;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.lang.instrument.IllegalClassFormatException;

import javax.ejb.EJBException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.TransactionRequiredException;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.vfs.VirtualFile;

/**
 * Date: 07.06.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@MessageLogger(projectCode = "WFLYJPA", length = 4)
public interface JpaLogger extends BasicLogger {
    /**
     * Default root level logger with the package name for he category.
     */
    JpaLogger ROOT_LOGGER = Logger.getMessageLogger(JpaLogger.class, "org.jboss.as.jpa");

    /**
     * Logs a warning message indicating duplicate persistence.xml files were found.
     *
     * @param puName    the persistence XML file.
     * @param ogPuName  the original persistence.xml file.
     * @param dupPuName the duplicate persistence.xml file.
     */
    @LogMessage(level = WARN)
    @Message(id = 1, value = "Duplicate Persistence Unit definition for %s " +
        "in application.  One of the duplicate persistence.xml should be removed from the application." +
        " Application deployment will continue with the persistence.xml definitions from %s used.  " +
        "The persistence.xml definitions from %s will be ignored.")
    void duplicatePersistenceUnitDefinition(String puName, String ogPuName, String dupPuName);

    /**
     * Logs an informational message indicating the persistence.xml file is being read.
     *
     * @param puUnitName the persistence unit name.
     */
    @LogMessage(level = INFO)
    @Message(id = 2, value = "Read persistence.xml for %s")
    void readingPersistenceXml(String puUnitName);

    /**
     * Logs an informational message indicating the service, represented by the {@code serviceName} parameter, is
     * starting.
     *
     * @param serviceName the name of the service.
     * @param name        an additional name for the service.
     */
    @LogMessage(level = INFO)
    @Message(id = 3, value = "Starting %s Service '%s'")
    void startingService(String serviceName, String name);

    /**
     * Logs an informational message indicating the service, represented by the {@code serviceName} parameter, is
     * stopping.
     *
     * @param serviceName the name of the service.
     * @param name        an additional name for the service.
     */
    @LogMessage(level = INFO)
    @Message(id = 4, value = "Stopping %s Service '%s'")
    void stoppingService(String serviceName, String name);

//    /**
//     * Logs an error message indicating an exception occurred while preloading the default persistence provider adapter module.
//     * Initialization continues after logging the error.
//     *
//     * @param cause the cause of the error.
//     */
//    @LogMessage(level = ERROR)
//    @Message(id = 5, value = "Could not load default persistence provider adaptor module.  Management attributes will not be registered for the adaptor")
//    void errorPreloadingDefaultProviderAdaptor(@Cause Throwable cause);

    /**
     * Logs an error message indicating an exception occurred while preloading the default persistence provider module.
     * Initialization continues after logging the error.
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 6, value = "Could not load default persistence provider module.  ")
    void errorPreloadingDefaultProvider(@Cause Throwable cause);

    /**
     * Logs an error message indicating the persistence unit was not stopped
     *
     * @param cause       the cause of the error.
     * @param name        name of the persistence unit
     */
    @LogMessage(level = ERROR)
    @Message(id = 7, value = "Failed to stop persistence unit service %s")
    void failedToStopPUService(@Cause Throwable cause, String name);

//    /**
//     * Creates an exception indicating a failure to get the module for the deployment unit represented by the
//     * {@code deploymentUnit} parameter.
//     *
//     * @param deploymentUnit the deployment unit that failed.
//     */
    //@LogMessage(level = WARN)
    //@Message(id = 8, value = "Failed to get module attachment for %s")
    //void failedToGetModuleAttachment(DeploymentUnit deploymentUnit);

//    /**
//     * warn that the entity class could not be loaded with the
//     * {@link javax.persistence.spi.PersistenceUnitInfo#getClassLoader()}.
//     *
//     * @param cause     the cause of the error.
//     * @param className the entity class name.
//     */
    //@LogMessage(level = WARN)
    //@Message(id = 9, value = "Could not load entity class '%s', ignoring this error and continuing with application deployment")
    //void cannotLoadEntityClass(@Cause Throwable cause, String className);

    /**
     * Logs an informational message indicating the persistence unit service is starting phase n of 2.
     *
     * @param phase       is the phase number (1 or 2)
     * @param name        an additional name for the service.
     */
    @LogMessage(level = INFO)
    @Message(id = 10, value = "Starting Persistence Unit (phase %d of 2) Service '%s'")
    void startingPersistenceUnitService(int phase, String name);

    /**
     * Logs an informational message indicating the service is stopping.
     *
     * @param phase       is the phase number (1 or 2)
     * @param name        an additional name for the service.
     */
    @LogMessage(level = INFO)
    @Message(id = 11, value = "Stopping Persistence Unit (phase %d of 2) Service '%s'")
    void stoppingPersistenceUnitService(int phase, String name);

    /**
     * Logs warning about unexpected problem gathering statistics.
     *
     * @param cause is the cause of the warning
     */
    @LogMessage(level = WARN)
    @Message(id = 12, value = "Unexpected problem gathering statistics")
    void unexpectedStatisticsProblem(@Cause RuntimeException cause);

//    /**
//     * Creates an exception indicating the inability ot add the integration, represented by the {@code name} parameter,
//     * module to the deployment.
//     *
//     * @param cause the cause of the error.
//     * @param name  the name of the integration.
//     * @return a {@link RuntimeException} for the error.
//     */
    //@Message(id = 13, value = "Could not add %s integration module to deployment")
    // RuntimeException cannotAddIntegration(@Cause Throwable cause, String name);

//    /**
//     * Creates an exception indicating the input stream reference cannot be changed.
//     *
//     * @return an {@link IllegalArgumentException} for the error.
//     */
    //@Message(id = 14, value = "Cannot change input stream reference.")
    //IllegalArgumentException cannotChangeInputStream();

    /**
     * Creates an exception indicating the entity manager cannot be closed when it is managed by the container.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 15, value = "Container managed entity manager can only be closed by the container " +
        "(will happen when @remove method is invoked on containing SFSB)")
    IllegalStateException cannotCloseContainerManagedEntityManager();

//    /**
//     * Creates an exception indicating only ExtendedEntityMangers can be closed.
//     *
//     * @param entityManagerTypeName the entity manager type name.
//     * @return a {@link RuntimeException} for the error.
//     */
    //@Message(id = 16, value = "Can only close SFSB XPC entity manager that are instances of ExtendedEntityManager %s")
    //RuntimeException cannotCloseNonExtendedEntityManager(String entityManagerTypeName);

    /**
     * Creates an exception indicating the transactional entity manager cannot be closed when it is managed by the
     * container.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 17, value = "Container managed entity manager can only be closed by the container " +
        "(auto-cleared at tx/invocation end and closed when owning component is closed.)")
    IllegalStateException cannotCloseTransactionContainerEntityManger();

    /**
     * Creates an exception indicating the inability to create an instance of the adapter class represented by the
     * {@code className} parameter.
     *
     * @param cause     the cause of the error.
     * @param className the adapter class name.
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 18, value = "Could not create instance of adapter class '%s'")
    DeploymentUnitProcessingException cannotCreateAdapter(@Cause Throwable cause, String className);

    /**
     * Creates an exception indicating the application could not be deployed with the persistence provider, represented
     * by the {@code providerName} parameter, packaged.
     *
     * @param cause        the cause of the error.
     * @param providerName the persistence provider.
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 19, value = "Could not deploy application packaged persistence provider '%s'")
    DeploymentUnitProcessingException cannotDeployApp(@Cause Throwable cause, String providerName);

    /**
     * Creates an exception indicating a failure to get the Hibernate session factory from the entity manager.
     *
     * @param cause the cause of the error.
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 20, value = "Couldn't get Hibernate session factory from entity manager")
    RuntimeException cannotGetSessionFactory(@Cause Throwable cause);

    /**
     * A message indicating the inability to inject a
     * {@link javax.persistence.spi.PersistenceUnitTransactionType#RESOURCE_LOCAL} container managed EntityManager
     * using the {@link javax.persistence.PersistenceContext} annotation.
     *
     * @return the message.
     */
    @Message(id = 21, value = "Cannot inject RESOURCE_LOCAL container managed EntityManagers using @PersistenceContext")
    String cannotInjectResourceLocalEntityManager();

//    /**
//     * Creates an exception indicating the inability to inject a
//     * {@link javax.persistence.spi.PersistenceUnitTransactionType#RESOURCE_LOCAL} entity manager, represented by the
//     * {@code unitName} parameter, using the {@code <persistence-context-ref>}.
//     *
//     * @param unitName the unit name.
//     * @return a {@link DeploymentUnitProcessingException} for the error.
//     */
    //@Message(id = 22, value = "Cannot inject RESOURCE_LOCAL entity manager %s using <persistence-context-ref>")
    //DeploymentUnitProcessingException cannotInjectResourceLocalEntityManager(String unitName);

//    /**
//     * Creates an exception indicating the persistence provider adapter module, represented by the {@code adapterModule}
//     * parameter, had an error loading.
//     *
//     * @param cause                    the cause of the error.
//     * @param adapterModule            the name of the adapter module.
//     * @param persistenceProviderClass the persistence provider class.
//     * @return a {@link DeploymentUnitProcessingException} for the error.
//     */
    //@Message(id = 23, value = "Persistence provider adapter module (%s) load error (class %s)")
    //DeploymentUnitProcessingException cannotLoadAdapterModule(@Cause Throwable cause, String adapterModule, String persistenceProviderClass);

//    /**
//     * Creates an exception indicating the entity class could not be loaded with the
//     * {@link javax.persistence.spi.PersistenceUnitInfo#getClassLoader()}.
//     *
//     * @param cause     the cause of the error.
//     * @param className the entity class name.
//     * @return a {@link RuntimeException} for the error.
//     */
    //@Message(id = 24, value = "Could not load entity class '%s' with PersistenceUnitInfo.getClassLoader()")
    //RuntimeException cannotLoadEntityClass(@Cause Throwable cause, String className);

    /**
     * Creates an exception indicating the {@code injectionTypeName} could not be loaded from the JPA modules class
     * loader.
     *
     * @param cause             the cause of the error.
     * @param injectionTypeName the name of the type.
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 25, value = "Couldn't load %s from JPA modules classloader")
    RuntimeException cannotLoadFromJpa(@Cause Throwable cause, String injectionTypeName);

//    /**
//     * Creates an exception indicating the module, represented by the {@code moduleId} parameter, could not be loaded
//     * for the adapter, represented by the {@code name} parameter.
//     *
//     * @param cause    the cause of the error.
//     * @param moduleId the module id that was attempting to be loaded.
//     * @param name     the name of the adapter.
//     * @return a {@link RuntimeException} for the error.
//     */
    //@Message(id = 26, value = "Could not load module %s to add %s adapter to deployment")
    //RuntimeException cannotLoadModule(@Cause Throwable cause, ModuleIdentifier moduleId, String name);

    /**
     * Creates an exception indicating the persistence provider module, represented by the
     * {@code persistenceProviderModule} parameter, had an error loading.
     *
     * @param cause                     the cause of the error.
     * @param persistenceProviderModule the name of the adapter module.
     * @param persistenceProviderClass  the persistence provider class.
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 27, value = "Persistence provider module load error %s (class %s)")
    DeploymentUnitProcessingException cannotLoadPersistenceProviderModule(@Cause Throwable cause, String persistenceProviderModule, String persistenceProviderClass);

//    /**
//     * Creates an exception indicating the top of the stack could not be replaced because the stack is {@code null}.
//     *
//     * @return a {@link RuntimeException} for the error.
//     */
    //@Message(id = 28, value = "Internal error: Cannot replace top of stack as stack is null (same as being empty).")
    //RuntimeException cannotReplaceStack();

    /**
     * Creates an exception indicating that both {@code key1} and {@code key2} cannot be specified for the object.
     *
     * @param key1      the first key/tag.
     * @param value1    the first value.
     * @param key2      the second key/tag.
     * @param value2    the second value.
     * @param parentTag the parent tag.
     * @param object    the object the values are being specified for.
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 29, value = "Cannot specify both %s (%s) and %s (%s) in %s for %s")
    DeploymentUnitProcessingException cannotSpecifyBoth(String key1, Object value1, String key2, Object value2, String parentTag, Object object);

    /**
     * Creates an exception indicating the extended persistence context for the SFSB already exists.
     *
     * @param puScopedName          the persistence unit name.
     * @param existingEntityManager the existing transactional entity manager.
     * @param self                  the entity manager attempting to be created.
     * @return an {@link javax.ejb.EJBException} for the error.
     */
    @Message(id = 30, value = "Found extended persistence context in SFSB invocation call stack but that cannot be used " +
            "because the transaction already has a transactional context associated with it.  " +
            "This can be avoided by changing application code, either eliminate the extended " +
            "persistence context or the transactional context.  See JPA spec 2.0 section 7.6.3.1.  " +
            "Scoped persistence unit name=%s, persistence context already in transaction =%s, extended persistence context =%s.")
    EJBException cannotUseExtendedPersistenceTransaction(String puScopedName, EntityManager existingEntityManager, EntityManager self);

    /**
     * Creates an exception indicating the child could not be found on the parent.
     *
     * @param child  the child that could not be found.
     * @param parent the parent.
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 31, value = "Could not find child '%s' on '%s'")
    RuntimeException childNotFound(String child, VirtualFile parent);

    /**
     * Creates an exception indicating the class level annotation must provide the parameter specified.
     *
     * @param annotation the annotation.
     * @param className  the class name
     * @param parameter  the parameter.
     * @return a string for the error.
     */
    @Message(id = 32, value = "Class level %s annotation on class %s must provide a %s")
    String classLevelAnnotationParameterRequired(String annotation, String className, String parameter);

    /**
     * A message indicating that the persistence unit, represented by the {@code path} parameter, could not be found at
     * the current deployment unit, represented by the {@code deploymentUnit} parameter.
     *
     * @param puName         the persistence unit name.
     * @param deploymentUnit the deployment unit.
     * @return the message.
     */
    @Message(id = 33, value = "Can't find a persistence unit named %s in %s")
    String persistenceUnitNotFound(String puName, DeploymentUnit deploymentUnit);

    /**
     * Creates an exception indicating that the persistence unit, represented by the {@code path} and {@code puName}
     * parameters, could not be found at the current deployment unit, represented by the {@code deploymentUnit}
     * parameter.
     *
     * @param path           the path.
     * @param puName         the persistence unit name.
     * @param deploymentUnit the deployment unit.
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 34, value = "Can't find a persistence unit named %s#%s at %s")
    IllegalArgumentException persistenceUnitNotFound(String path, String puName, DeploymentUnit deploymentUnit);

//    /**
//     * Creates an exception indicating the parameter, likely a collection, is empty.
//     *
//     * @param parameterName the parameter name.
//     * @return an {@link IllegalArgumentException} for the error.
//     */
    //@Message(id = 35, value = "Parameter %s is empty")
    //IllegalArgumentException emptyParameter(String parameterName);

    /**
     * Creates an exception indicating there was an error when trying to get the transaction associated with the
     * current thread.
     *
     * @param cause the cause of the error.
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 36, value = "An error occurred while getting the transaction associated with the current thread: %s")
    IllegalStateException errorGettingTransaction(Exception cause);

    /**
     * Creates an exception indicating a failure to get the adapter for the persistence provider.
     *
     * @param className the adapter class name.
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 37, value = "Failed to get adapter for persistence provider '%s'")
    DeploymentUnitProcessingException failedToGetAdapter(String className);

    /**
     * Creates an exception indicating a failure to add the persistence unit service.
     *
     * @param cause  the cause of the error.
     * @param puName the persistence unit name.
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 38, value = "Failed to add persistence unit service for %s")
    DeploymentUnitProcessingException failedToAddPersistenceUnit(@Cause Throwable cause, String puName);

//    /**
//     * Creates an exception indicating a failure to get the module for the deployment unit represented by the
//     * {@code deploymentUnit} parameter.
//     *
//     * @param deploymentUnit the deployment unit that failed.
//     * @return a {@link DeploymentUnitProcessingException} for the error.
//     */
    //@Message(id = 39, value = "Failed to get module attachment for %s")
    //DeploymentUnitProcessingException failedToGetModuleAttachment(DeploymentUnit deploymentUnit);

    /**
     * A message indicating a failure to parse the file.
     *
     * @param file the file that could not be parsed.
     * @return the message.
     */
    @Message(id = 40, value = "Failed to parse %s")
    String failedToParse(VirtualFile file);

    /**
     * Creates an exception indicating the entity manager factory implementation can only be a Hibernate version.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 41, value = "Can only inject from a Hibernate EntityManagerFactoryImpl")
    RuntimeException hibernateOnlyEntityManagerFactory();

//    /**
//     * Creates an exception indicating the entity manager factory implementation can only be a Hibernate version.
//     *
//     * @return a {@link RuntimeException} for the error.
//     */
    //@Message(id = 42, value = "File %s not found")
    //RuntimeException fileNotFound(File file);

    /**
     * Creates an exception indicating the persistence unit name contains an invalid character.
     *
     * @param persistenceUnitName the persistence unit name.
     * @param c                   the invalid character.
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 43, value = "Persistence unit name (%s) contains illegal '%s' character")
    IllegalArgumentException invalidPersistenceUnitName(String persistenceUnitName, char c);

    /**
     * Creates an exception indicating the (custom) scoped persistence unit name is invalid.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 44, value = "jboss.as.jpa.scopedname hint (%s) contains illegal '%s' character")
    IllegalArgumentException invalidScopedName(String persistenceUnitName, char c);

//    /**
//     * Creates an exception indicating the inability to integrate the module, represented by the {@code integrationName}
//     * parameter, to the deployment as it expected a {@link java.net.JarURLConnection}.
//     *
//     * @param integrationName the name of the integration that could not be integrated.
//     * @param connection      the invalid connection.
//     * @return a {@link RuntimeException} for the error.
//     */
    //@Message(id = 45, value = "Could not add %s integration module to deployment, did not get expected JarUrlConnection, got %s")
    //RuntimeException invalidUrlConnection(String integrationName, URLConnection connection);

    //@Message(id = 46, value = "Could not load %s")
    //XMLStreamException errorLoadingJBossJPAFile(@Cause Throwable cause, String path);

//    /**
//     * Creates an exception indicating the persistence unit metadata likely because thread local was not set.
//     *
//     * @return a {@link RuntimeException} for the error.
//     */
    //@Message(id = 47, value = "Missing PersistenceUnitMetadata (thread local wasn't set)")
    //RuntimeException missingPersistenceUnitMetadata();

    /**
     * Creates an exception indicating the persistence provider adapter module, represented by the {@code adapterModule}
     * parameter, has more than one adapter.
     *
     * @param adapterModule the adapter module name.
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 48, value = "Persistence provider adapter module (%s) has more than one adapter")
    RuntimeException multipleAdapters(String adapterModule);

//    /**
//     * Creates an exception indicating more than one thread is invoking the stateful session bean at the same time.
//     *
//     * @param sessionBean the stateful session bean.
//     * @return a {@link RuntimeException} for the error.
//     */
    //@Message(id = 49, value = "More than one thread is invoking stateful session bean '%s' at the same time")
    //RuntimeException multipleThreadsInvokingSfsb(Object sessionBean);

//    /**
//     * Creates an exception indicating more than one thread is using the entity manager instance at the same time.
//     *
//     * @param entityManager the entity manager.
//     * @return a {@link RuntimeException} for the error.
//     */
    //@Message(id = 50, value = "More than one thread is using EntityManager instance '%s' at the same time")
    //RuntimeException multipleThreadsUsingEntityManager(EntityManager entityManager);

//    /**
//     * Creates an exception indicating the {@code name} was not set in the {@link org.jboss.invocation.InterceptorContext}.
//     *
//     * @param name    the name of the field not set.
//     * @param context the context.
//     * @return an {@link IllegalArgumentException} for the error.
//     */
    //@Message(id = 51, value = "%s not set in InterceptorContext: %s")
    //IllegalArgumentException notSetInInterceptorContext(String name, InterceptorContext context);

//    /**
//     * Creates an exception indicating the method is not yet implemented.
//     *
//     * @return a {@link RuntimeException} for the error.
//     */
    //@Message(id = 52, value = "Not yet implemented")
    //RuntimeException notYetImplemented();

    /**
     * Creates an exception indicating the {@code description} is {@code null}.
     *
     * @param description   the description of the parameter.
     * @param parameterName the parameter name.
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 53, value = "Internal %s error, null %s passed in")
    RuntimeException nullParameter(String description, String parameterName);

//    /**
//     * Creates an exception indicating the variable is {@code null}.
//     *
//     * @param varName the variable name.
//     * @return an {@link IllegalArgumentException} for the error.
//     */
    //@Message(id = 54, value = "Parameter %s is null")
    //IllegalArgumentException nullVar(String varName);

//    /**
//     * A message indicating the object for the class ({@code cls} has been defined and is not {@code null}.
//     *
//     * @param cls      the class for the object.
//     * @param previous the previously defined object.
//     * @return the message.
//     */
    //@Message(id = 55, value = "Previous object for class %s is %s instead of null")
    //String objectAlreadyDefined(Class<?> cls, Object previous);

//    /**
//     * Creates an exception indicating the parameter must be an ExtendedEntityManager
//     *
//     * @param gotClass
//     * @return a {@link RuntimeException} for the error.
//     */
    //@Message(id = 56, value = "Internal error, expected parameter of type ExtendedEntityManager but instead got %s")
    //RuntimeException parameterMustBeExtendedEntityManager(String gotClass);

    /**
     * Creates an exception indicating the persistence provider could not be found.
     *
     * @param providerName the provider name.
     * @return a {@link javax.persistence.PersistenceException} for the error.
     */
    @Message(id = 57, value = "PersistenceProvider '%s' not found")
    PersistenceException persistenceProviderNotFound(String providerName);

    /**
     * Creates an exception indicating the relative path could not be found.
     *
     * @param cause the cause of the error.
     * @param path  the path that could not be found.
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 58, value = "Could not find relative path: %s")
    RuntimeException relativePathNotFound(@Cause Throwable cause, String path);

    /**
     * A message indicating the annotation is only valid on setter method targets.
     *
     * @param annotation the annotation.
     * @param methodInfo the method information.
     * @return the message.
     */
    @Message(id = 59, value = "%s injection target is invalid.  Only setter methods are allowed: %s")
    String setterMethodOnlyAnnotation(String annotation, MethodInfo methodInfo);

    /**
     * Creates an exception indicating a transaction is required for the operation.
     *
     * @return a {@link javax.persistence.TransactionRequiredException} for the error.
     */
    @Message(id = 60, value = "Transaction is required to perform this operation (either use a transaction or extended persistence context)")
    TransactionRequiredException transactionRequired();

    /**
     * JBoss 4 prevented applications from referencing the persistence unit without specifying the pu name, if there
     * were multiple persistence unit definitions in the app.  JBoss 5 loosened the checking up, to let applications,
     * just use any PU definition that they find.  For AS7, we are strictly enforcing this again just like we did in
     * JBoss 4.
     * AS7-2275
     *
     * @param deploymentUnit the deployment unit.
     * @param puCount is number of persistence units defined in application
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 61, value = "Persistence unitName was not specified and there are %d persistence unit definitions in application deployment %s."+
        "  Either change the application deployment to have only one persistence unit definition or specify the unitName for each reference to a persistence unit.")
    IllegalArgumentException noPUnitNameSpecifiedAndMultiplePersistenceUnits(int puCount, DeploymentUnit deploymentUnit);

    /**
     * Creates an exception indicating the persistence provider could not be instantiated ,
     *
     * @param cause the cause of the error.
     * @param providerClassName name of the provider class
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 62, value = "Could not create instance of persistence provider class %s")
    RuntimeException couldNotCreateInstanceProvider(@Cause Throwable cause, String providerClassName);

    /**
     * internal error indicating that the number of stateful session beans associated with a
     * extended persistence context has reached a negative count.
     *
     * @return a {@link RuntimeException} for the error
     */
    @Message(id = 63, value = "internal error, the number of stateful session beans (%d) associated " +
        "with an extended persistence context (%s) cannot be a negative number.")
    RuntimeException referenceCountedEntityManagerNegativeCount(int referenceCount, String scopedPuName);

    /**
     * Can't use a new unsynchronization persistence context when transaction already has a synchronized persistence context.
     *
     * @param puScopedName          the persistence unit name.
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 64, value =
            "JTA transaction already has a 'SynchronizationType.UNSYNCHRONIZED' persistence context (EntityManager) joined to it " +
            "but a component with a 'SynchronizationType.SYNCHRONIZED' is now being used.  " +
            "Change the calling component code to join the persistence context (EntityManager) to the transaction or "+
            "change the called component code to also use 'SynchronizationType.UNSYNCHRONIZED'.  "+
            "See JPA spec 2.1 section 7.6.4.1.  " +
            "Scoped persistence unit name=%s.")
    IllegalStateException badSynchronizationTypeCombination(String puScopedName);

    @Message(id = 65, value = "Resources of type %s cannot be registered")
    UnsupportedOperationException resourcesOfTypeCannotBeRegistered(String key);

    @Message(id = 66, value = "Resources of type %s cannot be removed")
    UnsupportedOperationException resourcesOfTypeCannotBeRemoved(String key);

    /**
     * Only one persistence provider adapter per (persistence provider or application) classloader is allowed
     *
     * @param classloader offending classloader
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 67, value = "Classloader '%s' has more than one Persistence provider adapter")
    RuntimeException classloaderHasMultipleAdapters(String classloader);

//    /**
//     * Likely cause is that the application deployment did not complete successfully
//     *
//     * @param scopedPuName
//     * @return
//     */
//    @Message(id = 68, value = "Persistence unit '%s' is not available")
//    IllegalStateException PersistenceUnitNotAvailable(String scopedPuName);

    /**
     * persistence provider adaptor module load error
     *
     * @param cause the cause of the error.
     * @param adaptorModule name of persistence provider adaptor module that couldn't be loaded.
     * @return the exception
     */
    @Message(id = 69, value =  "Persistence provider adapter module load error %s")
    DeploymentUnitProcessingException persistenceProviderAdaptorModuleLoadError(@Cause Throwable cause, String adaptorModule);

    /**
     * extended persistence context can only be used within a stateful session bean. WFLY-69
     *
     * @param scopedPuName name of the persistence unit
     * @return the exception
     */
    @Message(id = 70, value = "A container-managed extended persistence context can only be initiated within the scope of a stateful session bean (persistence unit '%s').")
    IllegalStateException xpcOnlyFromSFSB(String scopedPuName);

    @Message(id = 71, value = "Deployment '%s' specified more than one Hibernate Search module name ('%s','%s')")
    DeploymentUnitProcessingException differentSearchModuleDependencies(String deployment, String searchModuleName1, String searchModuleName2);

    // id = 72, value = "Could not obtain TransactionListenerRegistry from transaction manager")

    @Message(id = 73, value = "Transformation of class %s failed")
    IllegalStateException invalidClassFormat(@Cause IllegalClassFormatException cause, String className);

    @LogMessage(level = INFO)
    @Message(id = 74, value = "Deprecated Hibernate51CompatibilityTransformer is enabled for all application deployments.")
    void hibernate51CompatibilityTransformerEnabled();

}

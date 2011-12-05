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

package org.jboss.as.ee;

import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;
import org.jboss.vfs.VirtualFile;

import static org.jboss.logging.Logger.Level.WARN;

/**
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface EeLogger extends BasicLogger {

    /**
     * A logger with a category of the package name.
     */
    EeLogger ROOT_LOGGER = Logger.getMessageLogger(EeLogger.class, EeLogger.class.getPackage().getName());

    /**
     * A logger with a category of {@code org.jboss.as.server.deployment}.
     */
    EeLogger SERVER_DEPLOYMENT_LOGGER = Logger.getMessageLogger(EeLogger.class, "org.jboss.as.server.deployment");

    /**
     * Logs a warning message indicating the transaction datasource, represented by the {@code className} parameter,
     * could not be proxied and will not be enlisted in the transactions automatically.
     *
     * @param cause     the cause of the error.
     * @param className the datasource class name.
     */
    @LogMessage(level = WARN)
    @Message(id = 11000, value = "Transactional datasource %s could not be proxied and will not be enlisted in transactions automatically")
    void cannotProxyTransactionalDatasource(@Cause Throwable cause, String className);

    /**
     * Logs a warning message indicating the resource-env-ref could not be resolved.
     *
     * @param elementName the name of the element.
     * @param name        the name resource environment reference.
     */
    @LogMessage(level = WARN)
    @Message(id = 11001, value = "Could not resolve %s %s")
    void cannotResolve(String elementName, String name);

    /**
     * Logs a warning message indicating the class path entry, represented by the {@code entry} parameter, was not found
     * in the file.
     *
     * @param entry the class path entry.
     * @param file  the file.
     */
    @LogMessage(level = WARN)
    @Message(id = 11002, value = "Class Path entry %s in %s does not point to a valid jar for a Class-Path reference.")
    void classPathEntryNotAJar(String entry, VirtualFile file);

    /**
     * Logs a warning message indicating the class path entry in file may not point to a sub deployment.
     *
     * @param file the file.
     */
    @LogMessage(level = WARN)
    @Message(id = 11003, value = "Class Path entry in %s may not point to a sub deployment.")
    void classPathEntryASubDeployment(VirtualFile file);

    /**
     * Logs a warning message indicating the class path entry, represented by the {@code entry} parameter, was not found
     * in the file.
     *
     * @param entry the class path entry.
     * @param file  the file.
     */
    @LogMessage(level = WARN)
    @Message(id = 11004, value = "Class Path entry %s in %s not found.")
    void classPathEntryNotFound(String entry, VirtualFile file);

    /**
     * Logs a warning message indicating a failure to destroy the component instance.
     *
     * @param cause     the cause of the error.
     * @param component the component instance.
     */
    @LogMessage(level = WARN)
    @Message(id = 11005, value = "Failed to destroy component instance %s")
    void componentDestroyFailure(@Cause Throwable cause, ComponentInstance component);

    /**
     * Logs a warning message indicating the component is not being installed due to an exception.
     *
     * @param cause the cause of the error.
     * @param name  the name of the component.
     */
    @LogMessage(level = WARN)
    @Message(id = 11006, value = "Not installing optional component %s due to exception")
    void componentInstallationFailure(@Cause Throwable cause, String name);

    /**
     * Logs a warning message indicating the property, represented by the {@code name} parameter, is be ignored due to
     * missing on the setter method on the datasource class.
     *
     * @param name          the name of the property.
     * @param methodName    the name of the method.
     * @param parameterType the name of the parameter type.
     * @param className     the name of the datasource class.
     */
    @LogMessage(level = WARN)
    @Message(id = 11007, value = "Ignoring property %s due to missing setter method: %s(%s) on datasource class: %s")
    void ignoringProperty(String name, String methodName, String parameterType, String className);

    /**
     * Logs a warning message indicating the managed bean implementation class MUST NOT be an interface.
     *
     * @param sectionId the section id of the managed bean spec.
     * @param className the class name
     */
    @LogMessage(level = WARN)
    @Message(id = 11008, value = "[Managed Bean spec, section %s] Managed bean implementation class MUST NOT be an interface - " +
            "%s is an interface, hence won't be considered as a managed bean.")
    void invalidManagedBeanAbstractOrFinal(String sectionId, String className);

    /**
     * Logs a warning message indicating the managed bean implementation class MUST NOT be abstract or final.
     *
     * @param sectionId the section id of the managed bean spec.
     * @param className the class name
     */
    @LogMessage(level = WARN)
    @Message(id = 11009, value = "[Managed Bean spec, section %s] Managed bean implementation class MUST NOT be abstract or final - " +
            "%s won't be considered as a managed bean, since it doesn't meet that requirement.")
    void invalidManagedBeanInterface(String sectionId, String className);

    /**
     * Logs a warning message indicating an exception occurred while invoking the pre-destroy on the interceptor
     * component class, represented by the {@code component} parameter.
     *
     * @param cause     the cause of the error.
     * @param component the component.
     */
    @LogMessage(level = WARN)
    @Message(id = 11010, value = "Exception while invoking pre-destroy interceptor for component class: %s")
    void preDestroyInterceptorFailure(@Cause Throwable cause, Class<?> component);

    /**
     * Logs a warning message indicating the transaction datasource, represented by the {@code className} parameter,
     * will not be enlisted in the transaction as the transaction subsystem is not available.
     *
     * @param className the name of the datasource class.
     */
    @LogMessage(level = WARN)
    @Message(id = 11011, value = "Transactional datasource %s will not be enlisted in the transaction as the transaction subsystem is not available")
    void transactionSubsystemNotAvailable(String className);
}

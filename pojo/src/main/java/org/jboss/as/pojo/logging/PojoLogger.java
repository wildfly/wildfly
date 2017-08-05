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

package org.jboss.as.pojo.logging;

import org.jboss.as.pojo.descriptor.BeanMetaDataConfig;
import org.jboss.as.pojo.descriptor.ConfigVisitorNode;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.msc.service.StartException;
import org.jboss.vfs.VirtualFile;

import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.util.Set;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
@MessageLogger(projectCode = "WFLYPOJO", length = 4)
public interface PojoLogger extends BasicLogger {

    /**
     * A logger with a category of the package name.
     */
    PojoLogger ROOT_LOGGER = Logger.getMessageLogger(PojoLogger.class, "org.jboss.as.pojo");

    /**
     * Log old namespace usage.
     *
     * @param namespace the namespace
     */
    @LogMessage(level = INFO)
    @Message(id = 1, value = "Found legacy bean/pojo namespace: %s - might be missing some xml features (potential exceptions).")
    void oldNamespace(Object namespace);

    /**
     * Error at uninstall.
     *
     * @param joinpoint the joinpoint
     * @param cause the cause of the error.
     */
    @LogMessage(level = WARN)
    @Message(id = 2, value = "Ignoring uninstall action on target: %s")
    void ignoreUninstallError(Object joinpoint, @Cause Throwable cause);

    /**
     * Error invoking callback.
     *
     * @param callback the callback
     * @param cause the cause of the error.
     */
    @LogMessage(level = WARN)
    @Message(id = 3, value = "Error invoking callback: %s")
    void invokingCallback(Object callback, @Cause Throwable cause);

    /**
     * Error at incallback.
     *
     * @param callback the callback
     * @param cause the cause of the error.
     */
    @LogMessage(level = WARN)
    @Message(id = 4, value = "Error invoking incallback: %s")
    void errorAtIncallback(Object callback, @Cause Throwable cause);

    /**
     * Error at uncallback.
     *
     * @param callback the callback
     * @param cause the cause of the error.
     */
    @LogMessage(level = WARN)
    @Message(id = 5, value = "Error invoking uncallback: %s")
    void errorAtUncallback(Object callback, @Cause Throwable cause);

    /**
     * No Module instance found in attachments.
     *
     * @param unit the current deployment unit
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 6, value = "Failed to get module attachment for %s")
    DeploymentUnitProcessingException noModuleFound(DeploymentUnit unit);

    /**
     * Missing reflection index.
     *
     * @param unit the current deployment unit
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 7, value = "Missing deployment reflection index for %s")
    DeploymentUnitProcessingException missingReflectionIndex(DeploymentUnit unit);

    /**
     * Parsing failure.
     *
     * @param file the beans xml file
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 8, value = "Failed to parse POJO xml [ %s ]")
    DeploymentUnitProcessingException failedToParse(VirtualFile file);

//    /**
//     * Cannot instantiate new instance
//     *
//     * @param cause the cause
//     * @return a {@link IllegalArgumentException} for the error.
//     */
//    @Message(id = 9, value = "Cannot instantiate new instance.")
//    IllegalArgumentException cannotInstantiate(@Cause Throwable cause);

    /**
     * Cannot instantiate new collection instance
     *
     * @param cause the cause
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 10, value = "Cannot instantiate new collection instance.")
    IllegalArgumentException cannotInstantiateCollection(@Cause Throwable cause);

    /**
     * Cannot instantiate new map instance
     *
     * @param cause the cause
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 11, value = "Cannot instantiate new map instance.")
    IllegalArgumentException cannotInstantiateMap(@Cause Throwable cause);

    /**
     * Too dynamic to determine type.
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 12, value = "Too dynamic to determine injected type from factory!")
    IllegalArgumentException tooDynamicFromFactory();

    /**
     * Too dynamic to determine type.
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 13, value = "Too dynamic to determine injected type from dependency!")
    IllegalArgumentException tooDynamicFromDependency();

    /**
     * Not a value node.
     *
     * @param previous previous node
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14, value = "Previous node is not a value config: %s")
    IllegalArgumentException notValueConfig(ConfigVisitorNode previous);

    /**
     * Null factory method.
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 15, value = "Null factory method!")
    IllegalArgumentException nullFactoryMethod();

    /**
     * Null bean info.
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 16, value = "Null bean info!")
    IllegalArgumentException nullBeanInfo();

    /**
     * Invalid match size.
     *
     * @param set  whole set
     * @param type the type to match
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 17, value = "Invalid number of type instances match: %s, type: %s")
    IllegalArgumentException invalidMatchSize(Set set, Class type);

    /**
     * Cannot determine injected type.
     *
     * @param info the info
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 18, value = "Cannot determine injected type: %s, try setting class attribute (if available).")
    IllegalArgumentException cannotDetermineInjectedType(String info);

    /**
     * Null or empty alias.
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 19, value = "Null or empty alias.")
    IllegalArgumentException nullOrEmptyAlias();

    /**
     * Null or empty dependency.
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 20, value = "Null or empty dependency.")
    IllegalArgumentException nullOrEmptyDependency();

    /**
     * Missing value.
     *
     * @return a message
     */
    @Message(id = 21, value = "Missing value")
    String missingValue();

    /**
     * Missing mode value.
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 22, value = "Null value")
    IllegalArgumentException nullValue();

    /**
     * Missing mode value.
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 23, value = "Null name")
    IllegalArgumentException nullName();

    /**
     * Missing method name.
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 24, value = "Null method name!")
    IllegalArgumentException nullMethodName();

    /**
     * Unknown type.
     *
     * @param type the type
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 25, value = "Unknown type: %s")
    IllegalArgumentException unknownType(Object type);

    /**
     * Illegal parameter length.
     *
     * @param info the info
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 26, value = "Illegal parameter length: %s")
    IllegalArgumentException illegalParameterLength(Object info);

    /**
     * Missing factory method.
     *
     * @param beanConfig bean config
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 27, value = "Missing factory method in ctor configuration: %s")
    StartException missingFactoryMethod(BeanMetaDataConfig beanConfig);

    /**
     * Missing bean info.
     *
     * @param beanConfig bean config
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 28, value = "Missing bean info, set bean's class attribute: %s")
    String missingBeanInfo(BeanMetaDataConfig beanConfig);

    /**
     * Wrong type size.
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 29, value = "Wrong types size, doesn't match parameters!")
    IllegalArgumentException wrongTypeSize();

    /**
     * Null class info.
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 30, value = "Null ClassInfo!")
    IllegalArgumentException nullClassInfo();

    /**
     * Ctor not found.
     *
     * @param args  the args
     * @param clazz the class
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 31, value = "No such constructor: %s for class %s.")
    IllegalArgumentException ctorNotFound(Object args, String clazz);

    /**
     * Method not found.
     *
     * @param name  the method name
     * @param args  the args
     * @param clazz the class
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 32, value = "Method not found %s%s for class %s.")
    IllegalArgumentException methodNotFound(String name, Object args, String clazz);

    /**
     * Getter not found.
     *
     * @param type  the type
     * @param clazz the class
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 33, value = "No such getter: %s on class %s.")
    IllegalArgumentException getterNotFound(Class<?> type, String clazz);

    /**
     * Setter not found.
     *
     * @param type  the type
     * @param clazz the class
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 34, value = "No such setter: %s on class %s.")
    IllegalArgumentException setterNotFound(Class<?> type, String clazz);

    /**
     * Ambiguous match.
     *
     * @param info the info
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 35, value = "Ambiguous match %s.")
    IllegalArgumentException ambiguousMatch(Object info);

    /**
     * Ambiguous match.
     *
     * @param info  the info
     * @param name  the name
     * @param clazz the class
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 36, value = "Ambiguous match of %s for name %s on class %s.")
    IllegalArgumentException ambiguousMatch(Object info, String name, String clazz);

    /**
     * Field not found.
     *
     * @param name  the method name
     * @param clazz the class
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 37, value = "Field not found %s for class %s.")
    IllegalArgumentException fieldNotFound(String name, String clazz);

    /**
     * Parsing exception.
     *
     * @param beansXml the beans xml file
     * @param cause    the cause
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 38, value = "Exception while parsing POJO descriptor file: %s")
    DeploymentUnitProcessingException parsingException(VirtualFile beansXml, @Cause Throwable cause);
}

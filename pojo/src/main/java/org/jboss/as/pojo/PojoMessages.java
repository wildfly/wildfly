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

package org.jboss.as.pojo;

import org.jboss.as.pojo.descriptor.BeanMetaDataConfig;
import org.jboss.as.pojo.descriptor.ConfigVisitorNode;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.logging.Cause;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;
import org.jboss.msc.service.StartException;
import org.jboss.vfs.VirtualFile;

import java.util.Set;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface PojoMessages {

    /**
     * The messages
     */
    PojoMessages MESSAGES = Messages.getBundle(PojoMessages.class);

    /**
     * No Module instance found in attachments.
     *
     * @param unit the current deployment unit
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 17050, value = "Failed to get module attachment for %s")
    DeploymentUnitProcessingException noModuleFound(DeploymentUnit unit);

    /**
     * Missing reflection index.
     *
     * @param unit the current deployment unit
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 17051, value = "Missing deployment reflection index for %s")
    DeploymentUnitProcessingException missingReflectionIndex(DeploymentUnit unit);

    /**
     * Parsing failure.
     *
     * @param file the beans xml file
     * @return a {@link DeploymentUnitProcessingException} for the error.
     */
    @Message(id = 17052, value = "Failed to parse POJO xml [ %s ]")
    DeploymentUnitProcessingException failedToParse(VirtualFile file);

    /**
     * Cannot instantiate new instance
     *
     * @param cause the cause
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 17053, value = "Cannot instantiate new instance.")
    IllegalArgumentException cannotInstantiate(@Cause Throwable cause);

    /**
     * Cannot instantiate new collection instance
     *
     * @param cause the cause
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 17054, value = "Cannot instantiate new collection instance.")
    IllegalArgumentException cannotInstantiateCollection(@Cause Throwable cause);

    /**
     * Cannot instantiate new map instance
     *
     * @param cause the cause
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 17055, value = "Cannot instantiate new map instance.")
    IllegalArgumentException cannotInstantiateMap(@Cause Throwable cause);

    /**
     * Too dynamic to determine type.
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 17056, value = "Too dynamic to determine injected type from factory!")
    IllegalArgumentException tooDynamicFromFactory();

    /**
     * Too dynamic to determine type.
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 17057, value = "Too dynamic to determine injected type from dependency!")
    IllegalArgumentException tooDynamicFromDependency();

    /**
     * Not a value node.
     *
     * @param previous previous node
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 17058, value = "Previous node is not a value config: %s")
    IllegalArgumentException notValueConfig(ConfigVisitorNode previous);

    /**
     * Null factory method.
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 17059, value = "Null factory method!")
    IllegalArgumentException nullFactoryMethod();

    /**
     * Null bean info.
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 17060, value = "Null bean info!")
    IllegalArgumentException nullBeanInfo();

    /**
     * Invalid match size.
     *
     * @param set whole set
     * @param type the type to match
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 17061, value = "Invalid number of type instances match: %s, type: %s")
    IllegalArgumentException invalidMatchSize(Set set, Class type);

    /**
     * Cannot determine injected type.
     *
     * @param info the info
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 17062, value = "Cannot determine injected type: %s, try setting class attribute (if available).")
    IllegalArgumentException cannotDetermineInjectedType(String info);

    /**
     * Null or empty alias.
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 17063, value = "Null or empty alias.")
    IllegalArgumentException nullOrEmptyAlias();

    /**
     * Null or empty dependency.
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 17064, value = "Null or empty dependency.")
    IllegalArgumentException nullOrEmptyDependency();

    /**
     * Missing value.
     *
     * @return a message
     */
    @Message(id = 17065, value = "Missing value")
    String missingValue();

    /**
     * Missing mode value.
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 17066, value = "Null value")
    IllegalArgumentException nullValue();

    /**
     * Missing mode value.
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 17067, value = "Null name")
    IllegalArgumentException nullName();

    /**
     * Missing method name.
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 17068, value = "Null method name!")
    IllegalArgumentException nullMethodName();

    /**
     * Unknown type.
     *
     * @param type the type
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 17069, value = "Unknown type: %s")
    IllegalArgumentException unknownType(Object type);

    /**
     * Illegal parameter length.
     *
     * @param info the info
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 17070, value = "Illegal parameter length: %s")
    IllegalArgumentException illegalParameterLength(Object info);

    /**
     * Missing factory method.
     *
     * @param beanConfig bean config
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 17071, value = "Missing factory method in ctor configuration: %s")
    StartException missingFactoryMethod(BeanMetaDataConfig beanConfig);

    /**
     * Missing bean info.
     *
     * @param beanConfig bean config
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 17072, value = "Missing bean info, set bean's class attribute: %s")
    String missingBeanInfo(BeanMetaDataConfig beanConfig);

    /**
     * Wrong type size.
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 17073, value = "Wrong types size, doesn't match parameters!")
    IllegalArgumentException wrongTypeSize();

    /**
     * Null class info.
     *
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 17074, value = "Null ClassInfo!")
    IllegalArgumentException nullClassInfo();

    /**
     * Ctor not found.
     *
     * @param args the args
     * @param clazz the class
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 17075, value = "No such constructor: %s for class %s.")
    IllegalArgumentException ctorNotFound(Object args, String clazz);

    /**
     * Method not found.
     *
     * @param name the method name
     * @param args the args
     * @param clazz the class
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 17076, value = "Method not found %s%s for class %s.")
    IllegalArgumentException methodNotFound(String name, Object args, String clazz);

    /**
     * Getter not found.
     *
     * @param type the type
     * @param clazz the class
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 17077, value = "No such getter: %s on class %s.")
    IllegalArgumentException getterNotFound(Class<?> type, String clazz);

    /**
     * Setter not found.
     *
     * @param type the type
     * @param clazz the class
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 17078, value = "No such setter: %s on class %s.")
    IllegalArgumentException setterNotFound(Class<?> type, String clazz);

    /**
     * Ambiguous match.
     *
     * @param info the info
     * @return a {@link IllegalArgumentException} for the error.
     */
    @Message(id = 17079, value = "Ambiguous match %s.")
    IllegalArgumentException ambiguousMatch(Object info);
}

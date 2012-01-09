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

package org.jboss.as.controller;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.parsing.Element;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.controller.registry.OperationEntry.Flag;
import org.jboss.as.protocol.mgmt.RequestProcessingException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.logging.Cause;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;
import org.jboss.logging.Param;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartException;

/**
 * Date: 02.11.2011
 *
 * Reserved logging id ranges from: http://community.jboss.org/wiki/LoggingIds: 14600 - 14899
 *
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface ControllerMessages {

    /**
     * The messages
     */
    ControllerMessages MESSAGES = Messages.getBundle(ControllerMessages.class);

    /**
     * Creates an exception indicating the {@code name} is already defined.
     *
     * @param name     the name that is already defined.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 14630, value = "%s already defined")
    XMLStreamException alreadyDefined(String name, @Param Location location);

    /**
     * Creates an exception indicating the {@code value} has already been declared.
     *
     * @param name     the attribute name.
     * @param value    the value that has already been declared.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 14631, value = "%s %s already declared")
    XMLStreamException alreadyDeclared(String name, String value, @Param Location location);

    /**
     * Creates an exception indicating the {@code value} has already been declared.
     *
     * @param name        the attribute name.
     * @param value       the value that has already been declared.
     * @param parentName  the name of the parent.
     * @param parentValue the parent value.
     * @param location    the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 14632, value = "A %s %s already declared has already been declared in %s %s")
    XMLStreamException alreadyDeclared(String name, String value, String parentName, String parentValue, @Param Location location);

    /**
     * Creates an exception indicating the {@code value} has already been declared.
     *
     * @param name1       the first attribute name.
     * @param name2       the second attribute name.
     * @param value       the value that has already been declared.
     * @param parentName  the name of the parent.
     * @param parentValue the parent value.
     * @param location    the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 14633, value = "A %s or a %s %s already declared has already been declared in %s %s")
    XMLStreamException alreadyDeclared(String name1, String name2, String value, String parentName, String parentValue, @Param Location location);

    /**
     * Creates an exception indicating the {@code type} with the {@code name} is already registered at the
     * {@code location}.
     *
     * @param type     the type.
     * @param name     the name.
     * @param location the location.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14634, value = "An %s named '%s' is already registered at location '%s'")
    IllegalArgumentException alreadyRegistered(String type, String name, String location);

    /**
     * Creates an exception indicating an ambiguous file name was found as there are multiple files ending in the
     * {@code suffix} were found in the directory.
     *
     * @param backupType the backup type.
     * @param searchDir  the search directory.
     * @param suffix     the file suffix.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14635, value = "Ambiguous configuration file name '%s' as there are multiple files in %s that end in %s")
    IllegalStateException ambiguousConfigurationFiles(String backupType, File searchDir, String suffix);

    /**
     * Creates an exception indicating an ambiguous name, represented by the {@code prefix} parameter, was found in
     * the directory, represented by the {@code dir} parameter.
     *
     * @param prefix the file prefix.
     * @param dir    the search directory.
     * @param files  the ambiguous files.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14636, value = "Ambiguous name '%s' in %s: %s")
    IllegalArgumentException ambiguousName(String prefix, String dir, Collection<String> files);

    /**
     * Creates an exception indicating a thread was interrupted waiting for a response for asynch operation.
     *
     * @return a {@link RequestProcessingException} for the error.
     */
    @Message(id = 14637, value = "Thread was interrupted waiting for a response for asynch operation")
    RequestProcessingException asynchOperationThreadInterrupted();

    /**
     * Creates an exception indicating no asynch request with the batch id, represented by the {@code batchId}
     * parameter.
     *
     * @param batchId the batch id.
     *
     * @return a {@link RequestProcessingException} for the error.
     */
    @Message(id = 14638, value = "No asynch request with batch id %d")
    RequestProcessingException asynchRequestNotFound(int batchId);

    /**
     * A message indicating the attribute, represented by the {@code attributeName} parameter, is not writable.
     *
     * @param attributeName the attribute name.
     *
     * @return the message.
     */
    @Message(id = 14639, value = "Attribute %s is not writable")
    String attributeNotWritable(String attributeName);

    /**
     * A message indicating the attribute, represented by the {@code attributeName} parameter, is a registered child of
     * the resource.
     *
     * @param attributeName the name of the attribute.
     * @param resource      the resource the attribute is a child of.
     *
     * @return the message.
     */
    @Message(id = 14640, value = "'%s' is a registered child of resource (%s)")
    String attributeRegisteredOnResource(String attributeName, ModelNode resource);

    /**
     * Creates an exception indicating the inability to determine a default name based on the local host name.
     *
     * @param cause the cause of the error.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 14641, value = "Unable to determine a default name based on the local host name")
    RuntimeException cannotDetermineDefaultName(@Cause Throwable cause);

    /**
     * Creates an exception indicating the file could not be created.
     *
     * @param path the path to the file.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14642, value = "Could not create %s")
    IllegalStateException cannotCreate(String path);

    /**
     * Creates an exception indicating the file could not be deleted.
     *
     * @param file the file to delete.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14643, value = "Could not delete %s")
    IllegalStateException cannotDelete(File file);

    /**
     * Creates an exception indicating a submodel cannot be registered with a {@code null} path.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14644, value = "Cannot register submodels with a null PathElement")
    IllegalArgumentException cannotRegisterSubmodelWithNullPath();

    /**
     * Creates an exception indicating a non-runtime-only submodel cannot be registered with a runtime-only parent.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14645, value = "Cannot register non-runtime-only submodels with a runtime-only parent")
    IllegalArgumentException cannotRegisterSubmodel();

    /**
     * Creates an exception indicating the inability to remove the {@code name}.
     *
     * @param name the name.
     *
     * @return an {@link OperationFailedRuntimeException} for the error.
     */
    @Message(id = 14646, value = "Cannot remove %s")
    OperationFailedRuntimeException cannotRemove(String name);

    /**
     * Creates an exception indicating the file could not be renamed.
     *
     * @param fromPath the from file.
     * @param toPath   the to file.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14647, value = "Could not rename %s to %s")
    IllegalStateException cannotRename(String fromPath, String toPath);

    /**
     * Creates an exception indicating the inability to write the {@code name}.
     *
     * @param name the name.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14648, value = "Cannot write to %s")
    IllegalArgumentException cannotWriteTo(String name);

    /**
     * Creates an exception indicating a child, represented by the {@code childName} parameter, of the parent element,
     * represented by the {@code parentName} parameter, has already been declared.
     *
     * @param childName  the child element name.
     * @param parentName the parent element name.
     * @param location   the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 14649, value = "Child %s of element %s already declared")
    XMLStreamException childAlreadyDeclared(String childName, String parentName, @Param Location location);

    /**
     * Creates an exception indicating the canonical file for the boot file could not be found.
     *
     * @param cause the cause of the error.
     * @param file  the boot file.
     *
     * @return an {@link RuntimeException} for the error.
     */
    @Message(id = 14650, value = "Could not get canonical file for boot file: %s")
    RuntimeException canonicalBootFileNotFound(@Cause Throwable cause, File file);

    /**
     * Creates an exception indicating the canonical file for the main file could not be found.
     *
     * @param cause the cause of the error.
     * @param file  the main file.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14651, value = "Could not get canonical file for main file: %s")
    IllegalStateException canonicalMainFileNotFound(@Cause Throwable cause, File file);

    /**
     * A message indicating the channel is closed.
     *
     * @return the message.
     */
    @Message(id = 14652, value = "Channel closed")
    String channelClosed();

    /**
     * A message indicating the composite operation failed and was rolled back.
     *
     * @return the message.
     */
    @Message(id = 14653, value = "Composite operation failed and was rolled back. Steps that failed:")
    String compositeOperationFailed();

    /**
     * A message indicating the composite operation was rolled back.
     *
     * @return the message.
     */
    @Message(id = 14654, value = "Composite operation was rolled back")
    String compositeOperationRolledBack();

    /**
     * Creates an exception indicating a configuration file whose complete name is the same as the {@code backupType} is
     * not allowed.
     *
     * @param backupType the backup type.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14655, value = "Configuration files whose complete name is %s are not allowed")
    IllegalArgumentException configurationFileNameNotAllowed(String backupType);

    /**
     * Creates an exception indicating no configuration file ending in the {@code suffix} was found in the directory,
     * represented by the {@code dir} parameter.
     *
     * @param suffix the suffix.
     * @param dir    the search directory.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14656, value = "No configuration file ending in %s found in %s")
    IllegalStateException configurationFileNotFound(String suffix, File dir);

    /**
     * Creates an exception indicating the directory. represented by the {@code pathName} parameter, was not found.
     *
     * @param pathName the path name.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14657, value = "No directory %s was found")
    IllegalArgumentException directoryNotFound(String pathName);

    /**
     * Creates an exception indicating either the {@code remoteName} or the {@code localName} domain controller
     * configuration must be declared.
     *
     * @param remoteName the remote element name.
     * @param localName  the local element name.
     * @param location   the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 14658, value = "Either a %s or %s domain controller configuration must be declared.")
    XMLStreamException domainControllerMustBeDeclared(String remoteName, String localName, @Param Location location);

    /**
     * Creates an exception indicating an attribute, represented by the {@code name} parameter, has already been
     * declared.
     *
     * @param name     the attribute name.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 14659, value = "An attribute named '%s' has already been declared")
    XMLStreamException duplicateAttribute(String name, @Param Location location);

    /**
     * Creates an exception indicating a duplicate declaration.
     *
     * @param name     the name of the duplicate entry.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 14660, value = "Duplicate %s declaration")
    XMLStreamException duplicateDeclaration(String name, @Param Location location);

    /**
     * Creates an exception indicating a duplicate declaration.
     *
     * @param name     the name of the duplicate entry.
     * @param value    the duplicate entry.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 14661, value = "Duplicate %s declaration %s")
    XMLStreamException duplicateDeclaration(String name, String value, @Param Location location);

    /**
     * Creates an exception indicating ad duplicate path element, represented by the {@code name} parameter, was found.
     *
     * @param name the name of the duplicate entry.
     *
     * @return an {@link OperationFailedRuntimeException} for the error.
     */
    @Message(id = 14662, value = "Duplicate path element '%s' found")
    OperationFailedRuntimeException duplicateElement(String name);

    /**
     * Creates an exception indicating a duplicate interface declaration.
     *
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 14663, value = "Duplicate interface declaration")
    XMLStreamException duplicateInterfaceDeclaration(@Param Location location);

    /**
     * Creates an exception indicating an element, represented by the {@code name} parameter, has already been
     * declared.
     *
     * @param name     the element name.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 14664, value = "An element of this type named '%s' has already been declared")
    XMLStreamException duplicateNamedElement(String name, @Param Location location);

    /**
     * Creates an exception indicating a duplicate profile was included.
     *
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 14665, value = "Duplicate profile included")
    XMLStreamException duplicateProfile(@Param Location location);

    /**
     * Creates an exception indicating the resource is a duplicate.
     *
     * @param name the name of the resource.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14666, value = "Duplicate resource %s")
    IllegalStateException duplicateResource(String name);

    /**
     * Creates an exception indicating the resource type is a duplicate.
     *
     * @param type the duplicate type.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14667, value = "Duplicate resource type %s")
    IllegalStateException duplicateResourceType(String type);

    /**
     * A message indicating the element, represented by the {@code name} parameter, is not supported the file,
     * represented by the {@code file} parameter.
     *
     * @param name     the name of the element.
     * @param fileName the file name.
     *
     * @return the message.
     */
    @Message(id = 14668, value = "Element %s is not supported in a %s file")
    String elementNotSupported(String name, String fileName);

    /**
     * A message indicating an error waiting for Tx commit/rollback.
     *
     * @return the message.
     */
    @Message(id = 14669, value = "Error waiting for Tx commit/rollback")
    String errorWaitingForTransaction();

    /**
     * Creates an exception indicating a failure to initialize the module.
     *
     * @param cause the cause of the error.
     * @param name  the name of the module.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 14670, value = "Failed initializing module %s")
    RuntimeException failedInitializingModule(@Cause Throwable cause, String name);

    /**
     * A message indicating the failed services.
     *
     * @return the message.
     */
    @Message(id = 14671, value = "Failed services")
    String failedServices();

    /**
     * Creates an exception indicating a failure to backup the file, represented by the {@code file} parameter.
     *
     * @param cause the cause of the error.
     * @param file  the file that failed to backup.
     *
     * @return a {@link ConfigurationPersistenceException} for the error.
     */
    @Message(id = 14672, value = "Failed to back up %s")
    ConfigurationPersistenceException failedToBackup(@Cause Throwable cause, File file);

    /**
     * Creates an exception indicating a failure to create backup copies of configuration the file, represented by the
     * {@code file} parameter.
     *
     * @param cause the cause of the error.
     * @param file  the configuration file that failed to backup.
     *
     * @return a {@link ConfigurationPersistenceException} for the error.
     */
    @Message(id = 14673, value = "Failed to create backup copies of configuration file %s")
    ConfigurationPersistenceException failedToCreateConfigurationBackup(@Cause Throwable cause, File file);

    /**
     * Creates an exception indicating a failure to load a module.
     *
     * @param cause the cause of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 14674, value = "Failed to load module")
    XMLStreamException failedToLoadModule(@Cause Throwable cause);

    /**
     * Creates an exception indicating a failure to load a module.
     *
     * @param cause the cause of the error.
     * @param name  the module name.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = Message.INHERIT, value = "Failed to load module %s")
    XMLStreamException failedToLoadModule(@Cause Throwable cause, String name);

    /**
     * Creates an exception indicating a failure to marshal the configuration.
     *
     * @param cause the cause of the error.
     *
     * @return a {@link ConfigurationPersistenceException} for the error.
     */
    @Message(id = 14675, value = "Failed to marshal configuration")
    ConfigurationPersistenceException failedToMarshalConfiguration(@Cause Throwable cause);

    /**
     * Creates an exception indicating a failure to parse the configuration.
     *
     * @param cause the cause of the error.
     *
     * @return a {@link ConfigurationPersistenceException} for the error.
     */
    @Message(id = 14676, value = "Failed to parse configuration")
    ConfigurationPersistenceException failedToParseConfiguration(@Cause Throwable cause);

    /**
     * Logs an error message indicating a failure to persist configuration change.
     *
     * @param cause the cause of the error.
     *
     * @return the message.
     */
    @Message(id = 14677, value = "Failed to persist configuration change: %s")
    String failedToPersistConfigurationChange(String cause);


    /**
     * Creates an exception indicating a failure to store the configuration.
     *
     * @param cause the cause of the error.
     *
     * @return a {@link ConfigurationPersistenceException} for the error.
     */
    @Message(id = 14678, value = "Failed to store configuration")
    ConfigurationPersistenceException failedToStoreConfiguration(@Cause Throwable cause);

    /**
     * Creates an exception indicating a failure to take a snapshot of the file, represented by the {@code file}
     * parameter.
     *
     * @param cause    the cause of the error.
     * @param file     the file that failed to take the snapshot of.
     * @param snapshot the snapshot file.
     *
     * @return a {@link ConfigurationPersistenceException} for the error.
     */
    @Message(id = 14679, value = "Failed to take a snapshot of %s to %s")
    ConfigurationPersistenceException failedToTakeSnapshot(@Cause Throwable cause, File file, File snapshot);

    /**
     * Creates an exception indicating a failure to write the configuration.
     *
     * @param cause the cause of the error.
     *
     * @return a {@link ConfigurationPersistenceException} for the error.
     */
    @Message(id = 14680, value = "Failed to write configuration")
    ConfigurationPersistenceException failedToWriteConfiguration(@Cause Throwable cause);

    /**
     * Creates an exception indicating {@code path1} does not exist.
     *
     * @param path1 the first non-existing path.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14681, value = "%s does not exist")
    IllegalArgumentException fileNotFound(String path1);

    /**
     * Creates an exception indicating no files beginning with the {@code prefix} were found in the directory,
     * represented by the {@code dir} parameter.
     *
     * @param prefix the file prefix.
     * @param dir    the search directory.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14682, value = "No files beginning with '%s' found in %s")
    IllegalArgumentException fileNotFoundWithPrefix(String prefix, String dir);

    /**
     * Creates an exception indicating the {@code clazz} cannot be used except in a full server boot.
     *
     * @param clazz the class that cannot be used.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14683, value = "%s cannot be used except in a full server boot")
    IllegalStateException fullServerBootRequired(Class<?> clazz);

    /**
     * A message indicating that no included group with the name, represented by the {@code name} parameter, was found.
     *
     * @param name the name of the group.
     *
     * @return the message.
     */
    @Message(id = 14684, value = "No included group with name %s found")
    String groupNotFound(String name);

    /**
     * A message indicating the interface criteria must be of the type represented by the {@code valueType} parameter.
     *
     * @param invalidType the invalid type.
     * @param validType   the valid type.
     *
     * @return the message.
     */
    @Message(id = 14685, value = "Illegal interface criteria type %s; must be %s")
    String illegalInterfaceCriteria(ModelType invalidType, ModelType validType);

    /**
     * A message indicating the value, represented by the {@code valueType} parameter, is invalid for the interface
     * criteria, represented by the {@code id} parameter.
     *
     * @param valueType the type of the invalid value.
     * @param id        the id of the criteria interface.
     * @param validType the valid type.
     *
     * @return the message.
     */
    @Message(id = 14686, value = "Illegal value %s for interface criteria %s; must be %s")
    String illegalValueForInterfaceCriteria(ModelType valueType, String id, ModelType validType);

    /**
     * Creates an exception indicating the resource is immutable.
     *
     * @return an {@link UnsupportedOperationException} for the error.
     */
    @Message(id = 14687, value = "Resource is immutable")
    UnsupportedOperationException immutableResource();

    /**
     * A message indicating the type is invalid.
     *
     * @param name        the name the invalid type was found for.
     * @param validTypes  a collection of valid types.
     * @param invalidType the invalid type.
     *
     * @return the message.
     */
    @Message(id = 14688, value = "Wrong type for %s. Expected %s but was %s")
    String incorrectType(String name, Collection<ModelType> validTypes, ModelType invalidType);

    /**
     * A message indicating interrupted while waiting for request.
     *
     * @return the message.
     */
    @Message(id = 14689, value = "Interrupted while waiting for request")
    String interruptedWaitingForRequest();

    /**
     * A message indicating the {@code name} is invalid.
     *
     * @param name the name of the invalid attribute.
     *
     * @return the message.
     */
    @Message(id = 14690, value = "%s is invalid")
    String invalid(String name);

    /**
     * A message indicating the {@code value} is invalid.
     *
     * @param cause    the cause of the error.
     * @param value    the invalid value.
     * @param name     the name of the invalid attribute.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 14691, value = "%d is not a valid %s")
    XMLStreamException invalid(@Cause Throwable cause, int value, String name, @Param Location location);

    /**
     * A message indicating the address, represented by the {@code address} parameter, is invalid.
     *
     * @param address the invalid address.
     * @param msg     the error message.
     *
     * @return the message.
     */
    @Message(id = 14692, value = "Invalid address %s (%s)")
    String invalidAddress(String address, String msg);

    /**
     * A message indicating the value, represented by the {@code value} parameter, is invalid and must be of the form
     * address/mask.
     *
     * @param value the invalid value.
     *
     * @return the message.
     */
    @Message(id = 14693, value = "Invalid 'value' %s -- must be of the form address/mask")
    String invalidAddressMaskValue(String value);

    /**
     * A message indicating the mask, represented by the {@code mask} parameter, is invalid.
     *
     * @param mask the invalid mask.
     * @param msg  the error message.
     *
     * @return the message.
     */
    @Message(id = 14694, value = "Invalid mask %s (%s)")
    String  invalidAddressMask(String mask, String msg);

    /**
     * A message indicating the address value, represented by the {@code value} parameter, is invalid.
     *
     * @param value the invalid address value.
     * @param msg   the error message.
     *
     * @return the message.
     */
    @Message(id = 14695, value = "Invalid address %s (%s)")
    String invalidAddressValue(String value, String msg);

    /**
     * A message indicating the attribute, represented by the {@code attributeName} parameter, is invalid in
     * combination with the {@code combos} parameter.
     *
     * @param attributeName the attribute name.
     * @param combos        the combinations.
     *
     * @return the message.
     */
    @Message(id = 14696, value = "%s is invalid in combination with %s")
    String invalidAttributeCombo(String attributeName, StringBuilder combos);

    /**
     * Creates an exception indicating an invalid value, represented by the {@code value} parameter, was found for the
     * attribute, represented by the {@code name} parameter.
     *
     * @param value    the invalid value.
     * @param name     the attribute name.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 14697, value = "Invalid value '%s' for attribute '%s'")
    XMLStreamException invalidAttributeValue(String value, QName name, @Param Location location);

    /**
     * Creates an exception indicating an invalid value, represented by the {@code value} parameter, was found for the
     * attribute, represented by the {@code name} parameter. The value must be between the {@code minInclusive} and
     * {@code maxInclusive} values.
     *
     * @param value        the invalid value.
     * @param name         the attribute name.
     * @param minInclusive the minimum value allowed.
     * @param maxInclusive the maximum value allowed.
     * @param location     the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 14698, value = "Illegal value %d for attribute '%s' must be between %d and %d (inclusive)")
    XMLStreamException invalidAttributeValue(int value, QName name, int minInclusive, int maxInclusive, @Param Location location);

    /**
     * Creates an exception indicating an invalid integer value, represented by the {@code value} parameter, was found
     * for the attribute, represented by the {@code name} parameter.
     *
     * @param cause    the cause of the error.
     * @param value    the invalid value.
     * @param name     the attribute name.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 14699, value = "Illegal value '%s' for attribute '%s' must be an integer")
    XMLStreamException invalidAttributeValueInt(@Cause Throwable cause, String value, QName name, @Param Location location);

    /**
     * A message indicating the pattern, represented by the {@code pattern} parameter, for the interface criteria,
     * represented by the {@code name} parameter, is invalid.
     *
     * @param pattern the pattern.
     * @param name    the interface criteria.
     *
     * @return the message.
     */
    @Message(id = 14700, value = "Invalid pattern %s for interface criteria %s")
    String invalidInterfaceCriteriaPattern(String pattern, String name);

    /**
     * Creates an exception indicating the {@code key} is invalid.
     *
     * @param key the invalid value.
     *
     * @return an {@link OperationFailedRuntimeException} for the error.
     */
    @Message(id = 14701, value = "Invalid key specification %s")
    String invalidPathElementKey(String key);

    /**
     * Creates an exception indicating the load factor must be greater than 0 and less than or equal to 1.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14702, value = "Load factor must be greater than 0 and less than or equal to 1")
    IllegalArgumentException invalidLoadFactor();

    /**
     * A message indicating the {@code value} parameter is invalid and must have a maximum length, represented by the
     * {@code length} parameter.
     *
     * @param value  the invalid value.
     * @param name   the name of the parameter.
     * @param length the maximum length.
     *
     * @return the message.
     */
    @Message(id = 14703, value = "'%s' is an invalid value for parameter %s. Values must have a maximum length of %d characters")
    String invalidMaxLength(String value, String name, int length);

    /**
     * A message indicating the {@code value} parameter is invalid and must have a minimum length, represented by the
     * {@code length} parameter.
     *
     * @param value  the invalid value.
     * @param name   the name of the parameter.
     * @param length the minimum length.
     *
     * @return the message.
     */
    @Message(id = 14704, value = "'%s' is an invalid value for parameter %s. Values must have a minimum length of %d characters")
    String invalidMinLength(String value, String name, int length);

    /**
     * A message indicating the {@code size} is an invalid size for the parameter, represented by the {@code name}
     * parameter.
     *
     * @param size    the invalid size.
     * @param name    the name of the parameter.
     * @param maxSize the maximum size allowed.
     *
     * @return the message
     */
    @Message(id = 14705, value = "[%d] is an invalid size for parameter %s. A maximum length of [%d] is required")
    String invalidMaxSize(int size, String name, int maxSize);

    /**
     * A message indicating the {@code size} is an invalid size for the parameter, represented by the {@code name}
     * parameter.
     *
     * @param size    the invalid size.
     * @param name    the name of the parameter.
     * @param minSize the minimum size allowed.
     *
     * @return the message
     */
    @Message(id = 14706, value = "[%d] is an invalid size for parameter %s. A minimum length of [%d] is required")
    String invalidMinSize(int size, String name, int minSize);

    /**
     * A message indicating the {@code value} is invalid for the parameter, represented by the {@code name} parameter.
     *
     * @param value    the invalid value.
     * @param name     the name of the parameter.
     * @param maxValue the minimum value required.
     *
     * @return the message.
     */
    @Message(id = 14707, value = "%d is an invalid value for parameter %s. A maximum value of %d is required")
    String invalidMaxValue(int value, String name, int maxValue);

    /**
     * A message indicating the {@code value} is invalid for the parameter, represented by the {@code name} parameter.
     *
     * @param value    the invalid value.
     * @param name     the name of the parameter.
     * @param maxValue the minimum value required.
     *
     * @return the message.
     */
    String invalidMaxValue(long value, String name, long maxValue);

    /**
     * A message indicating the {@code value} is invalid for the parameter, represented by the {@code name} parameter.
     *
     * @param value    the invalid value.
     * @param name     the name of the parameter.
     * @param minValue the minimum value required.
     *
     * @return the message.
     */
    @Message(id = 14708, value = "%d is an invalid value for parameter %s. A minimum value of %d is required")
    String invalidMinValue(int value, String name, int minValue);

    /**
     * A message indicating the {@code value} is invalid for the parameter, represented by the {@code name} parameter.
     *
     * @param value    the invalid value.
     * @param name     the name of the parameter.
     * @param minValue the minimum value required.
     *
     * @return the message.
     */
    String invalidMinValue(long value, String name, long minValue);

    /**
     * Creates an exception indicated an invalid modification after completed ste.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14709, value = "Invalid modification after completed step")
    IllegalStateException invalidModificationAfterCompletedStep();

    /**
     * Creates an exception indicating the {@code value} for the attribute, represented by the {@code name} parameter,
     * is not a valid multicast address.
     *
     * @param value    the invalid value.
     * @param name     the name of the attribute.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 14710, value = "Value %s for attribute %s is not a valid multicast address")
    XMLStreamException invalidMulticastAddress(String value, String name, @Param Location location);

    /**
     * Creates an exception indicating the {@code value} for the attribute, represented by the {@code name} parameter,
     * is not a valid multicast address.
     *
     * @param cause    the cause of the error.
     * @param value    the invalid value.
     * @param name     the name of the attribute.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    XMLStreamException invalidMulticastAddress(@Cause Throwable cause, String value, String name, @Param Location location);

    /**
     * Creates an exception indicating an outbound socket binding cannot have both the {@code localTag} and the
     * {@code remoteTag}.
     *
     * @param name      the name of the socket binding.
     * @param localTag  the local tag.
     * @param remoteTag the remote tag.
     * @param location  the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 14711, value = "An outbound socket binding: %s cannot have both %s as well as a %s at the same time")
    XMLStreamException invalidOutboundSocketBinding(String name, String localTag, String remoteTag, @Param Location location);

    /**
     * Creates an exception indicating the {@code flag} is invalid.
     *
     * @param flag       the invalid flag.
     * @param name       the name of the parameter.
     * @param validFlags a collection of valid flags.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14712, value = "%s is not a valid value for parameter %s -- must be one of %s")
    IllegalArgumentException invalidParameterValue(Flag flag, String name, Collection<Flag> validFlags);

    /**
     * Creates an exception indicating the {@code value} for the attribute, represented by the {@code name} parameter,
     * does not represent a properly hex-encoded SHA1 hash.
     *
     * @param cause    the cause of the error.
     * @param value    the invalid value.
     * @param name     the name of the attribute.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 14713, value = "Value %s for attribute %s does not represent a properly hex-encoded SHA1 hash")
    XMLStreamException invalidSha1Value(@Cause Throwable cause, String value, String name, @Param Location location);

    /**
     * Creates an exception indicating the stage is not valid for the context type.
     *
     * @param stage the stage.
     * @param type  the context type.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14714, value = "Stage %s is not valid for context type %s")
    IllegalStateException invalidStage(OperationContext.Stage stage, OperationContext.Type type);

    /**
     * Creates an exception indicating an invalid step stage specified.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14715, value = "Invalid step stage specified")
    IllegalArgumentException invalidStepStage();

    /**
     * Creates an exception indicating an invalid step stage for this context type.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14716, value = "Invalid step stage for this context type")
    IllegalArgumentException invalidStepStageForContext();

    /**
     * Creates an exception indicating the table cannot have a negative size.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14717, value = "Can not have a negative size table!")
    IllegalArgumentException invalidTableSize();

    /**
     * A message indicating the type, represented by the {@code type} parameter, is invalid.
     *
     * @param type the invalid type.
     *
     * @return the message.
     */
    @Message(id = 14718, value = "Invalid type %s")
    String invalidType(ModelType type);

    /**
     * Creates an exception indicating the {@code value} is invalid.
     *
     * @param value the invalid value.
     *
     * @return an {@link OperationFailedRuntimeException} for the error.
     */
    @Message(id = 14719, value = "Invalid value specification %s")
    String invalidPathElementValue(String value);

    /**
     * A message indicating the {@code value} for the parameter, represented by the {@code name} parameter, is invalid.
     *
     * @param value       the invalid value.
     * @param name        the name of the parameter.
     * @param validValues a collection of valid values.
     *
     * @return the message.
     */
    @Message(id = 14720, value = "Invalid value %s for %s; legal values are %s")
    String invalidValue(String value, String name, Collection<?> validValues);

    /**
     * Creates an exception indicating the {@code value} for the {@code name} must be greater than the minimum value,
     * represented by the {@code minValue} parameter.
     *
     * @param name     the name for the value that cannot be negative.
     * @param value    the invalid value.
     * @param minValue the minimum value.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 14721, value = "Illegal '%s' value %s -- must be greater than %s")
    XMLStreamException invalidValueGreaterThan(String name, int value, int minValue, @Param Location location);

    /**
     * Creates an exception indicating the {@code value} for the {@code name} cannot be negative.
     *
     * @param name     the name for the value that cannot be negative.
     * @param value    the invalid value.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 14722, value = "Illegal '%s' value %s -- cannot be negative")
    XMLStreamException invalidValueNegative(String name, int value, @Param Location location);

    /**
     * Creates an exception indicating there must be one of the elements, represented by the {@code sb} parameter,
     * included.
     *
     * @param sb       the acceptable elements.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 14723, value = "Must include one of the following elements: %s")
    XMLStreamException missingOneOf(StringBuilder sb, @Param Location location);

    /**
     * Creates an exception indicating there are missing required attribute(s).
     *
     * @param sb       the missing attributes.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 14724, value = "Missing required attribute(s): %s")
    XMLStreamException missingRequiredAttributes(StringBuilder sb, @Param Location location);

    /**
     * Creates an exception indicating there are missing required element(s).
     *
     * @param sb       the missing element.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 14725, value = "Missing required element(s): %s")
    XMLStreamException missingRequiredElements(StringBuilder sb, @Param Location location);

    /**
     * Creates an exception indicating an interruption awaiting to load the module.
     *
     * @param name the name of the module.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 14726, value = "Interrupted awaiting loading of module %s")
    XMLStreamException moduleLoadingInterrupted(String name);

    /**
     * Creates an exception indicating an interruption awaiting to initialize the module.
     *
     * @param name the name of the module.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 14727, value = "Interrupted awaiting initialization of module %s")
    RuntimeException moduleInitializationInterrupted(String name);

    /**
     * Creates an exception indicating a model contains multiple nodes.
     *
     * @param name the name of the node.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14728, value = "Model contains multiple %s nodes")
    IllegalStateException multipleModelNodes(String name);

    /**
     * A message indicating a namespace with the prefix, represented by the {@code prefix} parameter, is already
     * registered with the schema URI, represented by the {@code uri} parameter.
     *
     * @param prefix the namespace prefix.
     * @param uri    the schema URI.
     *
     * @return the message.
     */
    @Message(id = 14729, value = "Namespace with prefix %s already registered with schema URI %s")
    String namespaceAlreadyRegistered(String prefix, String uri);

    /**
     * A message indicating no namespace with the URI {@code prefix}, was found.
     *
     * @param prefix the prefix.
     *
     * @return the message.
     */
    @Message(id = 14730, value = "No namespace with URI %s found")
    String namespaceNotFound(String prefix);

    /**
     * A message indicating the element, represented by the {@code element} parameter, does not allow nesting.
     *
     * @param element the element.
     *
     * @return the message.
     */
    @Message(id = 14731, value = "Nested %s not allowed")
    String nestedElementNotAllowed(Element element);

    /**
     * Creates an exception indicating no active request was found for handling the report represented by the {@code id}
     * parameter.
     *
     * @param id the batch id.
     *
     * @return a {@link RequestProcessingException} for the error.
     */
    @Message(id = 14732, value = "No active request found for handling report %d")
    RequestProcessingException noActiveRequestForHandlingReport(int id);

    /**
     * Creates an exception indicating no active request was found for proxy control represented by the {@code id}
     * parameter.
     *
     * @param id the batch id.
     *
     * @return a {@link RequestProcessingException} for the error.
     */
    @Message(id = 14733, value = "No active request found for proxy operation control %d")
    RequestProcessingException noActiveRequestForProxyOperation(int id);

    /**
     * Creates an exception indicating no active request was found for reading the inputstream report represented by
     * the {@code id} parameter.
     *
     * @param id the batch id.
     *
     * @return a {@link IOException} for the error.
     */
    @Message(id = 14734, value = "No active request found for reading inputstream report %d")
    IOException noActiveRequestForReadingInputStreamReport(int id);

    /**
     * Creates an exception indicating there is no active step.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14735, value = "No active step")
    IllegalStateException noActiveStep();

    /**
     * Creates an exception indicating no active transaction found for the {@code id}.
     *
     * @param id the id.
     *
     * @return a {@link RequestProcessingException} for the error.
     */
    @Message(id = 14736, value = "No active tx found for id %d")
    RuntimeException noActiveTransaction(int id);

    /**
     * A message indicating there is no child registry for the child, represented by the {@code childType} and
     * {@code child} parameters.
     *
     * @param childType the child type.
     * @param child     the child.
     *
     * @return the message.
     */
    @Message(id = 14737, value = "No child registry for (%s, %s)")
    String noChildRegistry(String childType, String child);

    /**
     * Creates an exception indicating no child type for the {@code name}.
     *
     * @param name the name.
     *
     * @return an {@link OperationFailedRuntimeException} for the error.
     */
    @Message(id = 14738, value = "No child type %s")
    OperationFailedRuntimeException noChildType(String name);

    /**
     * A message indicating no handler for the step operation, represented by the {@code stepOpName} parameter, at
     * {@code address}.
     *
     * @param stepOpName the step operation name.
     * @param address    the address.
     *
     * @return the message.
     */
    @Message(id = 14739, value = "No handler for %s at address %s")
    String noHandler(String stepOpName, PathAddress address);

    /**
     * A message indicating that no interface criteria was provided.
     *
     * @return the message.
     */
    @Message(id = 14740, value = "No interface criteria was provided")
    String noInterfaceCriteria();

    /**
     * A message indicating there is no operation handler.
     *
     * @return the message.
     */
    @Message(id = 14741, value = "No operation handler")
    String noOperationHandler();

    /**
     * Creates an exception indicating a node is already registered at the location.
     *
     * @param location the location the node is registered at.
     * @param value    the node value.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14742, value = "A node is already registered at '%s%s)'")
    IllegalArgumentException nodeAlreadyRegistered(String location, String value);

    /**
     * Creates an exception indicating the {@code path} is not a directory.
     *
     * @param path the path.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14743, value = "%s is not a directory")
    IllegalStateException notADirectory(String path);

    /**
     * Creates an exception indicating no {@code path/className} was found for the module identifier.
     *
     * @param path      the path of the SPI.
     * @param className the class name.
     * @param id        the module identifier.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14744, value = "No %s%s found for %s")
    IllegalStateException notFound(String path, String className, ModuleIdentifier id);

    /**
     * Creates an exception indicating an asynchronous operation cannot execute without an executor.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14745, value = "Cannot execute asynchronous operation without an executor")
    IllegalStateException nullAsynchronousExecutor();

    /**
     * A message indicating the {@code name} may not be {@code null}.
     *
     * @param name the name that cannot be {@code null}.
     *
     * @return the message.
     */
    @Message(id = 14746, value = "%s may not be null")
    String nullNotAllowed(String name);

    /**
     * Creates an exception indicating the variable, represented by the {@code name} parameter, was {@code null}.
     *
     * @param name the name of the variable that was {@code null}.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14747, value = "%s is null")
    IllegalArgumentException nullVar(String name);

    /**
     * Creates a message indicating the operation step.
     *
     * @param step the step.
     *
     * @return the message.
     */
    @Message("Operation %s")
    String operation(String step);

    /**
     * Creates an exception indicating the operation is already complete.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14748, value = "Operation already complete")
    IllegalStateException operationAlreadyComplete();

    /**
     * Creates a message indicating the operation is cancelled.
     *
     * @return the message.
     */
    @Message("Operation cancelled")
    String operationCancelled();

    /**
     * Creates a message indicating the operation is cancelled asynchronously.
     *
     * @return a {@link CancellationException} for the error.
     */
    @Message("Operation cancelled asynchronously")
    CancellationException operationCancelledAsynchronously();

    /**
     * A message indicating the operation handler failed.
     *
     * @param msg the failure message.
     *
     * @return the message.
     */
    @Message(id = 14749, value = "Operation handler failed: %s")
    String operationHandlerFailed(String msg);

    /**
     * A message indicating the operation handler failed to complete.
     *
     * @return the message.
     */
    @Message(id = 14750, value = "Operation handler failed to complete")
    String operationHandlerFailedToComplete();

    /**
     * A message indicating the operation is rolling back.
     *
     * @return the message.
     */
    @Message(id = 14751, value = "Operation rolling back")
    String operationRollingBack();

    /**
     * A message indicating the operation succeeded and is committing.
     *
     * @return the message.
     */
    @Message(id = 14752, value = "Operation succeeded, committing")
    String operationSucceeded();

    /**
     * A message indicating there is no operation, represented by the {@code op} parameter, registered at the address,
     * represented by the {@code address} parameter.
     *
     * @param op      the operation.
     * @param address the address.
     *
     * @return the message.
     */
    @Message(id = 14753, value = "There is no operation %s registered at address %s")
    String operationNotRegistered(String op, PathAddress address);


    /**
     * Creates an exception indicating an operation reply value type description is required but was not implemented
     * for the operation represented by the {@code operationName} parameter.
     *
     * @param operationName the name of the operation that requires the reply value type description.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14754, value = "An operation reply value type description is required but was not implemented for operation %s")
    IllegalStateException operationReplyValueTypeRequired(String operationName);

    /**
     * A message indicating there was a parsing problem.
     *
     * @param row the row the problem occurred at.
     * @param col the column the problem occurred at.
     * @param msg a message to concatenate.
     *
     * @return the message.
     */
    @Message(id = 14755, value = "Parsing problem at [row,col]:[%d ,%d]%nMessage: %s")
    String parsingProblem(int row, int col, String msg);

    /**
     * Creates an exception indicating no configuration persister was injected.
     *
     * @return a {@link StartException} for the error.
     */
    @Message(id = 14756, value = "No configuration persister was injected")
    StartException persisterNotInjected();

    /**
     * Creates an exception indicating the thread was interrupted waiting for the operation to prepare/fail.
     *
     * @return a {@link RequestProcessingException} for the error.
     */
    @Message(id = 14757, value = "Thread was interrupted waiting for the operation to prepare/fail")
    RequestProcessingException prepareFailThreadInterrupted();

    /**
     * Creates an exception indicating the profile has no subsystem configurations.
     *
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 14758, value = "Profile has no subsystem configurations")
    XMLStreamException profileHasNoSubsystems(@Param Location location);

    /**
     * Creates an exception indicating no profile found for inclusion.
     *
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 14759, value = "No profile found for inclusion")
    XMLStreamException profileNotFound(@Param Location location);

    /**
     * Creates an exception indicating the proxy handler is already registered at the location.
     *
     * @param location the location.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14760, value = "A proxy handler is already registered at location '%s'")
    IllegalArgumentException proxyHandlerAlreadyRegistered(String location);

    /**
     * Creates an exception indicating a thread was interrupted waiting to read attachment input stream from a remote
     * caller.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 14761, value = "Thread was interrupted waiting to read attachment input stream from remote caller")
    RuntimeException remoteCallerThreadInterrupted();

    /**
     * A message indicating that removing services has lead to unsatisfied dependencies.
     * <p/>
     * ** Note: Use with {@link #removingServiceUnsatisfiedDependencies(String)}
     *
     * @return the message.
     */
    @Message(id = 14762, value = "Removing services has lead to unsatisfied dependencies:")
    String removingServiceUnsatisfiedDependencies();

    /**
     * A message indicating that removing services has lead to unsatisfied dependencies.
     * <p/>
     * ** Note: Use with {@link #removingServiceUnsatisfiedDependencies()}
     *
     * @param name the name of the service.
     *
     * @return the message.
     */
    @Message("%nService %s was depended upon by ")
    String removingServiceUnsatisfiedDependencies(String name);

    /**
     * A message indicating the {@code name} is required.
     *
     * @param name the name of the required attribute.
     *
     * @return the message.
     */
    @Message(id = 14763, value = "%s is required")
    String required(String name);

    /**
     * Creates an exception indicating the {@code name} is reserved.
     *
     * @param name     the name that is reserved.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 14764, value = "%s is reserved")
    XMLStreamException reserved(String name, @Param Location location);

    /**
     * A message indicating a resource does not exist.
     *
     * @param resource the resource.
     *
     * @return the message.
     */
    @Message(id = 14765, value = "Resource does not exist: %s")
    String resourceNotFound(ModelNode resource);

    /**
     * Creates an exception indicating a resource does not exist.
     *
     * @param ancestor the ancestor path.
     * @param address  the address.
     *
     * @return an {@link OperationFailedRuntimeException} for the error.
     */
    @Message(id = 14766, value = "Resource %s does not exist; a resource at address %s cannot be created until all ancestor resources have been added")
    OperationFailedRuntimeException resourceNotFound(PathAddress ancestor, PathAddress address);

    /**
     * Creates an exception indicating the rollback has already been invoked.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14767, value = "rollback() has already been invoked")
    IllegalStateException rollbackAlreadyInvoked();

    /**
     * A message indicating a schema with URI, represented by the {@code schemaUri} parameter, is already registered
     * with the location, represented by the {@code location} parameter.
     *
     * @param schemaUri the schema URI.
     * @param location  the location.
     *
     * @return the message.
     */
    @Message(id = 14768, value = "Schema with URI %s already registered with location %s")
    String schemaAlreadyRegistered(String schemaUri, String location);

    /**
     * A message indicating the schema was not found wit the {@code uri}.
     *
     * @param uri the schema URI.
     *
     * @return the message.
     */
    @Message(id = 14769, value = "No schema location with URI %s found")
    String schemaNotFound(String uri);

    /**
     * Creates an exception indicating the service install was cancelled.
     *
     * @return a {@link CancellationException} for the error.
     */
    @Message(id = 14770, value = "Service install was cancelled")
    CancellationException serviceInstallCancelled();

    /**
     * A message indicating the missing services.
     *
     * @param sb the missing services.
     *
     * @return the message.
     */
    @Message("Missing[%s]")
    String servicesMissing(StringBuilder sb);

    /**
     * A message that indicates there are services with missing or unavailable dependencies.
     *
     * @return the message.
     */
    @Message(id = 14771, value = "Services with missing/unavailable dependencies")
    String servicesMissingDependencies();

    /**
     * Creates an exception indicating the get service registry only supported in runtime operations.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14772, value = "Get service registry only supported in runtime operations")
    IllegalStateException serviceRegistryRuntimeOperationsOnly();

    /**
     * Creates an exception indicating the service removal only supported in runtime operations.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14773, value = "Service removal only supported in runtime operations")
    IllegalStateException serviceRemovalRuntimeOperationsOnly();

    /**
     * A message for the service status report header.
     *
     * @return the message.
     */
    @Message(id = 14774, value = "Service status report%n")
    String serviceStatusReportHeader();

    /**
     * A message for the service status report indicating new missing or unsatisfied dependencies.
     *
     * @return the message.
     */
    @Message(id = 14775, value = "   New missing/unsatisfied dependencies:%n")
    String serviceStatusReportDependencies();

    /**
     * A message for the service status report for missing dependencies.
     *
     * @param serviceName the name of the service
     *
     * @return the message.
     */
    @Message("      %s (missing) dependents: %s %n")
    String serviceStatusReportMissing(ServiceName serviceName, String dependents);

    /**
     * A message for the service status report for unavailable dependencies.
     *
     * @param serviceName the name of the service
     *
     * @return the message.
     */
    @Message("      %s (unavailable) dependents: %s %n")
    String serviceStatusReportUnavailable(ServiceName serviceName, String dependents);

    /**
     * A message for the service status report indicating new corrected service.
     *
     * @return the message.
     */
    @Message(id = 14776, value = "   Newly corrected services:%n")
    String serviceStatusReportCorrected();

    /**
     * A message for the service status report for no longer required dependencies.
     *
     * @param serviceName the name of the service
     *
     * @return the message.
     */
    @Message("      %s (no longer required)%n")
    String serviceStatusReportNoLongerRequired(ServiceName serviceName);

    /**
     * A message for the service status report for unavailable dependencies.
     *
     * @param serviceName the name of the service
     *
     * @return the message.
     */
    @Message("      %s (new available)%n")
    String serviceStatusReportAvailable(ServiceName serviceName);

    /**
     * A message for the service status report for failed services.
     *
     * @return the message.
     */
    @Message(id = 14777, value = "  Services which failed to start:")
    String serviceStatusReportFailed();

    /**
     * Creates an exception indicating the get service target only supported in runtime operations.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14778, value = "Get service target only supported in runtime operations")
    IllegalStateException serviceTargetRuntimeOperationsOnly();

    /**
     * Creates an exception indicating the stage is already complete.
     *
     * @param stage the stage.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14779, value = "Stage %s is already complete")
    IllegalStateException stageAlreadyComplete(OperationContext.Stage stage);

    /**
     * A message indicating the step handler failed after completion.
     *
     * @param handler the handler that failed.
     *
     * @return the message.
     */
    @Message(id = 14780, value = "Step handler %s failed after completion")
    String stepHandlerFailed(OperationStepHandler handler);

    /**
     * A message indicating the step handler for the operation failed handling operation rollback.
     *
     * @param handler the handler that failed.
     * @param op      the operation.
     * @param address the path address.
     * @param msg     the error message.
     *
     * @return the message.
     */
    @Message(id = 14781, value = "Step handler %s for operation %s at address %s failed handling operation rollback -- %s")
    String stepHandlerFailedRollback(OperationStepHandler handler, String op, PathAddress address, String msg);

    /**
     * A message indicating an interruption awaiting subsystem boot operation execution.
     *
     * @return the message.
     */
    @Message(id = 14782, value = "Interrupted awaiting subsystem boot operation execution")
    String subsystemBootInterrupted();

    /**
     * A message indicating the boot operations for the subsystem, represented by the {@code name} parameter, failed
     * without explanation.
     *
     * @param name the name of the subsystem.
     *
     * @return the message.
     */
    @Message(id = 14783, value = "Boot operations for subsystem %s failed without explanation")
    String subsystemBootOperationFailed(String name);

    /**
     * A message indicating a failure executing subsystem boot operations, but no individual operation failed.
     *
     * @return the message.
     */
    @Message(id = 14784, value = "Failed executing subsystem %s boot operations but no individual operation failed")
    String subsystemBootOperationFailedExecuting(String name);

    /**
     * Creates an exception indicating the table is full.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14785, value = "Table is full!")
    IllegalStateException tableIsFull();

    /**
     * Creates an exception indicating an interruption awaiting a transaction commit or rollback.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 14786, value = "Interrupted awaiting transaction commit or rollback")
    RuntimeException transactionInterrupted();

    /**
     * Creates an exception indicating a timeout occurred waiting for the transaction.
     *
     * @param type the transaction type.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 14787, value = "A timeout occurred waiting for the transaction to %s")
    RuntimeException transactionTimeout(String type);

    /**
     * Creates an exception indicating an unexpected attribute, represented by the {@code name} parameter, was
     * encountered.
     *
     * @param name     the unexpected attribute name.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 14788, value = "Unexpected attribute '%s' encountered")
    XMLStreamException unexpectedAttribute(QName name, @Param Location location);

    /**
     * Creates an exception indicating an unexpected element, represented by the {@code name} parameter, was
     * encountered.
     *
     * @param name     the unexpected element name.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 14789, value = "Unexpected element '%s' encountered")
    XMLStreamException unexpectedElement(QName name, @Param Location location);

    /**
     * Creates an exception indicating an unexpected end of an element, represented by the {@code name} parameter, was
     * encountered.
     *
     * @param name     the unexpected element name.
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 14790, value = "Unexpected end of element '%s' encountered")
    XMLStreamException unexpectedEndElement(QName name, @Param Location location);

    /**
     * Creates an exception indicating the {@code storage} was unexpected.
     *
     * @param storage the storage that was unexpected.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14791, value = "Unexpected storage %s")
    IllegalStateException unexpectedStorage(Storage storage);

    /**
     * A message indicating the attribute, represented by the {@code name} parameter, is unknown.
     *
     * @param name the attribute name.
     *
     * @return the message.
     */
    @Message(id = 14792, value = "Unknown attribute %s")
    String unknownAttribute(String name);

    /**
     * A message indicating there is no known child type with the name, represented by the {@code name} parameter.
     *
     * @param name the name of the child.
     *
     * @return the message.
     */
    @Message(id = 14793, value = "No known child type named %s")
    String unknownChildType(String name);

    /**
     * Creates an exception indicating the property, represented by the {@code name} parameter, is unknown.
     *
     * @param name the name of the property.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 14794, value = "Unknown property in interface criteria list: %s")
    RuntimeException unknownCriteriaInterfaceProperty(String name);

    /**
     * A message indicating the interface criteria type, represented by the {@code type} parameter, is unknown.
     *
     * @param type the unknown criteria type.
     *
     * @return the message.
     */
    @Message(id = 14795, value = "Unknown interface criteria type %s")
    String unknownCriteriaInterfaceType(String type);

    /**
     * Creates an exception indicating the interface, represented by the {@code value} attribute, for the attribute,
     * represented by the {@code attributeName} parameter, is unknown on in the element.
     *
     * @param value         the value of the attribute.
     * @param attributeName the attribute name.
     * @param elementName   the element name for the attribute.
     * @param location      the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 14796, value = "Unknown interface %s %s must be declared in element %s")
    XMLStreamException unknownInterface(String value, String attributeName, String elementName, @Param Location location);

    /**
     * Creates an exception indicating an unknown {@code elementName1} {@code value} {@code elementName2} must be
     * declared in the element represented by the {@code parentElement} parameter.
     *
     * @param elementName1  the name of the first element.
     * @param value         the value.
     * @param elementName2  the name of the second element.
     * @param parentElement the parent element name.
     * @param location      the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 14797, value = "Unknown %s %s %s must be declared in element %s")
    XMLStreamException unknownValueForElement(String elementName1, String value, String elementName2, String parentElement, @Param Location location);

    /**
     * A message indicating the validation failed.
     *
     * @param name the parameter name the validation failed on.
     *
     * @return the message.
     */
    @Message(id = 14798, value = "Validation failed for %s")
    String validationFailed(String name);

    /**
     * A message indicating that there are more services than would be practical to display
     *
     * @param number the number of services that were not displayed
     *
     * @return the message.
     */
    @Message(id = 14799, value = "... and %s more")
    String andNMore(int number);

    /**
     * Creates an exception indicating an invalid value, represented by the {@code value} parameter, was found for the
     * attribute, represented by the {@code name} parameter.
     *
     * @param value    the invalid value.
     * @param name     the attribute name.
     * @param validValues the legal values for the attribute
     * @param location the location of the error.
     *
     * @return a {@link XMLStreamException} for the error.
     */
    @Message(id = 14800, value = "Invalid value '%s' for attribute '%s' -- valid values are %s")
    XMLStreamException invalidAttributeValue(String value, QName name, Set<String> validValues, @Param Location location);

    /**
     * Creates an exception message indicating an expression could not be resolved due to lack of security permissions.
     *
     * @param toResolve  the node being resolved
     * @param e the SecurityException
     * @return an {@link OperationFailedException} for the caller
     */
    @Message(id = 14801, value = "Caught SecurityException attempting to resolve expression '%s' -- %s")
    String noPermissionToResolveExpression(ModelNode toResolve, SecurityException e);

    /**
     * Creates an exception message indicating an expression could not be resolved due to no corresponding system property
     * or environment variable.
     *
     * @param toResolve  the node being resolved
     * @param e the SecurityException
     * @return an {@link OperationFailedException} for the caller
     */
    @Message(id = 14802, value = "Cannot resolve expression '%s' -- %s")
    String cannotResolveExpression(ModelNode toResolve, IllegalStateException e);

    /**
     * Creates an exception indicating the resource is a duplicate.
     *
     * @param address the address of the resource.
     *
     * @return an {@link OperationFailedRuntimeException} for the error.
     */
    @Message(id = 14803, value = "Duplicate resource %s")
    OperationFailedRuntimeException duplicateResourceAddress(PathAddress address);

    /**
     * Creates an exception indicating a resource cannot be removed due to the existence of child resources.
     *
     * @param children the address elements for the children.
     *
     * @return an {@link OperationFailedException} for the error.
     */
    @Message(id = 14804, value = "Cannot remove resource before removing child resources %s")
    OperationFailedException cannotRemoveResourceWithChildren(List<PathElement> children);

    /**
     * Creates an exception indicating the canonical file for the main file could not be found.
     *
     * @param name  the main file.
     * @param configurationDir the configuration directory
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14805, value = "Could not get main file: %s. Specified files must be relative to the configuration dir: %s")
    IllegalStateException mainFileNotFound(String name, File configurationDir);

    /**
     * Creates an exception indicating a resource cannot be found.
     *
     * @param pathAddress the address for the resource.
     *
     * @return an {@link OperationFailedRuntimeException} for the error.
     */
    @Message(id = 14807, value = "Management resource '%s' not found")
    OperationFailedRuntimeException managementResourceNotFound(PathAddress pathAddress);

    /**
     * Creates an exception message indicating a child resource cannot be found.
     *
     * @param childAddress the address element for the child.
     *
     * @return an message for the error.
     */
    @Message(id = 14808, value = "Child resource '%s' not found")
    String childResourceNotFound(PathElement childAddress);

    /**
     * Creates an exception indicating a node is already registered at the location.
     *
     * @param location the location of the existing node.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14809, value = "A node is already registered at '%s'")
    IllegalArgumentException nodeAlreadyRegistered(String location);

    /**
     * Creates an exception indicating that an attempt was made to remove an extension before removing all of its
     * subsystems.
     *
     * @param moduleName the name of the extension
     * @param subsystem the name of the subsystem
     *
     * @return an {@link IllegalStateException} for the error
     */
    @Message(id = 14810, value = "An attempt was made to unregister extension %s which still has subsystem %s registered")
    IllegalStateException removingExtensionWithRegisteredSubsystem(String moduleName, String subsystem);

    /**
     * Creates an exception indicating that an attempt was made to register an override model for the root model
     * registration.
     *
     * @return an {@link IllegalStateException} for the error
     */
    @Message(id = 14811, value = "An override model registration is not allowed for the root model registration")
    IllegalStateException cannotOverrideRootRegistration();

    /**
     * Creates an exception indicating that an attempt was made to register an override model for a non-wildcard
     * registration.
     *
     * @param valueName the name of the non-wildcard registration that cannot be overridden
     * @return an {@link IllegalStateException} for the error
     */
    @Message(id = 14812, value = "An override model registration is not allowed for non-wildcard model registrations. This registration is for the non-wildcard name '%s'.")
    IllegalStateException cannotOverrideNonWildCardRegistration(String valueName);

    /**
     * Creates an exception indicating that an attempt was made to remove a wildcard model registration via
     * the unregisterOverrideModel API.
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 14813, value = "A registration named '*' is not an override model and cannot be unregistered via the unregisterOverrideModel API.")
    IllegalArgumentException wildcardRegistrationIsNotAnOverride();

    /**
     * Creates an exception indicating that an attempt was made to remove a resource registration from the root registration.
     *
     * @return an {@link IllegalStateException} for the error
     */
    @Message(id = 14814, value = "The root resource registration does not support overrides, so no override can be removed.")
    IllegalStateException rootRegistrationIsNotOverridable();

    /**
     * Creates an exception indicating there is no operation, represented by the {@code op} parameter, registered at the address,
     * represented by the {@code address} parameter.
     *
     * @param op      the operation.
     * @param address the address.
     * @return the message.
     */
    @Message(id = 14815, value = "There is no operation %s registered at address %s")
    IllegalArgumentException operationNotRegisteredException(String op, PathAddress address);


    /**
     * Creates a runtime exception indicating there was a failure to recover services during an operation rollback
     *
     * @param cause the cause of the failure
     * @return the runtime exception.
     */
    @Message(id = 14816, value = "Failed to recover services during operation rollback")
    RuntimeException failedToRecoverServices(@Param OperationFailedException cause);

    /**
     * Creates an IllegalStateException indicating a subsystem with the given name has already been registered by
     * a different extension.
     *
     * @param subsystemName the cause of the failure
     * @return the runtime exception.
     */
    @Message(id = 14817, value = "A subsystem named '%s' cannot be registered by extension '%s' -- a subsystem with that name has already been registered by extension '%s'.")
    IllegalStateException duplicateSubsystem(String subsystemName, String duplicatingModule, String existingModule);

    /**
     * Creates an exception indicating that the operation is missing one of the standard fields.
     *
     * @param field the standard field name
     * @param operation the operation as a string. May be empty
     */
    @Message(id = 14818, value = "Operation has no '%s' field. %s")
    IllegalArgumentException validationFailedOperationHasNoField(String field, String operation);

    /**
     * Creates an exception indicating that the operation has an empty name.
     *
     * @param operation the operation. May be null
     * @param operation the operation as a string. May be empty
     */
    @Message(id = 14819, value = "Operation has a null or empty name. %s")
    IllegalArgumentException validationFailedOperationHasANullOrEmptyName(String operation);

    /**
     * Creates an exception indicating that the operation could not be found
     *
     * @param name the name of the operation
     * @param address the operation address
     * @param operation the operation as a string. May be empty
     */
    @Message(id = 14820, value = "No operation called '%s' at '%s'. %s")
    IllegalArgumentException validationFailedNoOperationFound(String name, PathAddress address, String operation);

    /**
     * Creates an exception indicating that the operation contains a parameter not in its descriptor
     *
     * @param paramName the name of the parameter in the operation
     * @param parameterNames the valid parameter names
     * @param operation the operation as a string. May be empty
     */
    @Message(id = 14821, value = "Operation contains a parameter '%s' which is not one of the expected parameters %s. %s")
    IllegalArgumentException validationFailedActualParameterNotDescribed(String paramName, Set<String> parameterNames, String operation);

    /**
     * Creates an exception indicating that the operation does not contain a required parameter
     *
     * @param paramName the name of the required parameter
     * @param operation the operation as a string. May be empty
     */
    @Message(id = 14822, value = "Required parameter %s is not present. %s")
    IllegalArgumentException validationFailedRequiredParameterNotPresent(String paramName, String operation);

    /**
     * Creates an exception indicating that the operation contains both an alternative and a required parameter
     *
     * @param alternative the name of the alternative parameter
     * @param paramName the name of the required parameter
     * @param operation the operation as a string. May be empty
     */
    @Message(id = 14823, value = "Alternative parameter '%s' for required parameter '%s' was used. Please use one or the other. %s")
    IllegalArgumentException validationFailedRequiredParameterPresentAsWellAsAlternative(String alternative, String paramName, String operation);

    /**
     * Creates an exception indicating that an operation parameter could not be converted to the required type
     *
     * @param paramName the name of the required parameter
     * @param type the required type
     * @param operation the operation as a string. May be empty
     */
    @Message(id = 14824, value = "Could not convert the paramter '%s' to a %s. %s")
    IllegalArgumentException validationFailedCouldNotConvertParamToType(String paramName, ModelType type, String operation);

    /**
     * Creates an exception indicating that an operation parameter value is smaller than the allowed minimum value
     *
     * @param value the name of the required parameter
     * @param paramName the name of the required parameter
     * @param min the minimum value
     * @param operation the operation as a string. May be empty
     */
    @Message(id = 14825, value = "The value '%s' passed in for '%s' is smaller than the minimum value '%s'. %s")
    IllegalArgumentException validationFailedValueIsSmallerThanMin(Number value, String paramName, Number min, String operation);

    /**
     * Creates an exception indicating that an operation parameter value is greater than the allowed minimum value
     *
     * @param value the name of the required parameter
     * @param paramName the name of the required parameter
     * @param max the minimum value
     * @param operation the operation as a string. May be empty
     */
    @Message(id = 14826, value = "The value '%s' passed in for '%s' is bigger than the maximum value '%s'. %s")
    IllegalArgumentException validationFailedValueIsGreaterThanMax(Number value, String paramName, Number max, String operation);

    /**
     * Creates an exception indicating that an operation parameter value is shorter than the allowed minimum length
     *
     * @param value the name of the required parameter
     * @param paramName the name of the required parameter
     * @param minLength the minimum value
     * @param operation the operation as a string. May be empty
     */
    @Message(id = 14827, value = "The value '%s' passed in for '%s' is shorter than the minimum length '%s'. %s")
    IllegalArgumentException validationFailedValueIsShorterThanMinLength(Object value, String paramName, Object minLength, String operation);

    /**
     * Creates an exception indicating that an operation parameter value is longer than the allowed maximum length
     *
     * @param value the name of the required parameter
     * @param paramName the name of the required parameter
     * @param minLength the minimum value
     * @param operation the operation as a string. May be empty
     */
    @Message(id = 14828, value = "The value '%s' passed in for '%s' is longer than the maximum length '%s'. %s")
    IllegalArgumentException validationFailedValueIsLongerThanMaxLength(Object value, String paramName, Object maxLength, String operation);

    /**
     * Creates an exception indicating that an operation parameter list value has an element that is not of the accepted type
     *
     * @param paramName the name of the required parameter
     * @param elementType the expected element type
     * @param operation the operation as a string. May be empty
     */
    @Message(id = 14829, value = "%s is expected to be a list of %s. %s")
    IllegalArgumentException validationFailedInvalidElementType(String paramName, ModelType elementType, String operation);

    /**
     * Creates a string for use in an IllegalArgumentException or a warning message indicating that
     * the required attribute of a parameter in the operation description is not a boolean.
     *
     * @param paramName the name of the parameter
     * @param address the address of the operation
     * @param description the operation description
     */
    @Message(id = 14830, value = "'" + ModelDescriptionConstants.REQUIRED + "' parameter: '%s' must be a boolean in the description of the operation at %s: %s")
    String invalidDescriptionRequiredFlagIsNotABoolean(String paramName, PathAddress address, ModelNode description);

    /**
     * Creates a string for use in an IllegalArgumentException or a warning message indicating that
     * a parameter is undefined in the operation description.
     *
     * @param paramName the name of the parameter
     * @param address the address of the operation
     * @param description the operation description
     */
    @Message(id = 14831, value = "Undefined request property '%s' in description of the operation at %s: %s")
    String invalidDescriptionUndefinedRequestProperty(String name, PathAddress address, ModelNode description);

    /**
     * Creates a string for use in an IllegalArgumentException or a warning message indicating that
     * a parameter has no type in the operation description
     *
     * @param paramName the name of the parameter
     * @param address the address of the operation
     * @param description the operation description
     */
    @Message(id = 14832, value = "There is no type for parameter '%s' in the description of the operation at %s: %s")
    String invalidDescriptionNoParamTypeInDescription(String paramName, PathAddress address, ModelNode description);

    /**
     * Creates a string for use in an IllegalArgumentException or a warning message indicating that
     * a parameter has an invalid type in the operation description
     *
     * @param paramName the name of the parameter
     * @param address the address of the operation
     * @param description the operation description
     */
    @Message(id = 14833, value = "Could not determine the type of parameter '%s' in the description of the operation at %s: %s")
    String invalidDescriptionInvalidParamTypeInDescription(String paramName, PathAddress address, ModelNode description);

    /**
     * Creates a string for use in an IllegalArgumentException or a warning message indicating that
     * a parameter has a min or max attribute value of a different type from its expected value.
     *
     * @param minOrMax {@code min} or {@code max}
     * @param paramName the name of the parameter
     * @param expectedType the expected type
     * @param address the address of the operation
     * @param description the operation description
     */
    @Message(id = 14834, value = "The '%s' attribute of the '%s' parameter can not be converted to its type: %s in the description of the operation at %s: %s")
    String invalidDescriptionMinMaxForParameterHasWrongType(String minOrMax, String paramName, ModelType expectedType, PathAddress address, ModelNode description);

    /**
     * Creates a string for use in an IllegalArgumentException or a warning message indicating that
     * a parameter has a min-length or max-lenght attribute value that is not an integer.
     *
     * @param minOrMaxLength {@code min} or {@code max}
     * @param paramName the name of the parameter
     * @param address the address of the operation
     * @param description the operation description
     */
    @Message(id = 14835, value = "The '%s' attribute of the '%s' parameter can not be converted to an integer in the description of the operation at %s: %s")
    String invalidDescriptionMinMaxLengthForParameterHasWrongType(String minOrMaxLength, String paramName, PathAddress address, ModelNode description);
}

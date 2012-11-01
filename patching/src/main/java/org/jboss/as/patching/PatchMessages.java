/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.patching;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.patching.runner.PatchingException;
import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.logging.annotations.Cause;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * 16840 - 16899
 *
 * @author Emanuel Muckenhuber
 */
@MessageBundle(projectCode = "JBAS")
public interface PatchMessages {

    PatchMessages MESSAGES = Messages.getBundle(PatchMessages.class);

    @Message(id = Message.NONE, value = "Filesystem path of a pristine unzip of the distribution of the version of the " +
            "software to which the generated patch applies")
    String argAppliesToDist();

    /**
     * Instructions for the {@code -h} and {@code --help} command line arguments.
     *
     * @return the instructions.
     */
    @Message(id = Message.NONE, value = "Display this message and exit")
    String argHelp();

    @Message(id = Message.NONE, value = "Filesystem location to which the generated patch file should be written")
    String argOutputFile();

    @Message(id = Message.NONE, value = "Filesystem path of the patch generation configuration file to use")
    String argPatchConfig();

    @Message(id = Message.NONE, value = "Filesystem path of a pristine unzip of a distribution of software which " +
            "contains the changes that should be incorporated in the patch")
    String argUpdatedDist();

    @Message(id = Message.NONE, value = "Usage: %s [args...]%nwhere args include:")
    String patchGeneratorUsageHeadline(String todo);

    /**
     * Instructions for {@code --version} command line argument.
     *
     * @return the instructions.
     */
    @Message(id = Message.NONE, value = "Print version and exit")
    String argVersion();

    @Message(id = 16840, value = "Patch does not apply - expected (%s), but was (%s)")
    PatchingException doesNotApply(List<String> appliesTo, String version);

    @Message(id = 16841, value = "Failed to delete (%s)")
    IOException failedToDelete(String path);

    @Message(id = 16842, value = "Failed to create directory (%s)")
    IOException cannotCreateDirectory(String path);

    @Message(id = 16843, value = "Cannot complete operation. Patch '%s' is currently active")
    OperationFailedException patchActive(String patchId);

    @Message(id = 16845, value = "Failed to show history of patches")
    OperationFailedException failedToShowHistory(@Cause Throwable cause);

    /**
     * A message indicating the argument, represented by the {@code arg} parameter, expected an additional argument.
     *
     * @param arg the argument that expects an additional argument.
     *
     * @return the message.
     */
    @Message(id = 16846, value = "Argument expected for option %s")
    String argumentExpected(String arg);

    @Message(id = 16847, value = "Missing required argument(s): %s")
    String missingRequiredArgs(Set<String> missing);

    @Message(id = 16848, value = "File at path specified by argument %s does not exist")
    String fileDoesNotExist(String arg);

    @Message(id = 16849, value = "File at path specified by argument %s is not a directory")
    String fileIsNotADirectory(String arg);

    @Message(id = 16850, value = "File at path specified by argument %s is a directory")
    String fileIsADirectory(String arg);

    @Message(id = 16851, value = "Unable to apply or rollback a patch when the server is in a reload-required state.")
    OperationFailedException serverRequiresReload();

    @Message(id = 16852, value = "Cannot rollback patch (%s)")
    PatchingException cannotRollbackPatch(String id);

    @Message(id = 16853, value = "Patch '%s' already applied")
    PatchingException alreadyApplied(String patchId);

}

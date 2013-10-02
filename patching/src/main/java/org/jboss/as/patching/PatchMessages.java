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

import java.io.IOException;
import java.io.SyncFailedException;
import java.util.Collection;
import java.util.Set;

import org.jboss.as.patching.metadata.ContentItem;
import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.logging.annotations.Param;

/**
 * This module is using message IDs in the range 16800-16899.
 * <p/>
 * This file is using the subset 16840-16899 for non-logger messages.
 * <p/>
 * See <a href="http://community.jboss.org/docs/DOC-16810">http://community.jboss.org/docs/DOC-16810</a> for the full
 * list of currently reserved JBAS message id blocks.
 * <p/>
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

    @Message(id = Message.NONE, value = "Conflicts detected")
    String detectedConflicts();

    @Message(id = Message.NONE, value = "failed to resolve a jboss.home.dir use the --distribution attribute to point to a valid installation")
    IllegalStateException cliFailedToResolveDistribution();

    @Message(id = Message.NONE, value ="No layers directory found at %s")
    IllegalStateException installationNoLayersConfigFound(String path);

    @Message(id = Message.NONE, value = "Cannot find layer '%s' under directory %s")
    IllegalStateException installationMissingLayer(String layer, String path);

    @Message(id = Message.NONE, value = "no associated module or bundle repository with layer '%s'")
    IllegalStateException installationInvalidLayerConfiguration(String layerName);

    @Message(id = Message.NONE, value = "Duplicate %s '%s'")
    IllegalStateException installationDuplicateLayer(String type, String layer);

    @Message(id = Message.NONE, value = "null input stream")
    IllegalArgumentException nullInputStream();

    @Message(id = Message.NONE, value = "null output stream")
    IllegalArgumentException nullOutputStream();

    @Message(id = Message.NONE, value = "Not a directory %s")
    IllegalStateException notADirectory(String path);

    @Message(id = Message.NONE, value = "patch types don't match")
    IllegalStateException patchTypesDontMatch();

    @Message(id = Message.NONE, value = "invalid rollback information")
    PatchingException invalidRollbackInformation();

    // User related errors

    @Message(id = 16840, value = "Patch does not apply - expected (%s), but was (%s)")
    PatchingException doesNotApply(String appliesTo, String version);

    @Message(id = 16841, value = "Failed to delete (%s)")
    IOException failedToDelete(String path);

    @Message(id = 16842, value = "Failed to create directory (%s)")
    IOException cannotCreateDirectory(String path);

    /**
     * A message indicating the argument, represented by the {@code arg} parameter, expected an additional argument.
     *
     * @param arg the argument that expects an additional argument.
     *
     * @return the message.
     */
    @Message(id = 16843, value = "Argument expected for option %s")
    String argumentExpected(String arg);

    @Message(id = 16844, value = "Missing required argument(s): %s")
    String missingRequiredArgs(Set<String> missing);

    @Message(id = 16845, value = "File at path specified by argument %s does not exist")
    String fileDoesNotExist(String arg);

    @Message(id = 16846, value = "File at path specified by argument %s is not a directory")
    String fileIsNotADirectory(String arg);

    @Message(id = 16847, value = "File at path specified by argument %s is a directory")
    String fileIsADirectory(String arg);

    @Message(id = 16848, value = "Cannot rollback patch (%s)")
    PatchingException cannotRollbackPatch(String id);

    @Message(id = 16849, value = "Patch '%s' already applied")
    PatchingException alreadyApplied(String patchId);

    @Message(id = 16850, value = "There is no layer called %s installed")
    PatchingException noSuchLayer(String name);

    @Message(id = 16851, value = "Failed to resolve a valid patch descriptor for %s %s")
    PatchingException failedToResolvePatch(String product, String version);

    @Message(id = 16852, value = "Requires patch '%s'")
    PatchingException requiresPatch(String patchId);

    @Message(id = 16853, value = "Patch is incompatible with patch '%s'")
    PatchingException incompatiblePatch(String patchId);

    @Message(id = 16854, value = "Conflicts detected")
    ContentConflictsException conflictsDetected(@Param Collection<ContentItem> conflicts);

    @Message(id = 16855, value = "copied content does not match expected hash for item: %s")
    SyncFailedException wrongCopiedContent(ContentItem item);

    @Message(id = 16856, value = "invalid patch name '%s'")
    IllegalArgumentException illegalPatchName(String name);

    @Message(id = 16857, value = "Cannot rollback. No patches applied.")
    IllegalArgumentException noPatchesApplied();

    @Message(id = 16858, value = "Patch '%s' not found in history.")
    PatchingException patchNotFoundInHistory(String patchId);

}

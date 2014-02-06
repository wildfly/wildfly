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

package org.jboss.as.patching.logging;

import static org.jboss.logging.Logger.Level.*;

import java.io.IOException;
import java.io.SyncFailedException;
import java.util.Collection;
import java.util.Set;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.patching.ContentConflictsException;
import org.jboss.as.patching.PatchingException;
import org.jboss.as.patching.metadata.ContentItem;
import org.jboss.as.patching.validation.PatchingArtifact;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Param;

/**
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@MessageLogger(projectCode = "WFLYPAT", length = 4)
public interface PatchLogger extends BasicLogger {

    PatchLogger ROOT_LOGGER = Logger.getMessageLogger(PatchLogger.class, "org.jboss.as.patching");

    @LogMessage(level = WARN)
    @Message(id = 1, value = "Cannot delete file %s")
    void cannotDeleteFile(String name);

    @LogMessage(level = WARN)
    @Message(id = 2, value = "Cannot invalidate %s")
    void cannotInvalidateZip(String name);

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

    @Message(id = 3, value = "Patch does not apply - expected (%s), but was (%s)")
    PatchingException doesNotApply(String appliesTo, String version);

    @Message(id = 4, value = "Failed to delete (%s)")
    IOException failedToDelete(String path);

    @Message(id = 5, value = "Failed to create directory (%s)")
    IOException cannotCreateDirectory(String path);

    /**
     * A message indicating the argument, represented by the {@code arg} parameter, expected an additional argument.
     *
     * @param arg the argument that expects an additional argument.
     *
     * @return the message.
     */
    @Message(id = 6, value = "Argument expected for option %s")
    String argumentExpected(String arg);

    @Message(id = 7, value = "Missing required argument(s): %s")
    String missingRequiredArgs(Set<String> missing);

    @Message(id = 8, value = "File at path specified by argument %s does not exist")
    String fileDoesNotExist(String arg);

    @Message(id = 9, value = "File at path specified by argument %s is not a directory")
    String fileIsNotADirectory(String arg);

    @Message(id = 10, value = "File at path specified by argument %s is a directory")
    String fileIsADirectory(String arg);

    @Message(id = 11, value = "Cannot rollback patch (%s)")
    PatchingException cannotRollbackPatch(String id);

    @Message(id = 12, value = "Patch '%s' already applied")
    PatchingException alreadyApplied(String patchId);

    @Message(id = 13, value = "There is no layer called %s installed")
    PatchingException noSuchLayer(String name);

    @Message(id = 14, value = "Failed to resolve a valid patch descriptor for %s %s")
    PatchingException failedToResolvePatch(String product, String version);

    @Message(id = 15, value = "Requires patch '%s'")
    PatchingException requiresPatch(String patchId);

    @Message(id = 16, value = "Patch is incompatible with patch '%s'")
    PatchingException incompatiblePatch(String patchId);

    @Message(id = 17, value = "Conflicts detected")
    ContentConflictsException conflictsDetected(@Param Collection<ContentItem> conflicts);

    @Message(id = 18, value = "copied content does not match expected hash for item: %s")
    SyncFailedException wrongCopiedContent(ContentItem item);

    @Message(id = 19, value = "invalid patch name '%s'")
    IllegalArgumentException illegalPatchName(String name);

    @Message(id = 20, value = "Cannot rollback. No patches applied.")
    IllegalArgumentException noPatchesApplied();

    @Message(id = 21, value = "Patch '%s' not found in history.")
    PatchingException patchNotFoundInHistory(String patchId);

    @Message(id = 22, value = "Cannot complete operation. Patch '%s' is currently active")
    OperationFailedException patchActive(String patchId);

    @Message(id = 23, value = "Failed to show history of patches")
    OperationFailedException failedToShowHistory(@Cause Throwable cause);

    @Message(id = 24, value = "Unable to apply or rollback a patch when the server is in a restart-required state.")
    OperationFailedException serverRequiresRestart();

    @Message(id = 25, value = "failed to load identity info")
    String failedToLoadIdentity();

    @Message(id = 26, value = "No more patches")
    String noMorePatches();

    @Message(id = 27, value = "No patch history %s")
    String noPatchHistory(String path);

    @Message(id = 28, value = "Patch is missing file %s")
    String patchIsMissingFile(String path);

    @Message(id = 29, value = "File is not readable %s")
    String fileIsNotReadable(String path);

    @Message(id = 30, value = "Layer not found %s")
    String layerNotFound(String name);

    @LogMessage(level = ERROR)
    @Message(id = 31, value = "failed to undo change for: '%s'")
    void failedToUndoChange(String name);

    @Message(id = 32, value = "missing: '%s'")
    String missingArtifact(PatchingArtifact.ArtifactState state);

    @Message(id = 33, value = "inconsistent state: '%s'")
    String inconsistentArtifact(PatchingArtifact.ArtifactState state);

    @Message(id = 34, value = "in error: '%s'")
    String artifactInError(PatchingArtifact.ArtifactState state);
}

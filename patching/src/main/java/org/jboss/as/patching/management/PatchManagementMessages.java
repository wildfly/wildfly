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

package org.jboss.as.patching.management;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

/**
 * This module is using message IDs in the range 16900-16999.
 * <p/>
 * This file is using the subset 16940-16999 for non-logger messages.
 * <p/>
 * See <a href="http://community.jboss.org/docs/DOC-16810">http://community.jboss.org/docs/DOC-16810</a> for the full
 * list of currently reserved JBAS message id blocks.
 * <p/>
 *
 * @author Emanuel Muckenhuber
 */
@MessageBundle(projectCode = "JBAS")
public interface PatchManagementMessages {

    PatchManagementMessages MESSAGES = Messages.getBundle(PatchManagementMessages.class);

    @Message(id = 16940, value = "Cannot complete operation. Patch '%s' is currently active")
    OperationFailedException patchActive(String patchId);

    @Message(id = 16941, value = "Failed to show history of patches")
    OperationFailedException failedToShowHistory(@Cause Throwable cause);

    @Message(id = 16942, value = "Unable to apply or rollback a patch when the server is in a restart-required state.")
    OperationFailedException serverRequiresRestart();

    @Message(id = 16943, value = "failed to load identity info")
    String failedToLoadIdentity();

    @Message(id = 16944, value = "No more patches")
    String noMorePatches();

    @Message(id = 16945, value = "No patch history %s")
    String noPatchHistory(String path);

    @Message(id = 16946, value = "Patch is missing file %s")
    String patchIsMissingFile(String path);

    @Message(id = 16947, value = "File is not readable %s")
    String fileIsNotReadable(String path);

    @Message(id = 16948, value = "Layer not found %s")
    String layerNotFound(String name);
}

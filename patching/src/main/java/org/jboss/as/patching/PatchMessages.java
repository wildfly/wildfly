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
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;

import java.io.IOException;
import java.util.List;

/**
 * 16840 - 16899
 *
 * @author Emanuel Muckenhuber
 */
@MessageBundle(projectCode = "JBAS")
public interface PatchMessages {

    PatchMessages MESSAGES = Messages.getBundle(PatchMessages.class);

    @Message(id = 16840, value = "Patch does not apply - expected (%s), but was (%s)")
    PatchingException doesNotApply(List < String > appliesTo, String version);

    @Message(id = 16841, value = "Failed to delete (%s)")
    IOException failedToDelete(String path);

    @Message(id = 16842, value = "Failed to create directory (%s)")
    IOException cannotCreateDirectory(String path);

    @Message(id = 16843, value = "Cannot complete operation. Patch '%s' is currently active")
    OperationFailedException patchActive(String patchId);

}

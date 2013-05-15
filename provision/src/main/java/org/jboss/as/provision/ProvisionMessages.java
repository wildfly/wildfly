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

package org.jboss.as.provision;

import java.io.File;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

/**
 * Logging Id ranges: 20350-20399
 *
 * https://community.jboss.org/wiki/LoggingIds
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author Thomas.Diesler@jboss.com
 * @since 03-May-2013
 */
@MessageBundle(projectCode = "JBAS")
public interface ProvisionMessages {

    ProvisionMessages MESSAGES = Messages.getBundle(ProvisionMessages.class);

    @Message(id = 20350, value = "%s is null")
    IllegalArgumentException illegalArgumentNull(String name);

    @Message(id = 20351, value = "Cannot find bundles directory: %s")
    IllegalStateException illegalStateCannotFindBundleDir(File dir);

    @Message(id = 20352, value = "Illegal repository base location: %s")
    IllegalStateException illegalStateArtifactBaseLocation(File dir);

    @Message(id = 20353, value = "No layers directory found at %s")
    IllegalStateException illegalStateNoLayersDirectoryFound(File dir);

    @Message(id = 20354, value = "Cannot find layer %s under directory %s")
    IllegalStateException illegalStateCannotFindLayer(String layer, File dir);

    @Message(id = 20355, value = "Starting web context failed")
    String startContextFailed();
}

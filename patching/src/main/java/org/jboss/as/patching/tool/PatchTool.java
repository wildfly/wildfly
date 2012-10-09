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

package org.jboss.as.patching.tool;

import org.jboss.as.boot.DirectoryStructure;
import org.jboss.as.patching.PatchInfo;
import org.jboss.as.patching.PatchLogger;
import org.jboss.as.patching.runner.ContentVerificationPolicy;
import org.jboss.as.patching.runner.PatchingException;
import org.jboss.as.patching.runner.PatchingResult;
import org.jboss.as.patching.runner.PatchingTaskRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * An interactive offline patch tool.
 *
 * @author Emanuel Muckenhuber
 */
class PatchTool {

    private final PatchInfo info;
    private final DirectoryStructure structure;
    private final PatchingTaskRunner runner;

    PatchTool(PatchInfo info, DirectoryStructure structure) {
        this.info = info;
        this.structure = structure;
        this.runner = new PatchingTaskRunner(info, structure);
    }

    PatchingResult applyPatch(final File file, final ContentVerificationPolicy policy) throws PatchingException {
        try {
            final InputStream is = new FileInputStream(file);
            try {
                return runner.executeDirect(is, policy);
            } finally {
                if(is != null) try {
                    is.close();
                } catch (IOException e) {
                    PatchLogger.ROOT_LOGGER.debugf(e, "failed to close input stream");
                }
            }
        } catch (IOException e) {
            throw new PatchingException(e);
        }
    }

    PatchingResult rollback(final String patchId, boolean overrideAll) throws PatchingException {
        return runner.rollback(patchId, overrideAll);
    }

}

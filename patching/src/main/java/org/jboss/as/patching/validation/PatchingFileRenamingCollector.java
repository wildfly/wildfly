/*
 * Copyright (C) 2014 Red Hat, inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.jboss.as.patching.validation;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.jboss.as.patching.logging.PatchLogger;
import org.jboss.as.patching.runner.PatchUtils;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2013 Red Hat, inc.
 */
public class PatchingFileRenamingCollector {

    private static final PatchLogger log = PatchLogger.ROOT_LOGGER;
    private final File renamingFailureMarker;

    public PatchingFileRenamingCollector(final File renamingFailureMarker) {
        this.renamingFailureMarker = renamingFailureMarker;
    }

    public void renameFiles() throws IOException {
        List<String> failures = PatchUtils.readRefs(renamingFailureMarker);
        for(String path : failures) {
           File toBeRenamed = new File(path);
           if(toBeRenamed.exists()) {
               if(!toBeRenamed.renameTo(PatchUtils.getRenamedFileName(toBeRenamed))) {
                   log.cannotDeleteFile(path);
               }
           }
        }
        renamingFailureMarker.delete();
    }
}

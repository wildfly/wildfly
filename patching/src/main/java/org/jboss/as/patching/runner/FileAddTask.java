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

package org.jboss.as.patching.runner;

import org.jboss.as.patching.metadata.MiscContentItem;
import org.jboss.as.patching.metadata.MiscContentModification;
import org.jboss.as.patching.metadata.ModificationType;

import java.io.File;
import java.io.IOException;

/**
 * @author Emanuel Muckenhuber
 */
class FileAddTask extends AbstractFileTask {

    FileAddTask(File target, File backup, MiscContentModification modification) {
        super(target, backup, modification);
    }

    @Override
    public boolean prepare(PatchingContext context) throws IOException {
        boolean result = super.prepare(context);
        if(result) {
            // Check that there was really no content copied
            return backupHash == NO_CONTENT;
        }
        return result;
    }

    @Override
    protected MiscContentModification createRollback(PatchingContext context, MiscContentItem item, MiscContentItem backupItem, byte[] targetHash) {
        return new MiscContentModification(backupItem, targetHash, ModificationType.REMOVE);
    }

}

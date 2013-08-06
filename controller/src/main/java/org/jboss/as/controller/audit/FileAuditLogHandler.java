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
package org.jboss.as.controller.audit;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.services.path.PathManagerService;
import org.xnio.IoUtils;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class FileAuditLogHandler extends AuditLogHandler {
    private static final SimpleDateFormat OLD_FILE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final byte[] LINE_TERMINATOR = String.format("%n").getBytes();
    private final PathManagerService pathManager;
    private final String path;
    private final String relativeTo;
    private volatile File file;

    public FileAuditLogHandler(String name, String formatterName, int maxFailureCount, PathManagerService pathManager, String path, String relativeTo) {
        super(name, formatterName, maxFailureCount);
        this.pathManager = pathManager;
        this.path = path;
        this.relativeTo = relativeTo;
        this.file = file;
    }

    @Override
    void initialize() {
        if (file != null){
            return;
        }
        File file = new File(pathManager.resolveRelativePathEntry(path, relativeTo));
        if (file.exists() && file.isDirectory()) {
            throw ControllerMessages.MESSAGES.resolvedFileDoesNotExistOrIsDirectory(file);
        }
        File parent = file.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }
        if (file.exists()) {
            File backup = new File(file.getParentFile(), file.getName() + OLD_FILE_FORMATTER.format(new Date()));
            if (!file.renameTo(backup)) {
                //TODO i18n
                throw ControllerMessages.MESSAGES.couldNotBackUp(file.getAbsolutePath(), backup.getAbsolutePath());
            }
        }
        try {
            file.createNewFile();
        } catch (IOException e){
            throw new RuntimeException(e);
        }
        this.file = file;
    }

    @Override
    void stop() {
        file = null;
    }

    @Override
    void writeLogItem(String formattedItem) throws IOException {
        final OutputStream output = new BufferedOutputStream(new FileOutputStream(file, true));
        try {
            output.write(formattedItem.getBytes());
            output.write(LINE_TERMINATOR);
        } finally {
            IoUtils.safeClose(output);
        }
    }

    boolean compare(AuditLogHandler other){
        if (other instanceof FileAuditLogHandler == false){
            return false;
        }
        FileAuditLogHandler otherHandler = (FileAuditLogHandler)other;
        if (!name.equals(otherHandler.name)){
            return false;
        }
        if (!getFormatterName().equals(otherHandler.getFormatterName())) {
            return false;
        }
        if (!path.equals(otherHandler.path)){
            return false;
        }
        if (!compare(relativeTo, otherHandler.relativeTo)){
            return false;
        }
        return true;
    }

    private boolean compare(Object one, Object two){
        if (one == null && two == null){
            return true;
        }
        if (one == null && two != null){
            return false;
        }
        if (one != null && two == null){
            return false;
        }
        return one.equals(two);
    }
}

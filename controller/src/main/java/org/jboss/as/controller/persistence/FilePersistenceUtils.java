/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.controller.persistence;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.dmr.ModelNode;
import org.xnio.IoUtils;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class FilePersistenceUtils {

    static File createTempFile(File fileName) {
        return new File(fileName.getParentFile(), fileName.getName() + ".tmp");
    }

    static ExposedByteArrayOutputStream marshalXml(final AbstractConfigurationPersister persister, final ModelNode model) throws ConfigurationPersistenceException {
        ExposedByteArrayOutputStream marshalled = new ExposedByteArrayOutputStream(1024 * 8);
        try {
            try {
                BufferedOutputStream output = new BufferedOutputStream(marshalled);
                persister.marshallAsXml(model, output);
                output.close();
                marshalled.close();
            } finally {
                IoUtils.safeClose(marshalled);
            }
        } catch (Exception e) {
            throw ControllerLogger.ROOT_LOGGER.failedToMarshalConfiguration(e);
        }
        return marshalled;
    }

    static void copyFile(final File file, final File backup) throws IOException {
        final InputStream in = new BufferedInputStream(new FileInputStream(file));
        try {
            final FileOutputStream fos = new FileOutputStream(backup);
            final BufferedOutputStream output = new BufferedOutputStream(fos);
            try {
                StreamUtils.copyStream(in, output);
                output.flush();
                fos.getFD().sync();
                fos.close();
            } finally {
                StreamUtils.safeClose(output);
            }
        } finally {
            StreamUtils.safeClose(in);
        }
    }

    static void rename(File file, File to) throws IOException {
        if (!file.renameTo(to) && file.exists()) {
            copyFile(file, to);
        }
    }

    static void moveTempFileToMain(File tempFileName, File fileName) throws ConfigurationPersistenceException {
        //Rename the temp file written to the target file
        try {
            FilePersistenceUtils.rename(tempFileName, fileName);
            //Only delete the temp file if all went well, to give people the chance to manually recover it if something really weird happened
            deleteFile(tempFileName);
        } catch (Exception e) {
            throw ControllerLogger.ROOT_LOGGER.failedToRenameTempFile(e, tempFileName, fileName);
        }
    }

    static void deleteFile(File file) {
        if (file.exists()) {
            if (!file.delete() && file.exists()) {
                file.deleteOnExit();
                throw new IllegalStateException(ControllerLogger.ROOT_LOGGER.couldNotDeleteFile(file));
            }
        }
    }

    static File writeToTempFile(ExposedByteArrayOutputStream marshalled, File tempFileName) throws IOException {
        deleteFile(tempFileName);

        final FileOutputStream fos = new FileOutputStream(tempFileName);
        final InputStream is = marshalled.getInputStream();
        try {
            BufferedOutputStream output = new BufferedOutputStream(fos);
            byte[] bytes = new byte[1024];
            int read;
            while ((read = is.read(bytes)) > -1) {
                output.write(bytes, 0, read);
            }
            output.flush();
            fos.getFD().sync();
            output.close();
            is.close();
        } finally {
            IoUtils.safeClose(fos);
            IoUtils.safeClose(is);
        }
        return tempFileName;
    }
}

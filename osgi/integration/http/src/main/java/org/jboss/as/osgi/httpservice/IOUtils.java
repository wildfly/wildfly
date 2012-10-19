/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.osgi.httpservice;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Thomas.Diesler@jboss.com
 * @since 19-Jul-2012
 */
class IOUtils {

    // Hide the constructor
    private IOUtils() {
    }

    public static void copyStream(OutputStream output, InputStream input) throws IOException {
        try {
            byte[] bytes = new byte[1024];
            int r = input.read(bytes);
            while (r > 0) {
                output.write(bytes, 0, r);
                r = input.read(bytes);
            }
        } catch (IOException e) {
            throw e;
        } finally {
            input.close();
            output.close();
        }
    }

    public static boolean deleteRecursive(File file) {
        boolean result = true;
        if (file.isDirectory()) {
            for (File aux : file.listFiles())
                result &= deleteRecursive(aux);
        }
        result &= file.delete();
        return result;
    }
}
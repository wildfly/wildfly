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
package org.jboss.as.process;

import java.io.File;

import static org.jboss.as.process.ProcessMessages.MESSAGES;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class DefaultJvmUtils {

    /**
     * Get the home dir of the currently executing jvm
     *
     * @return The home dir
     */
    public static String getCurrentJvmHome() {
        return System.getProperty("java.home");
    }


    /**
     * Find the java executable under a given java home dir
     *
     * @param javaHome The java home dir
     * @return The java executable
     * @throws IllegalStateException if the java executable could not be found
     */
    public static String findJavaExecutable(String javaHome) {
        File file = new File(javaHome);
        if (!file.exists()) {
            throw MESSAGES.invalidJavaHome(file.getAbsolutePath());
        }
        file = new File(file, "bin");
        if (!file.exists()) {
            throw MESSAGES.invalidJavaHomeBin(file.getAbsolutePath(), file.getParentFile().getAbsolutePath());
        }
        File java = new File(file, "java");
        if (!java.exists()) {
            java = new File(file, "java.exe");
        }
        if (!java.exists()) {
            throw MESSAGES.cannotFindJavaExe(file.getAbsolutePath());
        }
        return java.getAbsolutePath();
    }

}

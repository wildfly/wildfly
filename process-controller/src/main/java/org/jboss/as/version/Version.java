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

package org.jboss.as.version;

import java.io.InputStream;
import java.util.jar.Manifest;

/**
 * Common location to manager the AS version.
 *
 * @author John Bailey
 * @author Jason T. Greene
 */
public class Version {
    public static final String AS_VERSION;
    public static final String AS_RELEASE_CODENAME;
    public static final int MANAGEMENT_MAJOR_VERSION = 1;
    public static final int MANAGEMENT_MINOR_VERSION = 0;

    static {
        InputStream stream = Version.class.getClassLoader().getResourceAsStream("META-INF/MANIFEST.MF");
        Manifest manifest = null;
        try {
            if (stream != null)
                manifest = new Manifest(stream);
        } catch (Exception e) {
        }

        String version = "Unknown", code = version;
        if (manifest != null) {
            version = manifest.getMainAttributes().getValue("JBossAS-Release-Version");
            code = manifest.getMainAttributes().getValue("JBossAS-Release-Codename");
        }

        AS_VERSION = version;
        AS_RELEASE_CODENAME = code;
    }

}

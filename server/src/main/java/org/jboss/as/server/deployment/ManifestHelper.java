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

package org.jboss.as.server.deployment;

import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Manifest helper methods
 *
 * @author Thomas.Diesler@jboss.com
 * @since 15-Nov-2012
 */
public final class ManifestHelper {

    // Hide ctor
    private ManifestHelper() {
    }

    public static boolean hasMainAttributeValue(Manifest manifest, String attribute) {
        return getMainAttributeValue(manifest, attribute) != null;
    }

    public static String getMainAttributeValue(Manifest manifest, String attribute) {
        String result = null;
        if (manifest != null && attribute != null) {
            Attributes attributes = manifest.getMainAttributes();
            result = attributes.getValue(attribute);
        }
        return result;
    }
}

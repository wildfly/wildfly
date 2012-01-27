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

package org.jboss.as.domain.management;

/**
 * Constants specific to the Domain Management module.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ModelDescriptionConstants {

    public static final String IDENTITY = "identity";
    public static final String KEYSTORE_PASSWORD = "keystore-password";
    public static final String KEYSTORE_PATH = "keystore-path";
    public static final String KEYSTORE_RELATIVE_TO = "keystore-relative-to";
    public static final String PASSWORD = "password";
    public static final String PATH = "path";
    public static final String REALM = "realm";
    public static final String RELATIVE_TO = "relative-to";
    public static final String ROLES = "roles";
    public static final String USERNAME = "username";
    public static final String VERBOSE = "verbose";
    public static final String WHOAMI = "whoami";

    // Prevent instantiation.
    private ModelDescriptionConstants() {}
}

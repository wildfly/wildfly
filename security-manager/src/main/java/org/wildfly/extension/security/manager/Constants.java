/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.security.manager;

/**
 * Constants used throughout the security manager subsystem.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class Constants {

    public static final String SUBSYSTEM_NAME = "security-manager";
    public static final String SECURITY_MANAGER_SERVICE = "security-manager-service";
    public static final String DEPLOYMENT_PERMISSIONS = "deployment-permissions";
    public static final String MINIMUM_SET = "minimum-set";
    public static final String MAXIMUM_SET = "maximum-set";
    public static final String MAXIMUM_PERMISSIONS = "maximum-permissions";
    public static final String MINIMUM_PERMISSIONS = "minimum-permissions";
    public static final String PERMISSION = "permission";
    public static final String PERMISSION_CLASS = "class";
    public static final String PERMISSION_NAME = "name";
    public static final String PERMISSION_ACTIONS = "actions";
    public static final String PERMISSION_MODULE = "module";
    public static final String DEFAULT_VALUE = "default";

}

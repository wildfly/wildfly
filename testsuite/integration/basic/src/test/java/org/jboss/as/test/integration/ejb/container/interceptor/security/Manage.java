/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ejb.container.interceptor.security;

/**
 * @author Josef Cacek
 */
public interface Manage {

    String ROLE_ROLE1 = "Role1";
    String ROLE_ROLE2 = "Role2";
    String ROLE_USERS = "Users";
    String ROLE_GUEST = "guest";

    /** All test roles */
    String[] ROLES_ALL = { ROLE_ROLE1, ROLE_ROLE2, ROLE_GUEST, ROLE_USERS };

    String BEAN_NAME_TARGET = "TargetBean";
    String BEAN_NAME_BRIDGE = "BridgeBean";

    /** Default result of methods defined in this interface. */
    String RESULT = "OK";

    String role1();

    String role2();

    String allRoles();

}

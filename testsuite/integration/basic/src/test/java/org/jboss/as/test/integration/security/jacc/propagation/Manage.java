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
package org.jboss.as.test.integration.security.jacc.propagation;

/**
 * Interface used for testing authorization propagation in the JACC policy module.
 *
 * @author Josef Cacek
 */
public interface Manage {

    String TEST_NAME = "jacc-propagation-test";

    String ROLE_ADMIN = "Admin";
    String ROLE_MANAGER = "Manager";
    String ROLE_USER = "User";

    /**
     * All test roles
     */
    String[] ROLES_ALL = {ROLE_ADMIN, ROLE_MANAGER, ROLE_USER};

    String BEAN_NAME_TARGET = "TargetBean";
    String BEAN_NAME_BRIDGE = "BridgeBean";

    /**
     * Default result of methods defined in this interface.
     */
    String RESULT = "OK";

    String admin();

    String manage();

    String work();

}

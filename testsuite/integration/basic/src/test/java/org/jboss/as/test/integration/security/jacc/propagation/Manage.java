/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

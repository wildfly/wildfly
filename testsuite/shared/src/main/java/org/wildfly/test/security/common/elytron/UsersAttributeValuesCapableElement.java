/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.security.common.elytron;

import java.util.List;

/**
 * This interface represent configuration element with predefined list of users and their attribute values. It provides ability to tests
 * to come up with own user population for the tested scenario.
 *
 * @author Josef Cacek
 */
public interface UsersAttributeValuesCapableElement extends ConfigurableElement {

    /**
     * Returns predefined (not {@code null}) list of users and their attributes to be created.
     */
    List<UserWithAttributeValues> getUsersWithAttributeValues();
}

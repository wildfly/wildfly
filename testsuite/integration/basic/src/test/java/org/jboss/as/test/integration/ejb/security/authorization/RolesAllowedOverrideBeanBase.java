/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security.authorization;

import jakarta.annotation.security.RolesAllowed;

/**
 * @author wangchao
 *
 */

@RolesAllowed("Admin")
public class RolesAllowedOverrideBeanBase {

    public String aMethod(final String message) {
        return message;
    }

    public String bMethod(final String message) {
        return message;
    }

}

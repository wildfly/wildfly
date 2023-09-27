/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security;

import jakarta.annotation.security.DenyAll;

/**
 * User: jpai
 */
public class Base implements FullAccess {

    @Override
    public void doAnything() {

    }

    @DenyAll
    public void restrictedBaseClassMethod() {

    }

    @DenyAll
    public void overriddenMethod() {

    }
}

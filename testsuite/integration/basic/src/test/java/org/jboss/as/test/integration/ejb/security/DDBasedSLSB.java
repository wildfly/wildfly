/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security;

import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * User: jpai
 */
@SecurityDomain("other")
public class DDBasedSLSB implements FullAccess {

    public void accessDenied() {

    }

    public void onlyTestRoleCanAccess() {

    }

    @Override
    public void doAnything() {

    }
}

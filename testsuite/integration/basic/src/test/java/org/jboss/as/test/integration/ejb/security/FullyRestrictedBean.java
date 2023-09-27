/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security;

import jakarta.annotation.security.DenyAll;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Singleton;

import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * User: jpai
 */
@Singleton
@DenyAll
@LocalBean
@SecurityDomain("other")
public class FullyRestrictedBean extends AnnotatedSLSB {

    @Override
    public void overriddenMethod() {
        // the @DenyAll on the class level of this bean should have been applied
        // and the invocation to this method shouldn't have been allowed
        throw new RuntimeException("Access to this method shouldn't have been allowed!");
    }
}

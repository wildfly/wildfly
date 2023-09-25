/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.security.jbossappxml;

/**
 * @author anil saldhana
 */
public interface BeanInterface {
    String getCallerPrincipal();
    boolean isCallerInRole(String roleName);
}
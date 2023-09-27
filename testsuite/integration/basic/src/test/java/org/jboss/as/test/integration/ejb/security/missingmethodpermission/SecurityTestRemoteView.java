/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security.missingmethodpermission;

/**
 * @author Jaikiran Pai
 */
public interface SecurityTestRemoteView {

    String methodWithSpecificRole();

    String methodWithNoRole();

}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.remote.contextdata;

import jakarta.ejb.Remote;

@Remote
public interface TestRemote {

    UseCaseValidator invoke(UseCaseValidator useCaseValidator) throws TestException;

}

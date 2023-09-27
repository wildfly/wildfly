/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.manualmode.ejb.interceptor.serverside;

import jakarta.ejb.Remote;

@Remote
public interface SubstituteSampleBeanRemote {
    String getSimpleName();
}

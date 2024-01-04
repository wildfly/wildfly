/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.invocationtimeout;

import jakarta.ejb.Remote;

/**
 * @author Jan Martiska
 */
@Remote
public interface LongRunningBeanRemote {

    void longRunningOperation();

}

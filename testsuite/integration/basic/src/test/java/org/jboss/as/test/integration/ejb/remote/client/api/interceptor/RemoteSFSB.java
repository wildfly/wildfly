/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.client.api.interceptor;

import java.util.Map;

/**
 * @author Jaikiran Pai
 */
public interface RemoteSFSB {

    Map<String, Object> getInvocationData(final String... key);
}

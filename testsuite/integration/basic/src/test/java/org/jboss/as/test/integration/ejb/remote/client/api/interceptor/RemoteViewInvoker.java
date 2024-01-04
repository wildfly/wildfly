/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.client.api.interceptor;

import java.util.Map;

/**
 * @author Jaikiran Pai
 */
public interface RemoteViewInvoker {

    Map<String, Object> invokeRemoteViewAndGetInvocationData(final String... key);

    Map<String, Object> getDataSetupForInvocationContext();
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.provider.bean;

import java.util.Collection;

public interface ServiceProviderRetriever {

    Collection<String> getProviders();
}

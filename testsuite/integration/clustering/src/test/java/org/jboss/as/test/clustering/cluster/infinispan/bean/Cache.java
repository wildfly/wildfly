/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.infinispan.bean;

/**
 * @author Paul Ferraro
 */
public interface Cache {
    String get(String key);
    String put(String key, String value);
    String remove(String key);
}

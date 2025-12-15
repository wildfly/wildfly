/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.bean;

import org.wildfly.clustering.server.expiration.ExpirationMetaData;

/**
 * Describes the immutable metadata of a bean.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 */
public interface ImmutableBeanMetaData<K> extends ExpirationMetaData {
    /**
     * Returns the component name of this bean.
     * @return the component name of this bean.
     */
    String getName();

    /**
     * Returns the identifier of the group to which this bean is associated.
     * @return a group identifier.
     */
    K getGroupId();
}

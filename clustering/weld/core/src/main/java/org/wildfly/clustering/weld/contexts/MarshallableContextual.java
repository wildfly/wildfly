/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.contexts;

import org.jboss.weld.serialization.spi.BeanIdentifier;

/**
 * @author Paul Ferraro
 */
public interface MarshallableContextual<C> {

    BeanIdentifier getIdentifier();

    C getInstance();
}

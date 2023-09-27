/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.marshalling.jboss;

import java.util.List;

/**
 * Contributes known classes to a {@link org.jboss.marshalling.ClassTable}.
 * @author Paul Ferraro
 */
public interface ClassTableContributor {
    List<Class<?>> getKnownClasses();
}

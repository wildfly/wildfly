/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.naming;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.context.Contextualizer;
import org.wildfly.clustering.context.ContextualizerFactory;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(ContextualizerFactory.class)
public class NamespaceContextualizerFactory implements ContextualizerFactory {

    @Override
    public Contextualizer createContextualizer(ClassLoader loader) {
        return new NamespaceContextExecutor();
    }
}

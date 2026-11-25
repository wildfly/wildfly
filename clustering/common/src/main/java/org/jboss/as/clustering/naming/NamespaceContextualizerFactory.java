/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.naming;

import java.util.function.Supplier;

import org.jboss.as.naming.context.NamespaceContextSelector;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.context.Context;
import org.wildfly.clustering.context.Contextualizer;
import org.wildfly.clustering.context.ContextualizerFactory;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(ContextualizerFactory.class)
public class NamespaceContextualizerFactory implements ContextualizerFactory {

    @Override
    public Contextualizer createContextualizer(ClassLoader loader) {
        NamespaceContextSelector selector = NamespaceContextSelector.getCurrentSelector();
        return (selector != null) ? Contextualizer.withContextProvider(new Supplier<Context<NamespaceContextSelector>>() {
            @Override
            public Context<NamespaceContextSelector> get() {
                NamespaceContextSelector.pushCurrentSelector(selector);
                return Context.of(selector, NamespaceContextSelector::popCurrentSelector);
            }
        }) : Contextualizer.NONE;
    }
}

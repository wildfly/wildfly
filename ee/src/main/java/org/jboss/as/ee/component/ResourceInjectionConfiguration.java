/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.component;

/**
 * A configuration for resource injection.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ResourceInjectionConfiguration {
    private final InjectionTarget target;
    private final InjectionSource source;
    private final boolean optional;

    /**
     * Construct a new instance.
     *
     * @param target the resource injection target
     * @param source the resource injection source
     * @param optional if the injection is optional or not
     */
    public ResourceInjectionConfiguration(final InjectionTarget target, final InjectionSource source, boolean optional) {
        this.target = target;
        this.source = source;
        this.optional = optional;
    }

    /**
     * Construct a new instance.
     *
     * @param target the resource injection target
     * @param source the resource injection source
     */
    public ResourceInjectionConfiguration(final InjectionTarget target, final InjectionSource source) {
        this(target, source, false);
    }

    /**
     * Get the resource injection for this configuration.
     *
     * @return the resource injection
     */
    public InjectionTarget getTarget() {
        return target;
    }

    /**
     * Get the injection source for this configuration.
     *
     * @return the injection source
     */
    public InjectionSource getSource() {
        return source;
    }

    /**
     *
     * @return True if the injection is optional
     */
    public boolean isOptional() {
        return optional;
    }
}

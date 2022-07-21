/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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

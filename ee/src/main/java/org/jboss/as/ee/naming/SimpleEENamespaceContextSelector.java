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

package org.jboss.as.ee.naming;

import org.jboss.as.naming.context.NamespaceContextSelector;

import javax.naming.Context;

/**
 * A simple EE-style namespace context selector.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class SimpleEENamespaceContextSelector extends NamespaceContextSelector {
    private final Context appContext;
    private final Context moduleContext;
    private final Context compContext;

    /**
     * Construct a new instance.
     *
     * @param appContext the app context to use, or {@code null} for none
     * @param moduleContext the module context to use, or {@code null} for none
     * @param compContext the comp context to use, or {@code null} for none
     */
    public SimpleEENamespaceContextSelector(final Context appContext, final Context moduleContext, final Context compContext) {
        this.appContext = appContext;
        this.moduleContext = moduleContext;
        this.compContext = compContext;
    }

    public Context getContext(final String identifier) {
        if (identifier.equals("app")) {
            return appContext;
        } else if (identifier.equals("module")) {
            return moduleContext;
        } else if (identifier.equals("comp")) {
            return compContext;
        } else {
            return null;
        }
    }
}

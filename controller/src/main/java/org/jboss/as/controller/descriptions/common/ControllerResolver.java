/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.descriptions.common;

import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public final class ControllerResolver {
    static final String RESOURCE_NAME = ControllerResolver.class.getPackage().getName() + ".LocalDescriptions";


    public static ResourceDescriptionResolver getResolver(final String... keyPrefix) {
        return getResolver(false, keyPrefix);
    }

    public static ResourceDescriptionResolver getResolver(boolean useUnprefixedChildTypes, final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder();
        for (String kp : keyPrefix) {
            if (prefix.length() > 0) {
                prefix.append('.').append(kp);
            } else {
                prefix.append(kp);
            }
        }

        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, ControllerResolver.class.getClassLoader(), true, useUnprefixedChildTypes);
    }
}

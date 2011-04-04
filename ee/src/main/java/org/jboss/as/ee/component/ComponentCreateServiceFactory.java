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
 * A factory for component creation which allows component behavior customization.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ComponentCreateServiceFactory {

    /**
     * Construct a new component creation service from the given configuration.
     *
     * @param configuration the configuration
     * @return the create service
     */
    BasicComponentCreateService constructService(ComponentConfiguration configuration);

    /**
     * The default, basic component create service factory.
     */
    ComponentCreateServiceFactory BASIC = new ComponentCreateServiceFactory() {
        public BasicComponentCreateService constructService(final ComponentConfiguration configuration) {
            return new BasicComponentCreateService(configuration);
        }
    };
}

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

package org.jboss.as.ejb3.component.singleton;

import org.jboss.as.ee.component.BasicComponent;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ejb3.component.EJBComponentCreateService;

/**
 * @author Stuart Douglas
 */
public class SingletonComponentCreateService extends EJBComponentCreateService {

    private final boolean initOnStartup;

    /**
     * Construct a new instance.
     *
     * @param componentConfiguration the component configuration
     */
    public SingletonComponentCreateService(final ComponentConfiguration componentConfiguration) {
        this(componentConfiguration, false);
    }

    public SingletonComponentCreateService(final ComponentConfiguration componentConfiguration, final boolean initOnStartup) {
        super(componentConfiguration);
        this.initOnStartup = initOnStartup;
    }

    @Override
    protected BasicComponent createComponent() {
        return new SingletonComponent(this);
    }

    public boolean isInitOnStartup() {
        return this.initOnStartup;
    }
}

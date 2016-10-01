/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
 * 2110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.ejb3.subsystem;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 *
 *
 * @author Stuart Douglas
 */
public class EjbNameRegexService implements Service<EjbNameRegexService> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "ejbNameRegex");

    private volatile boolean ejbNameRegexAllowed;

    public EjbNameRegexService(final boolean defaultDistinctName) {
        this.ejbNameRegexAllowed = defaultDistinctName;
    }

    @Override
    public void start(final StartContext context) throws StartException {

    }

    @Override
    public void stop(final StopContext context) {

    }

    @Override
    public EjbNameRegexService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public boolean isEjbNameRegexAllowed() {
        return ejbNameRegexAllowed;
    }

    public void setEjbNameRegexAllowed(final boolean ejbNameRegexAllowed) {
        this.ejbNameRegexAllowed = ejbNameRegexAllowed;
    }
}

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.naming.service;

import org.jboss.as.naming.context.external.ExternalContexts;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * A binder service for external contexts.
 * @author Eduardo Martins
 */
public class ExternalContextBinderService extends BinderService {

    private final InjectedValue<ExternalContexts> externalContextsInjectedValue;

    /**
     *
     * @param name
     * @param source
     */
    public ExternalContextBinderService(String name, Object source) {
        super(name, source);
        this.externalContextsInjectedValue = new InjectedValue<>();
    }

    public InjectedValue<ExternalContexts> getExternalContextsInjector() {
        return externalContextsInjectedValue;
    }

    @Override
    public synchronized void start(StartContext context) throws StartException {
        super.start(context);
        externalContextsInjectedValue.getValue().addExternalContext(context.getController().getName());
    }

    @Override
    public synchronized void stop(StopContext context) {
        super.stop(context);
        externalContextsInjectedValue.getValue().removeExternalContext(context.getController().getName());
    }

}


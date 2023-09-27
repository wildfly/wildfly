/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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


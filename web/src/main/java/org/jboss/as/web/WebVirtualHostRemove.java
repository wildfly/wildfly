/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.web;

import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.msc.service.ServiceController;

/**
 * @author Emanuel Muckenhuber
 */
public class WebVirtualHostRemove extends AbstractWebSubsystemUpdate<Void> {

    private static final long serialVersionUID = 8956517548114021696L;
    private final String name;

    public WebVirtualHostRemove(String name) {
        this.name = name;
    }

    /** {@inheritDoc} */
    protected void applyUpdate(WebSubsystemElement element) throws UpdateFailedException {
        if(! element.removeHost(name)) {
            throw new UpdateFailedException("no such hosts " + name);
        }
    }

    /** {@inheritDoc} */
    protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        final ServiceController<?> service = updateContext.getServiceRegistry().getService(WebSubsystemElement.JBOSS_WEB_HOST.append(name));
        if(service == null) {
            resultHandler.handleSuccess(null, param);
        } else {
            service.addListener(new UpdateResultHandler.ServiceRemoveListener<P>(resultHandler, param));
        }
    }

    /** {@inheritDoc} */
    public AbstractSubsystemUpdate<WebSubsystemElement, ?> getCompensatingUpdate(WebSubsystemElement original) {
        final WebVirtualHostElement host = original.getHost(name);
        if(host == null) {
            return null;
        }
        final WebVirtualHostAdd action = new WebVirtualHostAdd(name);
        action.setAliases(host.getAliases());
        return action;
    }

}

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

package org.jboss.as.security;

import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.as.security.service.JaasConfigurationService;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;

/**
 * Update to remove JAAS configuration
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public final class RemoveJaasUpdate extends AbstractSecuritySubsystemUpdate<Void> {

    private static final long serialVersionUID = -1169438216018092782L;

    /** {@inheritDoc} */
    protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        // remove jaas configuration service
        final ServiceController<?> jaasConfigurationService = updateContext.getServiceRegistry().getService(
                JaasConfigurationService.SERVICE_NAME);
        if (jaasConfigurationService != null) {
            jaasConfigurationService.setMode(Mode.REMOVE);
        }
    }

    /** {@inheritDoc} */
    public AbstractSubsystemUpdate<SecuritySubsystemElement, ?> getCompensatingUpdate(SecuritySubsystemElement original) {
        JaasElement element = original.getJaas();
        if (element != null)
            return element.asUpdate();
        return null;
    }

    /** {@inheritDoc} */
    protected void applyUpdate(SecuritySubsystemElement element) throws UpdateFailedException {
        element.setJaas(null);
    }

}

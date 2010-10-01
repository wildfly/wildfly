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

package org.jboss.as.messaging;

import org.hornetq.api.core.Pair;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.settings.impl.AddressSettings;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;

/**
 * Update removing {@code AddressSettings}.
 *
 * @author Emanuel Muckenhuber
 */
public class AddressSettingsRemoveUpdate extends AbstractMessagingSubsystemUpdate<Void> {

    private static final long serialVersionUID = -6207374666732723259L;

    private final String addressSettingsName;

    public AddressSettingsRemoveUpdate(String addressSettingsName) {
        super();
        this.addressSettingsName = addressSettingsName;
    }

    /** {@inheritDoc} */
    AbstractSubsystemUpdate<MessagingSubsystemElement, ?> getCompensatingUpdate(Configuration original) {
        final AddressSettings settings = original.getAddressesSettings().get(addressSettingsName);
        return new AddressSettingAddUpdate(new Pair<String, AddressSettings>(addressSettingsName, settings));
    }

    /** {@inheritDoc} */
    void applyUpdate(Configuration configuration) throws UpdateFailedException {
        final AddressSettings settings = configuration.getAddressesSettings().remove(addressSettingsName);
        if(settings == null) {
            throw new IllegalStateException(String.format("setting (%s) not found", addressSettingsName));
        }
    }

    /** {@inheritDoc} */
    protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        // TODO Auto-generated method stub

    }

}

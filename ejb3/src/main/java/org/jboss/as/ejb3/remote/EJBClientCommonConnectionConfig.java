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

package org.jboss.as.ejb3.remote;

import java.util.Properties;

import javax.security.auth.callback.CallbackHandler;

import org.jboss.as.domain.management.CallbackHandlerFactory;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.ejb.client.EJBClientConfiguration;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.xnio.Option;
import org.xnio.OptionMap;


/**
 * @author Jaikiran Pai
 */
class EJBClientCommonConnectionConfig implements EJBClientConfiguration.CommonConnectionCreationConfiguration {

    private OptionMap connectionCreationOptions = OptionMap.EMPTY;
    private OptionMap channelCreationOptions = OptionMap.EMPTY;
    private long connectionTimeout = 5000;
    private CallbackHandlerProvider callbackHandlerProvider;

    @Override
    public OptionMap getConnectionCreationOptions() {
        return this.connectionCreationOptions;
    }

    @Override
    public CallbackHandler getCallbackHandler() {
        return callbackHandlerProvider == null ? new AnonymousCallbackHandler() : callbackHandlerProvider.getCallbackHandler();
    }

    @Override
    public long getConnectionTimeout() {
        return this.connectionTimeout;
    }

    @Override
    public OptionMap getChannelCreationOptions() {
        return this.channelCreationOptions;
    }

    @Override
    public boolean isConnectEagerly() {
        return true;
    }

    protected void setChannelCreationOptions(final OptionMap channelCreationOptions) {
        this.channelCreationOptions = channelCreationOptions;
    }

    protected void setConnectionCreationOptions(final OptionMap connectionCreationOptions) {
        this.connectionCreationOptions = connectionCreationOptions;
    }

    protected void setConnectionTimeout(final long timeout) {
        this.connectionTimeout = timeout;
    }

    protected void setCallbackHandler(final ServiceRegistry serviceRegistry, final String username, final String securityRealmName) {
        this.callbackHandlerProvider = new CallbackHandlerProvider(serviceRegistry, username, securityRealmName);
    }

    protected static OptionMap getOptionMapFromProperties(final Properties properties, final ClassLoader classLoader) {
        final OptionMap.Builder optionMapBuilder = OptionMap.builder();
        for (final String propertyName : properties.stringPropertyNames()) {
            try {
                final Option<?> option = Option.fromString(propertyName, classLoader);
                optionMapBuilder.parse(option, properties.getProperty(propertyName), classLoader);
            } catch (IllegalArgumentException e) {
                EjbLogger.ROOT_LOGGER.failedToCreateOptionForProperty(propertyName, e.getMessage());
            }
        }
        return optionMapBuilder.getMap();
    }


    private class CallbackHandlerProvider {

        private final ServiceRegistry serviceRegistry;
        private final String userName;
        private final String securityRealmName;

        CallbackHandlerProvider(final ServiceRegistry serviceRegistry, final String userName, final String securityRealm) {
            this.serviceRegistry = serviceRegistry;
            this.userName = userName;
            this.securityRealmName = securityRealm;
        }

        CallbackHandler getCallbackHandler() {
            if (this.securityRealmName == null || this.securityRealmName.trim().isEmpty()) {
                return new AnonymousCallbackHandler();
            }
            final ServiceName securityRealmServiceName = SecurityRealm.ServiceUtil.createServiceName(this.securityRealmName);
            final ServiceController<SecurityRealm> securityRealmController = (ServiceController<SecurityRealm>) this.serviceRegistry.getService(securityRealmServiceName);
            if (securityRealmController == null) {
                return new AnonymousCallbackHandler();
            }
            final SecurityRealm securityRealm = securityRealmController.getValue();
            final CallbackHandlerFactory cbhFactory;
            if (securityRealm != null && (cbhFactory = securityRealm.getSecretCallbackHandlerFactory()) != null && this.userName != null) {
                return cbhFactory.getCallbackHandler(this.userName);
            } else {
                return new AnonymousCallbackHandler();
            }
        }
    }

}

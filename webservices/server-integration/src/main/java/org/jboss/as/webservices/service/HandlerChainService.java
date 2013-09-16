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
package org.jboss.as.webservices.service;

import static org.jboss.as.webservices.WSMessages.MESSAGES;

import java.util.LinkedList;
import java.util.List;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.wsf.spi.metadata.config.AbstractCommonConfig;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerChainMetaData;

/**
 * A service for setting a handler chain into an endpoint / client config.
 *
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
public final class HandlerChainService<T extends AbstractCommonConfig> implements Service<UnifiedHandlerChainMetaData> {

    private InjectedValue<T> abstractCommonConfig = new InjectedValue<T>();
    private final String handlerChainType;
    private final String handlerChainId;
    private final String protocolBindings;
    private volatile UnifiedHandlerChainMetaData handlerChain;

    public HandlerChainService(String handlerChainType, String handlerChainId, String protocolBindings) {
        this.handlerChainType = handlerChainType;
        this.handlerChainId = handlerChainId;
        this.protocolBindings = protocolBindings;
    }

    @Override
    public UnifiedHandlerChainMetaData getValue() {
        return handlerChain;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        final AbstractCommonConfig commonConfig = abstractCommonConfig.getValue();
        List<UnifiedHandlerChainMetaData> handlerChains;
        if ("pre-handler-chain".equals(handlerChainType)) {
            synchronized (commonConfig) {
                handlerChains = commonConfig.getPreHandlerChains();
                if (handlerChains == null) {
                    handlerChains = new LinkedList<UnifiedHandlerChainMetaData>();
                    commonConfig.setPreHandlerChains(handlerChains);
                }
            }
        } else if ("post-handler-chain".equals(handlerChainType)) {
            synchronized (commonConfig) {
                handlerChains = commonConfig.getPostHandlerChains();
                if (handlerChains == null) {
                    handlerChains = new LinkedList<UnifiedHandlerChainMetaData>();
                    commonConfig.setPostHandlerChains(handlerChains);
                }
            }
        } else {
            throw new StartException(
                    MESSAGES.wrongHandlerChainType(handlerChainType, "pre-handler-chain", "post-handler-chain"));
        }
        handlerChain = getChain(handlerChains, handlerChainId);
        if (handlerChain != null) {
            throw new StartException(MESSAGES.multipleHandlerChainsWithSameId(handlerChainType, handlerChainId,
                    commonConfig.getConfigName()));
        }
        handlerChain = new UnifiedHandlerChainMetaData();
        handlerChain.setId(handlerChainId);
        handlerChain.setProtocolBindings(protocolBindings);
        handlerChains.add(handlerChain);
    }

    private static UnifiedHandlerChainMetaData getChain(final List<UnifiedHandlerChainMetaData> handlerChains,
            final String handlerChainId) {
        for (final UnifiedHandlerChainMetaData handlerChain : handlerChains) {
            if (handlerChainId.equals(handlerChain.getId())) {
                return handlerChain;
            }
        }
        return null;
    }

    @Override
    public void stop(final StopContext context) {
        final AbstractCommonConfig commonConfig = abstractCommonConfig.getValue();

        final List<UnifiedHandlerChainMetaData> handlerChains;
        if ("pre-handler-chain".equals(handlerChainType)) {
            handlerChains = commonConfig.getPreHandlerChains();
        } else {
            handlerChains = commonConfig.getPostHandlerChains();
        }
        handlerChains.remove(handlerChain);
    }

    public InjectedValue<T> getAbstractCommonConfig() {
        return abstractCommonConfig;
    }
}

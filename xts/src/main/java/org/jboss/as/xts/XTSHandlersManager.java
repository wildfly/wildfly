package org.jboss.as.xts;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jboss.wsf.spi.management.ServerConfig;
import org.jboss.wsf.spi.metadata.config.CommonConfig;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerChainMetaData;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerMetaData;

/**
 * Class responsible for registering WSTX and JTAOverWSAT handlers.
 *
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 *
 */
public final class XTSHandlersManager {

    private static final String CLIENT_CONFIG_TYPE = "client-config";

    private static final String CLIENT_CONFIG_NAME = "Standard-Client-Config";

    private static final String HANDLER_CHAIN_ID = "xts-handler-chain";

    private static final String HANDLER_PROTOCOL_BINDINGS = "##SOAP11_HTTP ##SOAP12_HTTP";

    private static final String WSAT_HANDLER_NAME = "JaxWSHeaderContextProcessor";

    /**
     * WS-AT handler used when default context propagation is enabled.
     */
    private static final String WSAT_ENABLED_HANDLER_CLASS = "com.arjuna.mw.wst11.client.EnabledWSTXHandler";

    /**
     * WS-AT handler used when default context propagation is disabled.
     */
    private static final String WSAT_DISABLED_HANDLER_CLASS = "com.arjuna.mw.wst11.client.DisabledWSTXHandler";

    private static final String BRIDGE_HANDLER_NAME = "JaxWSTxOutboundBridgeHandler";

    /**
     * JTAOverWSAT handler used when default context propagation is enabled.
     */
    private static final String BRIDGE_ENABLED_HANDLER_CLASS = "org.jboss.jbossts.txbridge.outbound.EnabledJTAOverWSATHandler";

    /**
     * JTAOverWSAT handler used when default context propagation is disabled.
     */
    private static final String BRIDGE_DISABLED_HANDLER_CLASS = "org.jboss.jbossts.txbridge.outbound.DisabledJTAOverWSATHandler";

    private final ServerConfig serverConfig;


    public XTSHandlersManager(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    /**
     * Registers either enabled or disabled handlers based on <code>isEnabled</code>
     *
     * @param isEnabled
     */
    public void registerClientHandlers(final boolean isEnabled) {
        if (isEnabled) {
            registerEnabledClientHandlers();
        } else {
            registerDisabledClientHandlers();
        }
    }

    /**
     * Registers enabled handlers.
     */
    private void registerEnabledClientHandlers() {
        registerClientHandler(BRIDGE_ENABLED_HANDLER_CLASS, BRIDGE_HANDLER_NAME);
        registerClientHandler(WSAT_ENABLED_HANDLER_CLASS, WSAT_HANDLER_NAME);
    }

    /**
     * Registers disabled handlers.
     */
    private void registerDisabledClientHandlers() {
        registerClientHandler(BRIDGE_DISABLED_HANDLER_CLASS, BRIDGE_HANDLER_NAME);
        registerClientHandler(WSAT_DISABLED_HANDLER_CLASS, WSAT_HANDLER_NAME);
    }

    /**
     * Registers handler.
     *
     * @param handlerClass Class name of the handler to be registered.
     * @param handlerName Handler's name to be placed in handlers chain.
     */
    private void registerClientHandler(final String handlerClass, final String handlerName) {
        final UnifiedHandlerChainMetaData handlerChain = getHandlerChain(HANDLER_CHAIN_ID,
                HANDLER_PROTOCOL_BINDINGS, CLIENT_CONFIG_TYPE, CLIENT_CONFIG_NAME);
        final UnifiedHandlerMetaData handler = new UnifiedHandlerMetaData();

        handler.setHandlerName(handlerName);
        handler.setHandlerClass(handlerClass);
        handlerChain.addHandler(handler);
    }

    /**
     *
     * @param handlerChainId
     * @param protocolBindings
     * @param configType
     * @param configName
     * @return
     */
    private UnifiedHandlerChainMetaData getHandlerChain(final String handlerChainId,
            final String protocolBindings, final String configType, final String configName) {

        final CommonConfig commonConfig = getCommonConfig(configType, configName);

        List<UnifiedHandlerChainMetaData> handlerChains = commonConfig.getPostHandlerChains();
        if (handlerChains == null) {
            handlerChains = new LinkedList<UnifiedHandlerChainMetaData>();
            commonConfig.setPostHandlerChains(handlerChains);
        }

        UnifiedHandlerChainMetaData handlerChain = getChain(handlerChains, handlerChainId);
        if (handlerChain == null) {
            handlerChain = new UnifiedHandlerChainMetaData();
            handlerChain.setId(handlerChainId);
            handlerChain.setProtocolBindings(protocolBindings);
            handlerChains.add(handlerChain);
        }

        return handlerChain;
    }

    /**
     *
     * @param handlerChains
     * @param handlerChainId
     * @return
     */
    private UnifiedHandlerChainMetaData getChain(final List<UnifiedHandlerChainMetaData> handlerChains,
            final String handlerChainId) {

        for (final UnifiedHandlerChainMetaData handlerChain : handlerChains) {
            if (handlerChainId.equals(handlerChain.getId())) {
                return handlerChain;
            }
        }

        return null;
    }

    /**
     *
     * @param configType
     * @param configName
     * @return
     * @throws IllegalStateException
     */
    private CommonConfig getCommonConfig(final String configType, final String configName) {
        final Collection<? extends CommonConfig> commonConfigs = getCommonConfigs(configType);

        for (CommonConfig commonConfig : commonConfigs) {
            if (configName.equals(commonConfig.getConfigName())) {
                return commonConfig;
            }
        }

        throw XtsAsMessages.MESSAGES.commonConfigurationUnavailable();
    }

    /**
     *
     * @param configType
     * @return
     */
    private Collection<? extends CommonConfig> getCommonConfigs(final String configType) {
        if (CLIENT_CONFIG_TYPE.equals(configType)) {
            return serverConfig.getClientConfigs();
        } else {
            return Collections.emptyList();
        }
    }

}

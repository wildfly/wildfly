package org.jboss.as.xts;

import java.util.ArrayList;
import java.util.List;

import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerChainMetaData;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerMetaData;

/**
 * Class responsible for registering WSTX and JTAOverWSAT handlers.
 *
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 *
 */
public final class XTSHandlersManager {

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

    private final boolean enabled;

    public XTSHandlersManager(final boolean enabled) {
        this.enabled = enabled;
    }

    public UnifiedHandlerChainMetaData getHandlerChain() {
        List<UnifiedHandlerMetaData> handlers = new ArrayList<UnifiedHandlerMetaData>(2);
        if (enabled) {
            handlers.add(new UnifiedHandlerMetaData(BRIDGE_ENABLED_HANDLER_CLASS, BRIDGE_HANDLER_NAME, null, null, null,
                    null));
            handlers.add(new UnifiedHandlerMetaData(WSAT_ENABLED_HANDLER_CLASS, WSAT_HANDLER_NAME, null, null, null, null));
        } else {
            handlers.add(new UnifiedHandlerMetaData(BRIDGE_DISABLED_HANDLER_CLASS, BRIDGE_HANDLER_NAME, null, null, null,
                    null));
            handlers.add(new UnifiedHandlerMetaData(WSAT_DISABLED_HANDLER_CLASS, WSAT_HANDLER_NAME, null, null, null, null));
        }
        return new UnifiedHandlerChainMetaData(null, null, HANDLER_PROTOCOL_BINDINGS, handlers, false, HANDLER_CHAIN_ID);
    }
}

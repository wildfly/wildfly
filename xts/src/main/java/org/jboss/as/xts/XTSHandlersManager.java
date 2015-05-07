package org.jboss.as.xts;

import java.util.ArrayList;
import java.util.List;

import com.arjuna.mw.wst11.client.DisabledWSTXHandler;
import com.arjuna.mw.wst11.client.EnabledWSTXHandler;
import com.arjuna.mw.wst11.service.JaxWSHeaderContextProcessor;
import org.jboss.jbossts.txbridge.outbound.DisabledJTAOverWSATHandler;
import org.jboss.jbossts.txbridge.outbound.EnabledJTAOverWSATHandler;
import org.jboss.jbossts.txbridge.outbound.JaxWSTxOutboundBridgeHandler;
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

    private static final String WSAT_HANDLER_NAME = JaxWSHeaderContextProcessor.class.getSimpleName();

    /**
     * WS-AT handler used when default context propagation is enabled.
     */
    private static final String WSAT_ENABLED_HANDLER_CLASS = EnabledWSTXHandler.class.getName();

    /**
     * WS-AT handler used when default context propagation is disabled.
     */
    private static final String WSAT_DISABLED_HANDLER_CLASS = DisabledWSTXHandler.class.getName();

    private static final String BRIDGE_HANDLER_NAME = JaxWSTxOutboundBridgeHandler.class.getSimpleName();

    /**
     * JTAOverWSAT handler used when default context propagation is enabled.
     */
    private static final String BRIDGE_ENABLED_HANDLER_CLASS = EnabledJTAOverWSATHandler.class.getName();

    /**
     * JTAOverWSAT handler used when default context propagation is disabled.
     */
    private static final String BRIDGE_DISABLED_HANDLER_CLASS = DisabledJTAOverWSATHandler.class.getName();

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

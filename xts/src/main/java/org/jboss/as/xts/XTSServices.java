/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.xts;

import org.jboss.as.xts.logging.XtsAsLogger;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:adinn@redhat.com">Andrew Dinn</a>
 */
public final class XTSServices {

    public static final ServiceName JBOSS_XTS = ServiceName.JBOSS.append("xts");

    public static final ServiceName JBOSS_XTS_MAIN = JBOSS_XTS.append("main");

    public static final ServiceName JBOSS_XTS_HANDLERS = JBOSS_XTS.append("handlers");

    public static final ServiceName JBOSS_XTS_ENDPOINT = JBOSS_XTS.append("endpoint");

    public static final ServiceName JBOSS_XTS_TXBRIDGE_INBOUND_RECOVERY = JBOSS_XTS.append("txbridgeInboundRecovery");

    public static final ServiceName JBOSS_XTS_TXBRIDGE_OUTBOUND_RECOVERY = JBOSS_XTS.append("txbridgeOutboundRecovery");

    public static ServiceName endpointServiceName(String name) {
        return JBOSS_XTS_ENDPOINT.append(name);
    }

    public static <T> T notNull(T value) {
        if (value == null) throw XtsAsLogger.ROOT_LOGGER.xtsServiceIsNotStarted();
        return value;
    }

    private XTSServices() {
    }
}

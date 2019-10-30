/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

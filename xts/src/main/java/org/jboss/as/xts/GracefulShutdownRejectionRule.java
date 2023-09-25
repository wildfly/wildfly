/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.xts;

import com.arjuna.webservices.wsarj.ArjunaConstants;
import com.arjuna.webservices11.wscoor.CoordinationConstants;
import org.jboss.as.xts.logging.XtsAsLogger;
import org.jboss.wsf.spi.invocation.RejectionRule;

import javax.xml.namespace.QName;
import java.util.Map;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class GracefulShutdownRejectionRule implements RejectionRule {

    @Override
    public boolean rejectMessage(Map<QName, Object> headers) {
        if (headers.containsKey(ArjunaConstants.WSARJ_ELEMENT_INSTANCE_IDENTIFIER_QNAME)
                || headers.containsKey(CoordinationConstants.WSCOOR_ELEMENT_COORDINATION_CONTEXT_QNAME)) {
            return false;
        }
        XtsAsLogger.ROOT_LOGGER.rejectingCallBecauseNotPartOfXtsTx();
        return true;
    }
}

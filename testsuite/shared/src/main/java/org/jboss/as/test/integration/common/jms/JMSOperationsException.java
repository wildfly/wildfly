/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.common.jms;

/**
 * @author <a href="jmartisk@redhat.com">Jan Martiska</a>
 */
public class JMSOperationsException extends RuntimeException {

    JMSOperationsException(final String msg, final Throwable cause) {
        super(msg, cause);
    }

    JMSOperationsException(final String msg) {
        super(msg);
    }

    public JMSOperationsException(Throwable cause) {
        super(cause);
    }
}

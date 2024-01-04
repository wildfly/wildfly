/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.xts;

/**
 * @author <a href="mailto:adinn@redhat.com">Andrew Dinn</a>
 */
interface CommonAttributes {
    String HOST = "host";
    String XTS_ENVIRONMENT= "xts-environment";
    String DEFAULT_CONTEXT_PROPAGATION = "default-context-propagation";
    String ASYNC_REGISTRATION = "async-registration";
    // TODO, many more!
}

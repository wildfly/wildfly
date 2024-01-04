/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.deployers.spec.rars.ra16inoutmultianno;

import org.jboss.as.connector.deployers.spec.rars.BaseActivationSpec;

import jakarta.resource.spi.Activation;

/**
 * TestActivationSpec
 *
 * @author <a href="mailto:jeff.zhang@ironjacamar.org">Jeff Zhang</a>
 * @version $Revision: $
 */
@Activation(messageListeners = {})
public class TestActivationSpec extends BaseActivationSpec {

}

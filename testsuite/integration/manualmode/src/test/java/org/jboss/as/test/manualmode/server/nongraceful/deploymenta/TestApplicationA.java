/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.manualmode.server.nongraceful.deploymenta;


import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * @author Paul Ferraro
 */
@ApplicationPath("/testa")
public class TestApplicationA extends Application {

}

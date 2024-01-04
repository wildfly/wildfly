/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.injection;

import jakarta.ejb.Stateless;
import java.io.Serializable;

/**
 * @author Stuart Douglas
 */
@Stateless
public class Bus implements Serializable {
}

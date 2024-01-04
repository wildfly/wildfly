/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.serialization;

import java.io.Serializable;

/**
 * @author Stuart Douglas
 */
public class InfoB implements Serializable {
    public InfoC infoC = new InfoC();
}

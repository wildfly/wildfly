/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.serialization;

import java.io.Serializable;

/**
 * @author Stuart Douglas
 */
public class InfoA implements Serializable{

    public InfoB infoB = new InfoB();
}

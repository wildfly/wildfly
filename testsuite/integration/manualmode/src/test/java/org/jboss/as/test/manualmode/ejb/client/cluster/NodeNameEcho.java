/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.manualmode.ejb.client.cluster;

/**
 * @author Jaikiran Pai
 */
public interface NodeNameEcho {
    String getNodeName(boolean preferOtherNode);
}

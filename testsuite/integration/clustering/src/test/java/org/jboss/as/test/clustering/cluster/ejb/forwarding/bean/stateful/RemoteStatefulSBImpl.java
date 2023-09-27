/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.forwarding.bean.stateful;

import jakarta.ejb.Stateful;

import org.jboss.as.test.clustering.cluster.ejb.forwarding.bean.common.CommonStatefulSBImpl;

/**
 * @author Radoslav Husar
 */
@Stateful
public class RemoteStatefulSBImpl extends CommonStatefulSBImpl implements RemoteStatefulSB {
}

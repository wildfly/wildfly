/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.forwarding.bean.stateful;

import jakarta.ejb.Remote;

import org.jboss.as.test.clustering.cluster.ejb.forwarding.bean.common.CommonStatefulSB;

/**
 * The enterprise bean must implement a business interface. That is, remote clients may not access an enterprise bean through a
 * no-interface view.
 *
 * @author Radoslav Husar
 */
@Remote
public interface RemoteStatefulSB extends CommonStatefulSB {
}
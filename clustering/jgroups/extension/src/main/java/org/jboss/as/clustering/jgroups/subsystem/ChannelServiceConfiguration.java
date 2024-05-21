/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.function.Consumer;

import org.jboss.as.controller.RequirementServiceBuilder;
import org.wildfly.clustering.jgroups.ChannelFactory;

/**
 * @author Paul Ferraro
 */
public interface ChannelServiceConfiguration extends Consumer<RequirementServiceBuilder<?>> {

    boolean isStatisticsEnabled();

    ChannelFactory getChannelFactory();

    String getClusterName();
}

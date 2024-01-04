/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.OperationStepHandler;

/**
 * A described add operation handler.
 * @author Paul Ferraro
 */
public interface DescribedAddStepHandler extends OperationStepHandler, Described<AddStepHandlerDescriptor> {

}

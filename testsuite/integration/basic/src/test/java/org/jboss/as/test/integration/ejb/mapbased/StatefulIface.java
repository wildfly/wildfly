/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.mapbased;

/**
 * @author Jan Martiska / jmartisk@redhat.com
 */
public interface StatefulIface {

    String getCallerPrincipalName();

}

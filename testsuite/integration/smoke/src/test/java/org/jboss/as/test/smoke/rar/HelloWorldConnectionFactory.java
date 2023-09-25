/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.rar;

import java.io.Serializable;

import jakarta.resource.Referenceable;
import jakarta.resource.ResourceException;

/**
 * HelloWorldConnectionFactory
 *
 * @version $Revision: $
 */
public interface HelloWorldConnectionFactory extends Serializable, Referenceable {
   /**
    * get connection from factory
    *
    * @return HelloWorldConnection instance
    * @exception ResourceException Thrown if a connection can't be obtained
    */
   HelloWorldConnection getConnection() throws ResourceException;

}

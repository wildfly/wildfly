/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.manualmode.ejb.ssl.beans;

import jakarta.ejb.Remote;
import java.util.concurrent.Future;

/**
 * @author Ondrej Chaloupka
 * @author Jan Martiska
 */
@Remote
public interface StatelessBeanRemote {
    String ANSWER = "Hello";

    String sayHello();

    Future<String> sayHelloAsync();

}

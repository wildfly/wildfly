/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.naming.shared;

import jakarta.ejb.Remote;
import java.util.concurrent.TimeUnit;

/**
 * @author Eduardo Martins
 */
@Remote
public interface TestResults {

    String SHARED_BINDING_NAME_ONE = "java:global/sharedbinds/one";
    String SHARED_BINDING_NAME_TWO = "java:global/sharedbinds/two";

    boolean isPostContructOne();

    void setPostContructOne(boolean postContructOne);

    boolean isPostContructTwo();

    void setPostContructTwo(boolean postContructTwo);

    boolean isPreDestroyOne();

    void setPreDestroyOne(boolean preDestroyOne);

    boolean isPreDestroyTwo();

    void setPreDestroyTwo(boolean preDestroyTwo);

    void await(long timeout, TimeUnit timeUnit) throws InterruptedException;
}

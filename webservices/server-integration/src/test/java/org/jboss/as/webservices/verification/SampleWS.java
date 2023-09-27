/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.webservices.verification;

import jakarta.jws.WebService;

/**
 * @author sfcoy
 *
 */
@WebService(name="SampleWS", targetNamespace="urn:sample")
public interface SampleWS {

    void performWork();

    String discoverNewLands();

    boolean isWorking();

    void triggerReport();

}

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
@WebService(portName = "sample-ws-port", wsdlLocation = "/META-INF/wsdl/sample.wsdl", endpointInterface = "org.jboss.as.webservices.verification.SampleWS")
public class GoodSampleWSImpl {

    public void performWork() {
    }

    public String discoverNewLands() {
        return "Wallaby Hill";
    }

    public boolean isWorking() {
        return true;
    }

    public void triggerReport() {
    }
}

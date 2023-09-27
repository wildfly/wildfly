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
public class BrokenSampleWSImpl {

    void performWork() {

    }

    public static String discoverNewLands() {
        return "Wallaby Hill";
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    public final void triggerReport() {
    }

//    public boolean isWorking() {
//        return false;
//    }

}

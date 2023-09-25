/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.verification;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

/**
 * @author sfcoy
 *
 */
@WebService
public class AnnotatedSampleWSImpl implements SampleWS {

    @Override
    @WebMethod
    public void performWork() {

    }

    @Override
    @WebMethod
    public String discoverNewLands() {
        return null;
    }

    @Override
    @WebMethod
    public boolean isWorking() {
        return false;
    }

    @Override
    @WebMethod
    public void triggerReport() {

    }

}

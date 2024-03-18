/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.web.httpinvoker;

import org.jboss.dmr.ModelNode;
import org.junit.Assert;

public class HTTPInvokerSecuredElytronTestCase extends HTTPInvokerSecuredTestCase {

    @Override
    protected void validateOperation(ModelNode operationResult) {
        Assert.assertEquals("The http-authentication-factory should be set",
            "application-http-authentication", operationResult.get("http-authentication-factory").asString());
        Assert.assertFalse("The security-realm should be undefined",
            operationResult.get("security-realm").isDefined());
    }
}

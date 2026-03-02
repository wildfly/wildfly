/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.servlet.async;

import java.io.IOException;

public class AsyncServletSetTimeoutTestCase extends AsyncServletTimeoutTestCaseBase {

    @Override
    protected String getUrl() {
        return url + "init/execute?timeout=" + getTimeout();
    }

    @Override
    protected long getTimeout() {
        return 1000;
    }

    @Override
    protected void setup() throws IOException {
        // NOP
    }

}

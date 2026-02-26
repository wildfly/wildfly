/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.web.timeout;

import java.util.ArrayList;
import java.util.List;

import jakarta.ejb.Singleton;
import jakarta.enterprise.context.ApplicationScoped;

@Singleton
@ApplicationScoped
public class TimeoutLogBean implements TimeoutLog{

    List<ActionSequence> lst =  new ArrayList<ActionSequence>();
    @Override
    public void receivedMessage() {
        lst.add(ActionSequence.MSG_RECEIVED);
    }

    @Override
    public void sentResponse() {
        lst.add(ActionSequence.MSG_SENT);
    }

    @Override
    public void failedIO() {
        lst.add(ActionSequence.IO_EXCEPTION);
    }

    @Override
    public List<ActionSequence> getTestResult() {
        return lst;
    }

}

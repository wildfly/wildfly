/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.extension.rts.common;

import java.util.HashMap;
import java.util.Map;

import org.jboss.jbossts.star.util.TxStatus;

public final class Work {
    String id;
    String tid;
    String uri;
    String pLinks;
    String enlistUrl;
    String recoveryUrl;
    String fault;
    Map<String, String> oldState;
    Map<String, String> newState;
    String status;
    int vStatus = 0;
    int syncCount = 0;
    int commitCnt = 0;
    int prepareCnt = 0;
    int rollbackCnt = 0;
    int commmitOnePhaseCnt = 0;

    Work(String id, String tid, String uri, String pLinks, String enlistUrl, String recoveryUrl, String fault) {
        this.id = id;
        this.tid = tid;
        this.uri = uri;
        this.pLinks = pLinks;
        this.enlistUrl = enlistUrl;
        this.recoveryUrl = recoveryUrl;
        this.fault = fault;
        this.oldState = new HashMap<String, String>();
        this.newState = new HashMap<String, String>();
    }

    public void start() {
        newState.clear();
        newState.putAll(oldState);
    }

    public void end(boolean commit) {
        if (commit) {
            oldState.clear();
            oldState.putAll(newState);
        }
    }

    public boolean inTxn() {
        return status != null && TxStatus.fromStatus(status).isActive();
        // return TxSupport.isActive(status);
    }

}

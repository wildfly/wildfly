/*
* JBoss, Home of Professional Open Source.
* Copyright 2013, Red Hat Middleware LLC, and individual contributors
* as indicated by the @author tags. See the copyright.txt file in the
* distribution for a full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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

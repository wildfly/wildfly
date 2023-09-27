/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.multinode.batch.stoprestart;

import java.util.Properties;
import jakarta.batch.runtime.BatchStatus;
import jakarta.ejb.Remote;

@Remote
public interface BatchClientIF {

    long start(String jobName, Properties jobParams);

    void stop(long jobExecutionId);

    long restart(long jobExecutionId, Properties restartParams);

    BatchStatus getJobStatus(long jobExecutionId);
}

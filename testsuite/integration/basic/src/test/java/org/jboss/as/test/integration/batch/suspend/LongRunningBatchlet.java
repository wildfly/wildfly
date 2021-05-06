/*
 * Copyright 2021 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.integration.batch.suspend;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.batch.api.BatchProperty;
import javax.batch.api.Batchlet;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * <p>Batchlet that runs until the static method <em>success</em> is called.
 * It is used to perform the suspend and resume while this job is running.
 * Only one job of this class can be executed simultaneously.</p>
 *
 * @author rmartinc
 */
@Named
public class LongRunningBatchlet implements Batchlet {

    @Inject
    @BatchProperty(name = "max.seconds")
    private Integer maxSeconds;

    private static final AtomicBoolean success = new AtomicBoolean(false);
    private static CountDownLatch latch = new CountDownLatch(0);

    public static synchronized boolean isStarted() {
        return latch.getCount() > 0;
    }

    public static synchronized void success() throws Exception {
        if (!success.compareAndSet(false, true)) {
            throw new Exception("Called twice!");
        }
        latch.countDown();
    }

    public static synchronized void reset() throws Exception {
        if (latch.getCount() > 0) {
            throw new Exception("The job is not finished!");
        }
        success.set(false);
        latch = new CountDownLatch(1);
    }

    @Override
    public String process() throws Exception {
        reset();
        latch.await(maxSeconds, TimeUnit.SECONDS);
        String exitStatus = success.get()? "OK" : "KO";
        return exitStatus;
    }

    @Override
    public void stop() throws Exception {
        latch.countDown();
    }
}

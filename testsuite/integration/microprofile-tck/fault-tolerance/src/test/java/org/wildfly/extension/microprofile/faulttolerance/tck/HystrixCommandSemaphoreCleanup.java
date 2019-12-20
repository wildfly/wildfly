/*
 * Copyright 2018 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.microprofile.faulttolerance.tck;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;

/**
 * Ported from SmallRye Fault Tolerance TCK.
 * See also https://github.com/Netflix/Hystrix/issues/1889
 *
 * @author Martin Kouba
 * @author Radoslav Husar
 */
@ApplicationScoped
public class HystrixCommandSemaphoreCleanup {

    // Initialize eagerly
    void init(@Observes @Initialized(ApplicationScoped.class) Object event) {
    }

    @PreDestroy
    void onShutdown() {
        DummyCommand.clearSemaphoreCache();
    }

    static class DummyCommand extends HystrixCommand<Void> {

        protected DummyCommand() {
            super(HystrixCommandGroupKey.Factory.asKey("DummyGroup"));
        }

        @Override
        protected Void run() {
            return null;
        }

        static void clearSemaphoreCache() {
            fallbackSemaphorePerCircuit.clear();
            executionSemaphorePerCircuit.clear();
            System.out.println("### Clear command semaphore cache ###");
        }

    }
}

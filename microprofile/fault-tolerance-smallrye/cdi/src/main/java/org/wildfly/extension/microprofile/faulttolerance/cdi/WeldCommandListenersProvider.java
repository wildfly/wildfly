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
package org.wildfly.extension.microprofile.faulttolerance.cdi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;

import org.jboss.weld.inject.WeldInstance;
import org.jboss.weld.inject.WeldInstance.Handler;

import io.smallrye.faulttolerance.CommandListener;
import io.smallrye.faulttolerance.DefaultCommandListenersProvider;
import io.smallrye.faulttolerance.config.FaultToleranceOperation;

/**
 * CDI API is limited and SmallRye does not directly depend on Weld API, only on CDI API.
 * To avoid leaks for listeners which are {@link Dependent} also to ensure {@link javax.annotation.PreDestroy} is called.
 *
 * @author Martin Kouba
 * @author Radoslav Husar
 */
@Priority(1000)
@Alternative
@ApplicationScoped
public class WeldCommandListenersProvider extends DefaultCommandListenersProvider {

    @Inject
    WeldInstance<CommandListener> listeners;

    @Override
    public List<CommandListener> getCommandListeners() {
        if (listeners.isUnsatisfied()) {
            return null;
        }
        List<CommandListener> commandListeners = new ArrayList<>();
        for (Handler<CommandListener> handler : listeners.handlers()) {
            if (Dependent.class.equals(handler.getBean().getScope())) {
                // Wrap dependent listener
                commandListeners.add(new CommandListener() {

                    @Override
                    public void beforeExecution(FaultToleranceOperation operation) {
                        handler.get().beforeExecution(operation);
                    }

                    @Override
                    public void afterExecution(FaultToleranceOperation operation) {
                        handler.get().afterExecution(operation);
                        handler.destroy();
                    }

                    @Override
                    public int getPriority() {
                        return handler.get().getPriority();
                    }

                });
            } else {
                commandListeners.add(handler.get());
            }
        }
        Collections.sort(commandListeners);
        return commandListeners;
    }

}
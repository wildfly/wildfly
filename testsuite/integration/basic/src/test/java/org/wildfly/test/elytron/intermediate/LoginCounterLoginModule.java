/*
 * Copyright 2020 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.test.elytron.intermediate;

import java.security.Principal;
import java.security.acl.Group;
import java.util.concurrent.atomic.AtomicInteger;
import javax.security.auth.login.LoginException;
import org.jboss.security.auth.spi.AbstractServerLoginModule;

/**
 * Login module that counts the number of times called. Configure with
 * password-stacking set to useFirstPass and with flag to optional.
 *
 * @author rmartinc
 */
public class LoginCounterLoginModule extends AbstractServerLoginModule {

    private static final AtomicInteger timesCalled = new AtomicInteger(0);
    private static final AtomicInteger timesSuccess  = new AtomicInteger(0);

    @Override
    public boolean login() throws LoginException {
        boolean result = super.login();
        timesCalled.incrementAndGet();
        if (result) {
            timesSuccess.incrementAndGet();
        }
        return result;
    }

    public static int getTimesCalled() {
        return timesCalled.get();
    }

    public static int getTimesSuccess() {
        return timesSuccess.get();
    }

    @Override
    public boolean commit() throws LoginException {
        return true;
    }

    @Override
    protected Principal getIdentity() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    protected Group[] getRoleSets() throws LoginException {
        throw new UnsupportedOperationException("Not supported.");
    }

}

/*
 * Copyright 2021 Red Hat, Inc.
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
package org.jboss.as.test.integration.ejb.security.cache;

import java.security.Principal;
import java.util.concurrent.ConcurrentHashMap;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Remote;
import javax.ejb.Singleton;
import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;

/**
 * Implementation of the RemotePicketboxCacheValidator using a concurrent hash
 * map for the cache.
 *
 * @author rmartinc
 */
@Singleton
@Remote(RemotePicketboxCacheValidator.class)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class PicketboxCacheValidatorBean implements RemotePicketboxCacheValidator {

    private final ConcurrentHashMap<Principal, Object> cache;

    public PicketboxCacheValidatorBean() {
        cache = new ConcurrentHashMap<>();
    }

    @Override
    public Result check() {
        SecurityContext sc = SecurityContextAssociation.getSecurityContext();
        if (sc == null) {
            return new Result(Status.ERROR);
        }
        Object credential = sc.getUtil().getCredential();
        Principal principal = sc.getUtil().getUserPrincipal();
        if (credential == null || principal == null) {
            return new Result(Status.ERROR);
        }
        Object cachedCredential = cache.get(principal);
        if (cachedCredential == null) {
            cache.put(principal, credential);
            return new Result(principal.getName(), Status.NEW);
        } else if (cachedCredential.equals(credential)) {
            return new Result(principal.getName(), Status.CACHED);
        } else {
            return new Result(principal.getName(), Status.INVALID);
        }
    }
}

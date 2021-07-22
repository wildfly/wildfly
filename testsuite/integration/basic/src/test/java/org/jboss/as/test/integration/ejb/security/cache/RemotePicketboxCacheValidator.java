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

import java.io.Serializable;
import javax.ejb.Remote;

/**
 * Interface for an EJB that checks the principal and credential of a picketbox
 * login is cacheable as before.
 *
 * @author rmartinc
 */
@Remote
public interface RemotePicketboxCacheValidator {

    /**
     * The status of the picketbox principal/credential in the cache.
     */
    public enum Status {

        /**
         * Error obtaining the picketbox principal or credential.
         */
        ERROR,

        /**
         * The principal is new to the cache.
         */
        NEW,

        /**
         * The principal is in the cache and the credential is also the
         * one stored in it.
         */
        CACHED,

        /**
         * The principal is in the cache but the credential passed is different
         * to the one cached.
         */
        INVALID
    };

    /**
     * The result of the check.
     */
    public class Result implements Serializable {
        private String name;
        private Status status;

        public Result() {
            this.name = null;
            this.status = null;
        }

        public Result(Status status) {
            this.name = null;
            this.status = status;
        }

        public Result(String name, Status status) {
            this.name = name;
            this.status = status;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Status getStatus() {
            return status;
        }

        public void setStatus(Status status) {
            this.status = status;
        }

        @Override
        public String toString() {
            String result = this.status.name();
            if (this.name != null) {
                result += "(" + this.name + ")";
            }
            return result;
        }
    }

    /**
     * Method that checks if the picketbox logged principal and credential
     * is inside a cache.
     * @return The status of the principal in the cache
     */
    public Result check();
}

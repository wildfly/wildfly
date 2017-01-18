/*
 * Copyright 2017 Red Hat, Inc.
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
package org.jboss.as.connector.security;

import org.jboss.jca.core.spi.security.SubjectFactory;

import javax.security.auth.Subject;

import static org.jboss.as.connector.logging.ConnectorLogger.ROOT_LOGGER;

/**
 * SubjectFactory implementation for Elytron.
 *
 * @author Flavia Rainone
 */
public class ElytronSubjectFactory implements SubjectFactory {

    /**
     * Constructor
     */
    public ElytronSubjectFactory() {

    }

    /**
     * {@inheritDoc}
     */
    public Subject createSubject() {
        Subject subject = null; // TODO
        if (ROOT_LOGGER.isTraceEnabled()) {
            ROOT_LOGGER.subject(subject, Integer.toHexString(System.identityHashCode(subject)));
        }
        return subject;
    }

    /**
     * {@inheritDoc}
     */
    public Subject createSubject(String authenticationContext) {
        Subject subject = null; // TODO

        if (ROOT_LOGGER.isTraceEnabled()) {
            ROOT_LOGGER.subject(subject, Integer.toHexString(System.identityHashCode(subject)));
        }
        return subject;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("ElytronSubjectFactory@").append(Integer.toHexString(System.identityHashCode(this)));
        sb.append("]");

        return sb.toString();
    }
}
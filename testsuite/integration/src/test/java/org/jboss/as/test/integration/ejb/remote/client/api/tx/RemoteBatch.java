/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.remote.client.api.tx;

/**
 * User: jpai
 */
public interface RemoteBatch {

    public void createBatch(final String batchName);

    public void step1(final String batchName, final String stepName);

    public void successfulStep2(final String batchName, final String stepName);

    public void appExceptionFailingStep2(final String batchName, final String stepName) throws SimpleAppException;

    public void systemExceptionFailingStep2(final String batchName, final String stepName);

    public void independentStep3(final String batchName, final String stepName);

    public void step4(final String batchName, final String stepName);
}

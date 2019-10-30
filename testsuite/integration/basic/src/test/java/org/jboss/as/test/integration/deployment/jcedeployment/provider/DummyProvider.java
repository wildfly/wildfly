/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.deployment.jcedeployment.provider;

import java.security.Provider;

/**
 * Testing JCE provider which provides one dummy cipher only.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
public final class DummyProvider extends Provider {

    public static String PROVIDER_NAME = "DP";
    public static String DUMMY_CIPHER = "dummycipher";

    public DummyProvider() {
        super(PROVIDER_NAME, 0.1, "Dummy Provider v0.1");

        put("Cipher.DummyAlg/DummyMode/DummyPadding", DummyCipherSpi.class.getName());
    }

}

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.infinispan.session;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.nio.file.FileSystems;
import java.security.AllPermission;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;
import org.wildfly.clustering.web.annotation.Immutable;

/**
 * Unit test for {@link MutableDetector}
 * @author Paul Ferraro
 */
public class MutableDetectorTestCase {

    @Test
    public void isMutable() throws MalformedURLException, UnknownHostException {
        assertTrue(MutableDetector.isMutable(new Object()));
        assertTrue(MutableDetector.isMutable(new Date()));
        assertTrue(MutableDetector.isMutable(new AtomicInteger()));
        assertTrue(MutableDetector.isMutable(new AtomicLong()));
        assertFalse(MutableDetector.isMutable(null));
        assertFalse(MutableDetector.isMutable(Collections.EMPTY_LIST));
        assertFalse(MutableDetector.isMutable(Collections.EMPTY_MAP));
        assertFalse(MutableDetector.isMutable(Collections.EMPTY_SET));
        assertFalse(MutableDetector.isMutable(Boolean.TRUE));
        assertFalse(MutableDetector.isMutable(Character.valueOf('a')));
        assertFalse(MutableDetector.isMutable(Currency.getInstance(Locale.US)));
        assertFalse(MutableDetector.isMutable(TimeUnit.DAYS));
        assertFalse(MutableDetector.isMutable(Locale.getDefault()));
        assertFalse(MutableDetector.isMutable(Byte.valueOf(Integer.valueOf(1).byteValue())));
        assertFalse(MutableDetector.isMutable(Short.valueOf(Integer.valueOf(1).shortValue())));
        assertFalse(MutableDetector.isMutable(Integer.valueOf(1)));
        assertFalse(MutableDetector.isMutable(Long.valueOf(1)));
        assertFalse(MutableDetector.isMutable(Float.valueOf(1)));
        assertFalse(MutableDetector.isMutable(Double.valueOf(1)));
        assertFalse(MutableDetector.isMutable(BigInteger.valueOf(1)));
        assertFalse(MutableDetector.isMutable(BigDecimal.valueOf(1)));
        assertFalse(MutableDetector.isMutable(InetAddress.getLocalHost()));
        assertFalse(MutableDetector.isMutable(new InetSocketAddress(InetAddress.getLocalHost(), 80)));
        assertFalse(MutableDetector.isMutable(MathContext.UNLIMITED));
        assertFalse(MutableDetector.isMutable("test"));
        assertFalse(MutableDetector.isMutable(TimeZone.getDefault()));
        assertFalse(MutableDetector.isMutable(UUID.randomUUID()));
        File file = new File(System.getProperty("user.home"));
        assertFalse(MutableDetector.isMutable(file));
        assertFalse(MutableDetector.isMutable(file.toURI()));
        assertFalse(MutableDetector.isMutable(file.toURI().toURL()));
        assertFalse(MutableDetector.isMutable(FileSystems.getDefault().getRootDirectories().iterator().next()));
        assertFalse(MutableDetector.isMutable(new AllPermission()));
        assertFalse(MutableDetector.isMutable(new ImmutableObject()));
    }

    @Immutable
    static class ImmutableObject {
    }
}

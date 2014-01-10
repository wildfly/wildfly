/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.process.stdin;

import java.io.OutputStream;

/**
 * Variant of the <a href="http://commons.apache.org/proper/commons-codec">Commons Codec</a> project's class
 * of the same name. See {@link org.jboss.as.process.stdin.Base64} for an explanation of the rationale
 * for creating the variants in this package.
 * <p>
 * Provides Base64 encoding and decoding in a streaming fashion (unlimited size).
 * </p>
 * <p>
 * The behaviour of the Base64OutputStream is to ENCODE, whereas the  behaviour of the Base64InputStream
 * is to DECODE.
 * </p>
 * <p>
 * This class implements section <cite>6.8. Base64 Content-Transfer-Encoding</cite> from RFC 2045 <cite>Multipurpose
 * Internet Mail Extensions (MIME) Part One: Format of Internet Message Bodies</cite> by Freed and Borenstein.
 * </p>
 * <p>
 * Since this class operates directly on byte streams, and not character streams, it is hard-coded to only encode/decode
 * character encodings which are compatible with the lower 127 ASCII chart (ISO-8859-1, Windows-1252, UTF-8, etc).
 * </p>
 *
 * @see <a href="http://www.ietf.org/rfc/rfc2045.txt">RFC 2045</a>
 */
public class Base64OutputStream extends BaseNCodecOutputStream {

    /**
     * Creates a Base64OutputStream such that all data written is either Base64-encoded to the
     * original provided OutputStream.
     *
     * @param out
     *            OutputStream to wrap.
     *
     */
    public Base64OutputStream(final OutputStream out) {
        super(out, new Base64(), true);
    }
}

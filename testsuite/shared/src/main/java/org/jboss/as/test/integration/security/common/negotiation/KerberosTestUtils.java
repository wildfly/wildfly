/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.security.common.negotiation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Enumerated;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERApplicationSpecific;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.util.ASN1Dump;
import org.jboss.logging.Logger;
import org.junit.AssumptionViolatedException;

/**
 * Helper methods for JGSSAPI &amp; SPNEGO &amp; Kerberos testcases. It mainly helps to skip tests on configurations which
 * contains issues.
 *
 * @author Josef Cacek
 */
public final class KerberosTestUtils {

    private static final Logger LOGGER = Logger.getLogger(KerberosTestUtils.class);

    public static final String OID_KERBEROS_V5 = "1.2.840.113554.1.2.2";
    public static final String OID_KERBEROS_V5_LEGACY = "1.2.840.48018.1.2.2";
    public static final String OID_NTLM = "1.3.6.1.4.1.311.2.2.10";
    public static final String OID_SPNEGO = "1.3.6.1.5.5.2";
    public static final String OID_DUMMY = "1.1.2.5.6.7";

    /**
     * Just a private constructor.
     */
    private KerberosTestUtils() {
        // It's OK to be empty - we don't instantiate this class.
    }

    /**
     * This method throws an {@link AssumptionViolatedException} (i.e. it skips the test-case) if the configuration is
     * unsupported for HTTP authentication with Kerberos. Configuration in this case means combination of [ hostname used | JDK
     * vendor | Java version ].
     *
     * @throws AssumptionViolatedException
     */
    public static void assumeKerberosAuthenticationSupported() throws AssumptionViolatedException {
        if (isIPV6()) {
            throw new AssumptionViolatedException(
                    "Kerberos tests are not supported when hostname is not available for tested IPv6 address. Find more info in https://issues.jboss.org/browse/WFLY-5409");
        }
    }

    /**
     * Returns true if provided hostname is an IPv6 address.
     *
     * @return
     */
    private static boolean isIPV6() {
        return System.getProperty("ipv6") != null;
    }

    /**
     * Generates SPNEGO init token with given initial ticket and supported mechanisms.
     *
     * @param ticket initial ticket for the preferred (the first) mechanism.
     * @param supMechOids object identifiers (OIDs) of supported mechanisms for the SPNEGO.
     * @return ASN.1 encoded SPNEGO init token
     */
    public static byte[] generateSpnegoTokenInit(byte[] ticket, String... supMechOids) throws IOException {
        DEROctetString ticketForPreferredMech = new DEROctetString(ticket);

        ASN1EncodableVector mechSeq = new ASN1EncodableVector();
        for (String mech : supMechOids) {
            mechSeq.add(new ASN1ObjectIdentifier(mech));
        }
        DERTaggedObject taggedMechTypes = new DERTaggedObject(0, new DERSequence(mechSeq));
        DERTaggedObject taggedMechToken = new DERTaggedObject(2, ticketForPreferredMech);
        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(taggedMechTypes);
        v.add(taggedMechToken);
        DERSequence seqNegTokenInit = new DERSequence(v);
        DERTaggedObject taggedSpnego = new DERTaggedObject(0, seqNegTokenInit);

        ASN1EncodableVector appVec = new ASN1EncodableVector();
        appVec.add(new ASN1ObjectIdentifier(OID_SPNEGO));
        appVec.add(taggedSpnego);
        DERApplicationSpecific app = new DERApplicationSpecific(0, appVec);
        return app.getEncoded();
    }

    /**
     * Generates SPNEGO response (to a "select mechanism challenge") with given bytes as the ticket for selected mechanism.
     *
     * @param ticket
     * @return ASN.1 encoded SPNEGO response
     */
    public static byte[] generateSpnegoTokenResp(byte[] ticket) throws IOException {
        DEROctetString ourKerberosTicket = new DEROctetString(ticket);

        DERTaggedObject taggedNegState = new DERTaggedObject(0, new ASN1Enumerated(1)); // accept-incomplete
        DERTaggedObject taggedResponseToken = new DERTaggedObject(2, ourKerberosTicket);
        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(taggedNegState);
        v.add(taggedResponseToken);
        DERSequence seqNegTokenResp = new DERSequence(v);
        DERTaggedObject taggedSpnego = new DERTaggedObject(1, seqNegTokenResp);
        return taggedSpnego.getEncoded();
    }

    /**
     * Dumps ASN.1 object as String from given byte array.
     *
     * @param data
     */
    public static String dumpAsn1Obj(byte[] data) throws IOException {
        if (data == null)
            return null;
        try (ASN1InputStream bIn = new ASN1InputStream(new ByteArrayInputStream(data))) {
            return ASN1Dump.dumpAsString(bIn.readObject(), true);
        } catch (Exception e) {
            LOGGER.debug("ASN1Dump failed", e);
            return "[Unable to dump ASN.1: " + Base64.getEncoder().encodeToString(data) + " ]";
        }
    }

    /**
     * Dumps ASN.1 object as String from WWW-Authenticate/Negotiate HTTP header.
     *
     * @param response
     */
    public static String dumpNegotiateHeader(HttpResponse response) throws IOException {
        if (response == null)
            return null;
        final String negotiatePrefix = "Negotiate ";
        for (Header header : response.getHeaders("WWW-Authenticate")) {
            final String value = header.getValue();
            if (value.startsWith(negotiatePrefix)) {
                byte[] token = Base64.getDecoder().decode(value.substring(negotiatePrefix.length()));
                return dumpAsn1Obj(token);
            }
        }
        return null;
    }
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.certs.ocsp;

import org.bouncycastle.asn1.ocsp.OCSPResponseStatus;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.ocsp.BasicOCSPResp;
import org.bouncycastle.cert.ocsp.BasicOCSPRespBuilder;
import org.bouncycastle.cert.ocsp.CertificateID;
import org.bouncycastle.cert.ocsp.CertificateStatus;
import org.bouncycastle.cert.ocsp.OCSPReq;
import org.bouncycastle.cert.ocsp.OCSPResp;
import org.bouncycastle.cert.ocsp.OCSPRespBuilder;
import org.bouncycastle.cert.ocsp.Req;
import org.bouncycastle.cert.ocsp.RespID;
import org.bouncycastle.cert.ocsp.RevokedStatus;
import org.bouncycastle.cert.ocsp.UnknownStatus;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.jboss.logging.Logger;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.math.BigInteger;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * Simple OCSP responder using BouncyCastle for testing purposes.
 */
public class SimpleOcspServer {
    private static final Logger log = Logger.getLogger(SimpleOcspServer.class);
    private final int port;
    private ClientAndServer server;
    private final X509Certificate responderCert;
    private final PrivateKey responderKey;
    private static final CertificateStatus GOOD_STATUS_MARKER = new CertificateStatus() {};
    private final Map<CertId, CertificateStatus> certStatuses = new ConcurrentHashMap<>();

    public SimpleOcspServer(int port, X509Certificate responderCert, PrivateKey responderKey) {
        this.port = port;
        this.responderCert = responderCert;
        this.responderKey = responderKey;
    }

    public void start() {
        server = new ClientAndServer(port);
        server.when(
                request()
                        .withMethod("POST")
                        .withPath("/ocsp"),
                Times.unlimited())
                .respond(this::handleOcspRequest);
        server.when(
                request()
                        .withMethod("GET")
                        .withPath("/ocsp/.*"),
                Times.unlimited())
                .respond(this::handleOcspRequest);
    }

    public void stop() {
        if (server != null) {
            server.stop();
        }
    }

    public void addCertificate(X509Certificate issuerCert, X509Certificate cert, RevocationStatus status) {
        try {
            X509CertificateHolder issuerHolder = new JcaX509CertificateHolder(issuerCert);
            CertificateID certId = new CertificateID(
                    new JcaDigestCalculatorProviderBuilder().build().get(CertificateID.HASH_SHA1),
                    issuerHolder,
                    cert.getSerialNumber()
            );

            CertificateStatus certStatus = convertRevocationStatus(status);
            CertId key = new CertId(certId);
            certStatuses.put(key, certStatus);
        } catch (Exception e) {
            throw new RuntimeException("Failed to add certificate", e);
        }
    }

    private CertificateStatus convertRevocationStatus(RevocationStatus status) {
        if (status == RevocationStatus.GOOD) {
            return GOOD_STATUS_MARKER;
        } else if (status == RevocationStatus.REVOKED) {
            return new RevokedStatus(new Date(), CRLReason.unspecified);
        } else {
            return new UnknownStatus();
        }
    }

    private HttpResponse handleOcspRequest(HttpRequest request) {
        try {
            byte[] requestBytes = parseOcspRequestBytes(request);
            OCSPReq ocspRequest = new OCSPReq(requestBytes);
            OCSPResp ocspResponse = buildOcspResponse(ocspRequest);

            return response()
                    .withStatusCode(200)
                    .withHeader("Content-Type", "application/ocsp-response")
                    .withBody(ocspResponse.getEncoded());

        } catch (Exception e) {
            log.error("Failed to handle OCSP request", e);
            return response().withStatusCode(500);
        }
    }

    byte[] parseOcspRequestBytes(HttpRequest request) throws Exception {
        if ("POST".equals(request.getMethod().getValue())) {
            return request.getBody().getRawBytes();
        } else {
            String path = request.getPath().getValue();
            String encodedReq = path.substring(path.lastIndexOf('/') + 1);
            String urlDecoded = URLDecoder.decode(encodedReq, StandardCharsets.UTF_8);
            return Base64.getDecoder().decode(urlDecoded);
        }
    }

    OCSPResp buildOcspResponse(OCSPReq ocspRequest) throws Exception {
        BasicOCSPRespBuilder respBuilder = createBasicOcspRespBuilder();
        addCertificateResponses(respBuilder, ocspRequest.getRequestList());

        BasicOCSPResp basicResp = signOcspResponse(respBuilder);

        OCSPRespBuilder respGen = new OCSPRespBuilder();
        return respGen.build(OCSPResponseStatus.SUCCESSFUL, basicResp);
    }

    private BasicOCSPRespBuilder createBasicOcspRespBuilder() throws Exception {
        return new BasicOCSPRespBuilder(
                new RespID(new JcaX509CertificateHolder(responderCert).getSubject())
        );
    }

    private void addCertificateResponses(BasicOCSPRespBuilder respBuilder, Req[] requests) {
        for (Req req : requests) {
            CertificateID certID = req.getCertID();
            CertificateStatus status = lookupCertificateStatus(certID);
            respBuilder.addResponse(certID, status);
        }
    }

    private CertificateStatus lookupCertificateStatus(CertificateID certID) {
        CertId lookupKey = new CertId(certID);
        CertificateStatus status = certStatuses.get(lookupKey);

        if (status == null) {
            return new UnknownStatus();
        } else if (status == GOOD_STATUS_MARKER) {
            return CertificateStatus.GOOD;
        }
        return status;
    }

    private BasicOCSPResp signOcspResponse(BasicOCSPRespBuilder respBuilder) throws Exception {
        X509CertificateHolder[] chain = new X509CertificateHolder[]{
                new JcaX509CertificateHolder(responderCert)
        };

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider("BC")
                .build(responderKey);

        return respBuilder.build(signer, chain, new Date());
    }

    private static class CertId {
        private final BigInteger serialNumber;
        private final byte[] issuerNameHash;
        private final byte[] issuerKeyHash;

        CertId(CertificateID certID) {
            this.serialNumber = certID.getSerialNumber();
            this.issuerNameHash = certID.getIssuerNameHash();
            this.issuerKeyHash = certID.getIssuerKeyHash();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CertId)) return false;
            CertId certId = (CertId) o;
            return serialNumber.equals(certId.serialNumber) &&
                    Arrays.equals(issuerNameHash, certId.issuerNameHash) &&
                    Arrays.equals(issuerKeyHash, certId.issuerKeyHash);
        }

        @Override
        public int hashCode() {
            int result = serialNumber.hashCode();
            result = 31 * result + Arrays.hashCode(issuerNameHash);
            result = 31 * result + Arrays.hashCode(issuerKeyHash);
            return result;
        }
    }

    public enum RevocationStatus {
        GOOD, REVOKED, UNKNOWN
    }
}

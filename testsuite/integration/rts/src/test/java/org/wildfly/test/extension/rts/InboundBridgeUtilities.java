package org.wildfly.test.extension.rts;

import static org.wildfly.common.Assert.checkNotNullParamWithNullPointerException;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import javax.json.Json;
import javax.json.JsonArray;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;

import org.jboss.jbossts.star.util.TxLinkNames;
import org.jboss.jbossts.star.util.TxSupport;
import org.junit.Assert;

final class InboundBridgeUtilities {
    private final TxSupport txSupport;
    private final String inboundBridgeResourceUrl;
    private final String loggingRestATParticipantUrl;
    private final String loggingRestATParticipantInvocationsUrl;

    InboundBridgeUtilities(TxSupport txSupport, String inboundBridgeResourceUrl, String loggingRestATParticipantUrl, String loggingRestATParticipantInvocationsUrl) {
        this.txSupport = txSupport;
        this.inboundBridgeResourceUrl = inboundBridgeResourceUrl;
        this.loggingRestATParticipantUrl = loggingRestATParticipantUrl;
        this.loggingRestATParticipantInvocationsUrl = loggingRestATParticipantInvocationsUrl;
    }

    void enlistInboundBridgeResource() {
        final Link participantLink = Link.fromUri(txSupport.getTxnUri()).rel(TxLinkNames.PARTICIPANT).title(TxLinkNames.PARTICIPANT).build();
        final Response response = ClientBuilder.newClient().target(inboundBridgeResourceUrl).request()
                .header("link", participantLink).post(null);

        Assert.assertEquals(200, response.getStatus());
    }

    protected void enlistLoggingRestATParticipant() {
        String linkHeader = txSupport.makeTwoPhaseAwareParticipantLinkHeader(loggingRestATParticipantUrl, false, null, null);
        String recoveryUrl = txSupport.enlistParticipant(txSupport.getTxnUri(), linkHeader);

        Assert.assertFalse(recoveryUrl == null);
    }

    protected void resetInvocations() {
        Response response = ClientBuilder.newClient().target(inboundBridgeResourceUrl).request().put(null);
        Assert.assertEquals(200, response.getStatus());

        response = ClientBuilder.newClient().target(loggingRestATParticipantInvocationsUrl).request().put(null);
        Assert.assertEquals(200, response.getStatus());
    }

    protected JsonArray getInboundBridgeResourceInvocations() throws Exception {
        final String response = ClientBuilder.newClient().target(inboundBridgeResourceUrl).request().get(String.class);

        return createJsonArray(response);
    }

    protected JsonArray getLoggingRestATParticipantInvocations() throws Exception {
        String response = ClientBuilder.newClient().target(loggingRestATParticipantInvocationsUrl).request().get(String.class);
        return createJsonArray(response);
    }

    /**
     * Checking if the parameter <code>recordToAssert</code>
     * is placed exactly once in the {@link JsonArray}.
     */
    protected void assertJsonArray(JsonArray invocationsJSONArray, String recordToAssert, int expectedRecordFoundCount) {
        checkNotNullParamWithNullPointerException("recordToAssert", recordToAssert);
        int recordFoundCount = 0;
        for(int i = 0; i < invocationsJSONArray.size(); i++) {
            if(recordToAssert.equals(invocationsJSONArray.getString(i))) {
                recordFoundCount++;
            }
        }
        if (recordFoundCount != expectedRecordFoundCount) {
            Assert.fail("Invocation result returned as a JSON array '" + invocationsJSONArray + "' "
                    + "expected to contain the record '" + recordToAssert + "' " + expectedRecordFoundCount + " times "
                    + "but the record was " + recordFoundCount + " times in the array");
        }
    }

    private JsonArray createJsonArray(final String response) {
        try (Reader reader = new StringReader(response)) {
            return Json.createReader(reader).readArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

package com.redhat.gss.redhat_support_lib.infrastructure;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;

import com.redhat.gss.redhat_support_lib.errors.RequestException;

public class Telemetries {

    public static final String MACHINE_ID = "machine_id";

    protected Response get(ResteasyClient client, String uri)
            throws RequestException {
        Response response = client.target(uri).request()
                .accept(MediaType.APPLICATION_JSON).get();

        if (response.getStatus() != HttpStatus.SC_OK) {
            throw new RequestException(response.getStatusInfo().getStatusCode()
                    + " - " + response.getStatusInfo().getReasonPhrase());
        }
        return response;
    }

    protected Response addSystem(ResteasyClient client, String uri, String uuid)
            throws RequestException {
        MultipartFormDataOutput mdo = new MultipartFormDataOutput();
        mdo.addFormData("machine_id", uuid,
                    MediaType.APPLICATION_JSON_TYPE);
        Map<String,String> params = new HashMap<String,String>();
        params.put(MACHINE_ID,uuid);
        Response response = (Response) client.target(uri).request()
                .accept(MediaType.APPLICATION_JSON)
                .post(Entity.entity(params, MediaType.APPLICATION_JSON));
        if (response.getStatus() >= HttpStatus.SC_BAD_REQUEST) {
            throw new RequestException(response.getStatusInfo().getStatusCode()
                    + " - " + response.getStatusInfo().getReasonPhrase());
        }
        return response;
    }

    protected Response upload(ResteasyClient client, String uri, File file,
            String description) throws FileNotFoundException, ParseException,
            RequestException {
        MultipartFormDataOutput mdo = new MultipartFormDataOutput();
        if (description != null) {
            mdo.addFormData("description", description,
                    MediaType.APPLICATION_JSON_TYPE);
        }
        mdo.addFormData("file", file, MediaType.APPLICATION_OCTET_STREAM_TYPE,
                file.getName());
        GenericEntity<MultipartFormDataOutput> entity = new GenericEntity<MultipartFormDataOutput>(
                mdo) {
        };

        javax.ws.rs.client.Invocation.Builder builder = client.target(uri)
                .request(MediaType.APPLICATION_JSON);
        if (description != null) {
            builder.header("description", description);
        }
        Response response = builder.post(Entity.entity(entity,
                MediaType.MULTIPART_FORM_DATA_TYPE));

        if (response.getStatus() >= HttpStatus.SC_BAD_REQUEST) {
            throw new RequestException(response.getStatusInfo().getStatusCode()
                    + " - " + response.getStatusInfo().getReasonPhrase());
        }
        return response;
    }
}

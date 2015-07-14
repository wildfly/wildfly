package com.redhat.gss.redhat_support_lib.infrastructure;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;

import com.redhat.gss.redhat_support_lib.web.ConnectionManager;

import com.redhat.gss.redhat_support_lib.errors.RequestException;

public class Insights {

    private ConnectionManager connectionManager = null;

    public static final String MACHINE_ID = "machine_id";
    public static final String HOSTNAME = "hostname";

    public Insights(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public Response get(String uri) throws RequestException,
            MalformedURLException {
        Response response = connectionManager.getConnection().target(new URL(new URL(connectionManager.getConfig().getUrl()),
                uri).toString())
                .request().accept(MediaType.APPLICATION_JSON).get();

        if (response.getStatus() != HttpStatus.SC_OK) {
            throw new RequestException(response.getStatusInfo().getStatusCode()
                    + " - " + response.getStatusInfo().getReasonPhrase());
        }
        return response;
    }

    public Response addSystem(String uri, String uuid, String hostname)
            throws RequestException, MalformedURLException {
        String url = new URL(new URL(connectionManager.getConfig().getUrl()),
                uri).toString();
        Map<String, String> params = new HashMap<String, String>();
        params.put(MACHINE_ID, uuid);
        params.put(HOSTNAME, hostname);
        Response response = (Response) connectionManager.getConnection()
                .target(url).request().accept(MediaType.APPLICATION_JSON)
                .post(Entity.entity(params, MediaType.APPLICATION_JSON));
        if (response.getStatus() >= HttpStatus.SC_BAD_REQUEST) {
            throw new RequestException(response.getStatusInfo().getStatusCode()
                    + " - " + response.getStatusInfo().getReasonPhrase());
        }
        return response;
    }

    public Response updateSystem(String uri, String hostname)
            throws RequestException, MalformedURLException {
        Map<String, String> params = new HashMap<String, String>();
        params.put(HOSTNAME, hostname);
        Response response = (Response) connectionManager.getConnection()
                .target(new URL(new URL(connectionManager.getConfig().getUrl()),
                        uri).toString()).request().accept(MediaType.APPLICATION_JSON)
                .put(Entity.entity(params, MediaType.APPLICATION_JSON));
        if (response.getStatus() >= HttpStatus.SC_BAD_REQUEST) {
            throw new RequestException(response.getStatusInfo().getStatusCode()
                    + " - " + response.getStatusInfo().getReasonPhrase());
        }
        return response;
    }

    public Response upload(String uri, File file, String description)
            throws FileNotFoundException, ParseException, RequestException,
            MalformedURLException {
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

        javax.ws.rs.client.Invocation.Builder builder = connectionManager
                .getConnection().target(new URL(new URL(connectionManager.getConfig().getUrl()),
                        uri).toString())
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

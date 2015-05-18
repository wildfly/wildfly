package com.redhat.gss.redhat_support_lib.infrastructure;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;

import com.redhat.gss.redhat_support_lib.errors.RequestException;
import com.redhat.gss.redhat_support_lib.helpers.FilterHelper;
import com.redhat.gss.redhat_support_lib.helpers.QueryBuilder;
import com.redhat.gss.redhat_support_lib.parsers.CaseType;
import com.redhat.gss.redhat_support_lib.parsers.ValuesType;
import com.redhat.gss.redhat_support_lib.parsers.ValueType;
import com.redhat.gss.redhat_support_lib.web.ConnectionManager;

public class Cases extends BaseQuery {
    private static final Logger LOGGER = Logger
            .getLogger(Cases.class.getName());
    private ConnectionManager connectionManager = null;
    static String url = "/rs/cases/";

    public Cases(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    /**
     * Queries the API for the given case number. RESTful method:
     * https://api.access.redhat.com/rs/cases/<caseNumber>
     *
     * @param caseNum
     *            The exact caseNumber you are interested in.
     * @return A case object that represents the given case number.
     * @throws RequestException
     *             An exception if there was a connection related issue.
     * @throws MalformedURLException
     */
    public CaseType get(String caseNum) throws RequestException,
            MalformedURLException {
        String fullUrl = connectionManager.getConfig().getUrl() + url + caseNum;
        return get(connectionManager.getConnection(), fullUrl, CaseType.class);
    }

    /**
     * Queries the cases RESTful interface with a given set of keywords. RESTful
     * method: https://api.access.redhat.com/rs/cases?keyword=NFS
     *
     * @param keywords
     *            A string array of keywords to search on.
     * @param includeClosed
     *            Do not include closed cases.
     * @param detail
     *            Include additional details.
     * @param group
     *            See https://api.access.redhat.com/rs/groups
     * @param startDate
     *            Must be either: yyyy-MM-ddTHH:mm:ss or yyyy-MM-dd
     * @param endDate
     *            Must be either: yyyy-MM-ddTHH:mm:ss or yyyy-MM-dd
     * @param kwargs
     *            Additional properties to filter on. The RESTful interface can
     *            only search on keywords; however, you can use this method to
     *            post-filter the results returned. Simply supply a String array
     *            of valid properties and their associated values.
     * @return A list of solution objects
     * @throws RequestException
     *             An exception if there was a connection related issue.
     * @throws MalformedURLException
     */

    @SuppressWarnings("unchecked")
    public List<CaseType> list(String[] keywords, boolean includeClosed,
            boolean detail, String group, String startDate, String endDate,
            String count, String start, String[] kwargs)
            throws RequestException, MalformedURLException {

        StringBuilder xmlString = new StringBuilder();
        xmlString
                .append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><caseFilter xmlns=\"http://www.redhat.com/gss/strata\">");
        if (keywords != null) {
            for (String keyword : keywords) {
                xmlString.append("<keyword>");
                xmlString.append(keyword);
                xmlString.append("</keyword>");
            }
        }
        if (includeClosed) {
            xmlString.append("<includeClosed>true</includeClosed>");
        }
        if (group != null) {
            xmlString.append("<groupNumber>");
            xmlString.append(group);
            xmlString.append("</groupNumber>");
        } else {
            xmlString.append("<onlyUngrouped>");
            xmlString.append("true");
            xmlString.append("</onlyUngrouped>");
        }
        if (startDate != null) {
            xmlString.append("<startDate>");
            xmlString.append(startDate);
            xmlString.append("</startDate>");
        }
        if (endDate != null) {
            xmlString.append("<endDate>");
            xmlString.append(endDate);
            xmlString.append("</endDate>");
        }
        if (count != null) {
            xmlString.append("<count>");
            xmlString.append(count);
            xmlString.append("</count>");
        }
        if (start != null) {
            xmlString.append("<start>");
            xmlString.append(start);
            xmlString.append("</start>");
        }

        xmlString.append("<ownerSSOName>");
        xmlString.append(connectionManager.getConfig().getUsername());
        xmlString.append("</ownerSSOName>");

        xmlString.append("</caseFilter>");

        List<String> queryParams = new ArrayList<String>();
        if (detail) {
            queryParams.add("detail=true");
        }

        String fullUrl = QueryBuilder.appendQuery(connectionManager.getConfig()
                .getUrl() + url + "filter", queryParams);
        com.redhat.gss.redhat_support_lib.parsers.CasesType cases = add(
                connectionManager.getConnection(), fullUrl,
                xmlString.toString(),
                com.redhat.gss.redhat_support_lib.parsers.CasesType.class);
        return (List<CaseType>) FilterHelper.filterResults(cases.getCase(),
                kwargs);
    }

    /**
     * Add a new case
     *
     * @param cas
     *            The case to be added. Use InstanceMaker.makeCase to get a case
     *            bean.
     * @return The same case with the case number and view_uri set if
     *         successful.
     * @throws Exception
     *             An exception if there was a connection related issue.
     */
    public CaseType add(CaseType cas) throws Exception {

        String fullUrl = connectionManager.getConfig().getUrl() + url;
        Response resp = add(connectionManager.getConnection(), fullUrl, cas);
        MultivaluedMap<String, String> headers = resp.getStringHeaders();
        URL caseurl = null;
        try {
            caseurl = new URL(headers.getFirst("Location"));
        } catch (MalformedURLException e) {
            LOGGER.debug("Failed : Adding case " + cas.getSummary()
                    + " was unsuccessful.");
            throw new Exception();
        }
        String path = caseurl.getPath();
        cas.setCaseNumber(path.substring(path.lastIndexOf('/') + 1,
                path.length()));
        cas.setViewUri(caseurl.toString());
        return cas;
    }

    /**
     * Update an existing case
     *
     * @param cas
     *            The case to be updated.
     * @return The same case updated with the new case information if
     *         successful.
     * @throws RequestException
     *             An exception if there was a connection related issue.
     * @throws MalformedURLException
     */
    public CaseType update(CaseType cas) throws RequestException,
            MalformedURLException {

        String fullUrl = connectionManager.getConfig().getUrl() + url
                + cas.getCaseNumber();
        update(connectionManager.getConnection(), fullUrl, cas);
        return cas;
    }

    /**
     * Get severities
     *
     * @return The list of severities.
     * @throws RequestException
     *             An exception if there was a connection related issue.
     * @throws MalformedURLException
     */
    public List<ValueType> getSeverities() throws RequestException,
            MalformedURLException {

        String fullUrl = connectionManager.getConfig().getUrl()
                + "/rs/values/case/severity";
        ValuesType values = get(connectionManager.getConnection(), fullUrl,
                ValuesType.class);
        return values.getValue();
    }
}

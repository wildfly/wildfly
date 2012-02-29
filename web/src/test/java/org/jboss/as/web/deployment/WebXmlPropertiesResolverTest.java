/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.web.deployment;

import java.util.ArrayList;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.spec.*;
import org.junit.*;
import static org.junit.Assert.*;

/**
 *
 * @author kulikov
 */
public class WebXmlPropertiesResolverTest {
    private WebMetaData webMetaData;

    public WebXmlPropertiesResolverTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        webMetaData = new WebMetaData();
        System.setProperty("test.varibale", "TEST");
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testServletMapping() {
        ArrayList<String> patterns = new ArrayList();
        patterns.add("${test.varibale}");

        ServletMappingMetaData f = new ServletMappingMetaData();
        f.setServletName("test");
        f.setUrlPatterns(patterns);

        ArrayList<ServletMappingMetaData> map = new ArrayList();
        map.add(f);

        webMetaData.setServletMappings(map);

        WebXmlPropertiesResolver.process(webMetaData);
        assertEquals("TEST", map.get(0).getUrlPatterns().get(0));

    }

    @Test
    public void testWelcomeFileList() {
        ArrayList<String> files = new ArrayList();
        files.add("${test.varibale}");

        WelcomeFileListMetaData wfl = new WelcomeFileListMetaData();
        wfl.setWelcomeFiles(files);

        webMetaData.setWelcomeFileList(wfl);

        WebXmlPropertiesResolver.process(webMetaData);
        assertEquals("TEST", wfl.getWelcomeFiles().get(0));

    }

    @Test
    public void testContextParams() {
        ParamValueMetaData param = new ParamValueMetaData();
        param.setParamName("param.name");
        param.setParamValue("${test.varibale}");

        ArrayList<ParamValueMetaData> params = new ArrayList();
        params.add(param);

        webMetaData.setContextParams(params);

        WebXmlPropertiesResolver.process(webMetaData);
        assertEquals("TEST", param.getParamValue());

    }

    @Test
    public void testFilters() {
        ParamValueMetaData param = new ParamValueMetaData();
        param.setParamName("param.name");
        param.setParamValue("${test.varibale}");

        ArrayList<ParamValueMetaData> params = new ArrayList();
        params.add(param);

        FilterMetaData f = new FilterMetaData();
        f.setFilterName("test");
        f.setFilterClass("com.foo.Bar");
        f.setInitParam(params);

        FiltersMetaData filters = new FiltersMetaData();
        filters.add(f);


        webMetaData.setFilters(filters);

        WebXmlPropertiesResolver.process(webMetaData);
        assertEquals("TEST", param.getParamValue());

    }

    @Test
    public void testFilterMapping() {
        ArrayList<String> patterns = new ArrayList();
        patterns.add("${test.varibale}");

        FilterMappingMetaData f = new FilterMappingMetaData();
        f.setFilterName("test");
        f.setUrlPatterns(patterns);

        ArrayList<FilterMappingMetaData> filters = new ArrayList();
        filters.add(f);

        webMetaData.setFilterMappings(filters);

        WebXmlPropertiesResolver.process(webMetaData);
        assertEquals("TEST", filters.get(0).getUrlPatterns().get(0));

    }

    @Test
    public void testServlet() {
        ParamValueMetaData param = new ParamValueMetaData();
        param.setParamName("param.name");
        param.setParamValue("${test.varibale}");

        ArrayList<ParamValueMetaData> params = new ArrayList();
        params.add(param);

        ServletMetaData servlet = new ServletMetaData();
        servlet.setInitParam(params);
        servlet.setServletName("test");
        servlet.setServletClass("com.foo.Bar");

        ServletsMetaData servlets = new ServletsMetaData();
        servlets.add(servlet);

        webMetaData.setServlets(servlets);

        WebXmlPropertiesResolver.process(webMetaData);
        assertEquals("TEST", param.getParamValue());

    }

    /**
     * Test of process method, of class WebXmlPropertiesResolver.
     */
    @Test
    public void testErrorPage() {
        ArrayList<ErrorPageMetaData> errorPages = new ArrayList();
        ErrorPageMetaData errorPage = new ErrorPageMetaData();
        errorPage.setErrorCode("500");
        errorPage.setExceptionType("Exception type");
        errorPage.setLocation("${test.varibale}");
        errorPages.add(errorPage);

        webMetaData.setErrorPages(errorPages);

        WebXmlPropertiesResolver.process(webMetaData);
        assertEquals("TEST", errorPage.getLocation());

    }
}

package uk.gov.hmcts.reform.em.orchestrator.functional;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.em.orchestrator.service.dto.CcdBoolean;
import uk.gov.hmcts.reform.em.orchestrator.service.dto.CcdBundleDTO;
import uk.gov.hmcts.reform.em.orchestrator.service.dto.CcdValue;
import uk.gov.hmcts.reform.em.orchestrator.testutil.Env;
import uk.gov.hmcts.reform.em.orchestrator.testutil.TestUtil;

import java.io.IOException;

public class CcdStitchScenarios {

    private static final TestUtil testUtil = new TestUtil();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testPostBundleStitch() throws IOException {
        CcdBundleDTO bundle = testUtil.getTestBundle();
        String json = mapper.writeValueAsString(new CcdValue<>(bundle));
        String wrappedJson = String.format("{ \"case_details\":{ \"case_data\":{ \"caseBundles\":[ %s ] } } }", json);

        Response response = testUtil.authRequest()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(wrappedJson)
                .request("POST", Env.getTestUrl() + "/api/stitch-ccd-bundles");

        JsonPath path = response.getBody().jsonPath();
        Assert.assertEquals(200, response.getStatusCode());
        Assert.assertEquals("Bundle title", path.getString("data.caseBundles[0].value.title"));
        Assert.assertNotNull(path.getString("data.caseBundles[0].value.stitchedDocument.document_url"));
    }

    @Test
    public void testPostBundleStitchWithWordDoc() throws IOException {
        CcdBundleDTO bundle = testUtil.getTestBundleWithWordDoc();
        String json = mapper.writeValueAsString(new CcdValue<>(bundle));
        String wrappedJson = String.format("{ \"case_details\":{ \"case_data\":{ \"caseBundles\":[ %s ] } } }", json);

        Response response = testUtil.authRequest()
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .body(wrappedJson)
            .request("POST", Env.getTestUrl() + "/api/stitch-ccd-bundles");

        JsonPath path = response.getBody().jsonPath();
        Assert.assertEquals(200, response.getStatusCode());
        Assert.assertEquals("Bundle title", path.getString("data.caseBundles[0].value.title"));
        Assert.assertNotNull(path.getString("data.caseBundles[0].value.stitchedDocument.document_url"));
    }

    @Test
    public void testSpecificFilename() throws IOException {
        CcdBundleDTO bundle = testUtil.getTestBundle();
        bundle.setFileName("my-file-name.pdf");

        String json = mapper.writeValueAsString(new CcdValue<>(bundle));
        String wrappedJson = String.format("{ \"case_details\":{ \"case_data\":{ \"caseBundles\":[ %s ] } } }", json);

        Response response = testUtil.authRequest()
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .body(wrappedJson)
            .request("POST", Env.getTestUrl() + "/api/stitch-ccd-bundles");

        JsonPath path = response.getBody().jsonPath();
        Assert.assertEquals(200, response.getStatusCode());
        Assert.assertEquals("Bundle title", path.getString("data.caseBundles[0].value.title"));
        Assert.assertEquals("my-file-name.pdf", path.getString("data.caseBundles[0].value.fileName"));
        Assert.assertNotNull(path.getString("data.caseBundles[0].value.stitchedDocument.document_url"));
    }

    @Test
    public void testFilenameErrors() throws IOException {
        CcdBundleDTO bundle = testUtil.getTestBundle();
        bundle.setFileName("1234567890123456789012345678901%.pdf");

        String json = mapper.writeValueAsString(new CcdValue<>(bundle));
        String wrappedJson = String.format("{ \"case_details\":{ \"case_data\":{ \"caseBundles\":[ %s ] } } }", json);

        Response response = testUtil.authRequest()
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .body(wrappedJson)
            .request("POST", Env.getTestUrl() + "/api/stitch-ccd-bundles");

        JsonPath path = response.getBody().jsonPath();
        Assert.assertEquals(200, response.getStatusCode());
        Assert.assertEquals("Bundle title", path.getString("data.caseBundles[0].value.title"));
        Assert.assertEquals("1234567890123456789012345678901%.pdf", path.getString("data.caseBundles[0].value.fileName"));
        Assert.assertNotNull(path.getString("errors[0]"));
        Assert.assertNotNull(path.getString("errors[1]"));
    }

    @Test
    public void testLongBundleDescriptionErrors() throws IOException {
        CcdBundleDTO bundle = testUtil.getTestBundle();

        StringBuilder sample = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            sample.append("y");
        }
        bundle.setDescription(sample.toString());
        String json = mapper.writeValueAsString(new CcdValue<>(bundle));
        String wrappedJson = String.format("{ \"case_details\":{ \"case_data\":{ \"caseBundles\":[ %s ] } } }", json);

        Response response = testUtil.authRequest()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(wrappedJson)
                .request("POST", Env.getTestUrl() + "/api/stitch-ccd-bundles");

        JsonPath path = response.getBody().jsonPath();
        Assert.assertEquals(200, response.getStatusCode());
        Assert.assertNotNull(path.getString("errors[0]"));
    }

    @Test
    public void testWithoutCoversheets() throws IOException {
        CcdBundleDTO bundle = testUtil.getTestBundle();
        bundle.setHasCoversheets(CcdBoolean.No);

        String json = mapper.writeValueAsString(new CcdValue<>(bundle));
        String wrappedJson = String.format("{ \"case_details\":{ \"case_data\":{ \"caseBundles\":[ %s ] } } }", json);

        Response response = testUtil.authRequest()
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .body(wrappedJson)
            .request("POST", Env.getTestUrl() + "/api/stitch-ccd-bundles");

        JsonPath path = response.getBody().jsonPath();
        Assert.assertEquals(200, response.getStatusCode());
        Assert.assertEquals("Bundle title", path.getString("data.caseBundles[0].value.title"));
        Assert.assertEquals("No", path.getString("data.caseBundles[0].value.hasCoversheets"));
        Assert.assertNotNull(path.getString("data.caseBundles[0].value.stitchedDocument.document_url"));
    }
}

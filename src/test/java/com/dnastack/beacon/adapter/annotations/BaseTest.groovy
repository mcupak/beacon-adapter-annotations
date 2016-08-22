package com.dnastack.beacon.adapter.annotations

import com.dnastack.beacon.adapter.annotations.utils.Json
import com.dnastack.beacon.adater.annotations.AnnotationsBeaconAdapter
import com.dnastack.beacon.utils.AdapterConfig
import com.dnastack.beacon.utils.ConfigValue
import com.github.tomakehurst.wiremock.WireMockServer
import ga4gh.Variants
import org.apache.commons.lang.StringUtils
import org.testng.annotations.AfterMethod
import org.testng.annotations.AfterSuite
import org.testng.annotations.BeforeSuite
import org.testng.annotations.Test

import static com.dnastack.beacon.adapter.annotations.TestData.*
import static com.dnastack.beacon.adater.annotations.client.ga4gh.retro.Ga4ghRetroService.*
import static com.github.tomakehurst.wiremock.client.WireMock.*
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig

/**
 * Helper class for variants adapter tests.
 * </p>
 * When the java property ga4gh.testServer.url is not specified, the Ga4gh server is mocked, otherwise tests
 * are run against the specified one.
 * </p>
 * Not all tests might support integration testing against a real Beacon server - this is defined by
 * {@link com.dnastack.beacon.adapter.annotations.BaseTest#isIntegrationTestingSupported()}
 * </p>
 * For integration tests against a real server to work properly, the test data should equal to the real data. Currently,
 * it doesn't, anyway the tests infrastructure allows integration tests per se (may be added in the future).
 *
 * @author Artem (tema.voskoboynick@gmail.com)
 * @version 1.0
 */
public abstract class BaseTest {
    static final def MOCK_GA4GH_PORT = 8089
    static final def MOCK_GA4GH_SERVER = new WireMockServer(wireMockConfig().port(MOCK_GA4GH_PORT))
    static final AnnotationsBeaconAdapter ADAPTER
    static final boolean MOCKED_TESTING

/**
 * Define if the testing will be against a real Ga4gh server, or the mocked one.
 */
    static {
        def ga4ghTestUrl = System.properties.getProperty("ga4gh.testServer.url")
        MOCKED_TESTING = StringUtils.isBlank(ga4ghTestUrl)

        // Adapter initialization.

        ADAPTER = new AnnotationsBeaconAdapter()

        def configValues = MOCKED_TESTING ?
                [new ConfigValue("variants-url", "http://localhost:$MOCK_GA4GH_PORT/")] :
                []
        def adapterConfig = new AdapterConfig("Annotations Test Adapter", AnnotationsBeaconAdapter.getName(), configValues)
        ADAPTER.initAdapter(adapterConfig)
    }

    @BeforeSuite
    void startServer() {
        if (MOCKED_TESTING) {
            MOCK_GA4GH_SERVER.start();
        }
    }

    @AfterSuite
    void stopServer() {
        if (MOCKED_TESTING) {
            MOCK_GA4GH_SERVER.stop();
        }
    }

    @AfterMethod
    void resetMappings() {
        if (MOCKED_TESTING) {
            MOCK_GA4GH_SERVER.resetMappings();
        }
    }

    @Test
    void test() {
        if (!MOCKED_TESTING && !isIntegrationTestingSupported()) {
            return
        }

        if (MOCKED_TESTING) {
            setupMappings()
        }

        doTest()
    }

    void setupMappings() {
        setupGeneralMappings()
    }

    boolean isIntegrationTestingSupported() { return true }

    abstract void doTest();

    /**
     * Setups up the mappings usually shared by all tests.
     */
    protected setupGeneralMappings() {
        setupSearchVariantsMapping()
        setupGetVariantAnnotationSetMapping()
        setupGetReferenceSetMapping()
        setupGetVariantSetMapping()
        setupGetCallSetMapping()
    }

    private setupSearchVariantsMapping() {
        MOCK_GA4GH_SERVER.stubFor(post(urlEqualTo("/$VARIANTS_SEARCH_PATH"))
                .withRequestBody(equalToJson(Json.toJson(SEARCH_VARIANTS_TEST_REQUEST)))

                .willReturn(aResponse()
                .withBody(Json.toJson(SEARCH_VARIANTS_TEST_RESPONSE))))
    }

    private setupGetReferenceSetMapping() {
        MOCK_GA4GH_SERVER.stubFor(get(urlEqualTo("/$REFERENCE_SETS_GET_PATH/$TEST_REFERENCE_SET.id"))

                .willReturn(aResponse()
                .withBody(Json.toJson(TEST_REFERENCE_SET))))
    }

    private setupGetVariantAnnotationSetMapping() {
        MOCK_GA4GH_SERVER.stubFor(get(urlEqualTo("/$VARIANT_ANNOTATIONS_SETS_GET_PATH/$TEST_VARIANT_ANNOTATION_SET.id"))

                .willReturn(aResponse()
                .withBody(Json.toJson(TEST_VARIANT_ANNOTATION_SET))))
    }

    private setupGetVariantSetMapping() {
        MOCK_GA4GH_SERVER.stubFor(get(urlEqualTo("/$VARIANT_SETS_GET_PATH/$TEST_VARIANT_SET.id"))

                .willReturn(aResponse()
                .withBody(Json.toJson(TEST_VARIANT_SET))))
    }

    private setupGetCallSetMapping() {
        setupGetCallSetMapping(TEST_CALL_SET_1)
        setupGetCallSetMapping(TEST_CALL_SET_2)
    }

    private setupGetCallSetMapping(Variants.CallSet callSet) {
        MOCK_GA4GH_SERVER.stubFor(get(urlEqualTo("/$CALL_SETS_GET_PATH/$callSet.id"))

                .willReturn(aResponse()
                .withBody(Json.toJson(callSet))))
    }
}
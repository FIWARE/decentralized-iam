package org.fiware.decentralizediam.it.components;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.http.HttpStatus;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.fiware.decentralizediam.it.components.model.OpenIdConfiguration;
import org.fiware.decentralizediam.it.components.model.Policy;
import org.keycloak.common.crypto.CryptoIntegration;

import java.io.IOException;
import java.io.InputStream;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.fiware.decentralizediam.it.components.OrganizationEnvironment.OPERATOR_USER_NAME;
import static org.fiware.decentralizediam.it.components.OrganizationEnvironment.EMPLOYEE_USER_NAME;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 * @author <a href="https://github.com/vramperez">Victor Ramperez</a>
 */
@Slf4j
public class StepDefinitions {

    private static final OkHttpClient HTTP_CLIENT = TestUtils.OK_HTTP_CLIENT;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String OPERATOR_CREDENTIAL = "operator-credential";
    private static final String DEFAULT_SCOPE = "default";
    private static final String GRANT_TYPE_VP_TOKEN = "vp_token";
    private static final String TIL_DIRECT_ADDRESS = "http://tir.127.0.0.1.nip.io:8080";
    private static final String DID_ADDRESS = "http://did-helper.127.0.0.1.nip.io:8080";
    private static final String VERIFIER_ADDRESS = "http://verifier.127.0.0.1.nip.io:8080";
    private static final String RESPONSE_TYPE_DIRECT_POST = "direct_post";
    private static final String ENTITY_ID = "urn:ngsi-ld:EnergyReport:fms-1";
    private static final Map offerEntity = Map.of("type", "EnergyReport",
            "id", ENTITY_ID,
            "name", Map.of("type", "Property", "value", "Standard Server"),
            "consumption", Map.of("type", "Property", "value", "94"));

    private Wallet userWallet;
    private List<String> createdEntities = new ArrayList<>();
    private List<String> createdPolicies = new ArrayList<>();
    private String accessToken;

    @Before
    public void setup() throws Exception {
        CryptoIntegration.init(this.getClass().getClassLoader());
        Security.addProvider(new BouncyCastleProvider());
        userWallet = new Wallet();
        accessToken = null;
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        clean();
    }

    @After
    public void cleanUp() throws Exception {
        clean();
    }

    public void clean() throws Exception {
        cleanUpPolicies();
        cleanUpEntities();
    }

    private void cleanUpPolicies() throws Exception {
        Request getPolicies = new Request.Builder()
                .url(DataServiceEnvironment.PAP_ADDRESS + "/policy")
                .get().build();
        Response policyResponse = HTTP_CLIENT.newCall(getPolicies).execute();

        List<Policy> policies = OBJECT_MAPPER.readValue(policyResponse.body().string(), new TypeReference<List<Policy>>() {
        });

        policies.forEach(policyId -> {
            Request deletionRequest = new Request.Builder()
                    .url(DataServiceEnvironment.PAP_ADDRESS + "/policy/" + policyId.getId())
                    .delete()
                    .build();
            try {
                Response r = HTTP_CLIENT.newCall(deletionRequest).execute();
                String message = r.body().string();
                r.body().close();
                log.warn(message);
            } catch (IOException e) {
                // just log
                log.warn("Was not able to clean up policy {}.", policyId);
            }
        });
    }

    private void cleanUpEntities() {
        createdEntities.forEach(entityId -> {
            Request deletionRequest = new Request.Builder()
                    .url(DataServiceEnvironment.ORION_ADDRESS + "/ngsi-ld/v1/entities/" + entityId)
                    .delete()
                    .build();
            try {
                HTTP_CLIENT.newCall(deletionRequest).execute();
            } catch (IOException e) {
                // just log
                log.warn("Was not able to clean up entitiy {}.", entityId);
            }
        });
    }

    @Given("organization is registered in the trusted issuer list.")
    public void checkOrganizationRegistered() throws Exception {
        Request didCheckRequest = new Request.Builder()
                .url(TIL_DIRECT_ADDRESS + "/v4/issuers/" + TestUtils.getDid(DID_ADDRESS))
                .build();
        Response tirResponse = HTTP_CLIENT.newCall(didCheckRequest).execute();
        assertEquals(HttpStatus.SC_OK, tirResponse.code(), "The did should be registered at the trusted-issuer-list.");
        tirResponse.body().close();
    }

    @When("a policy to allow users with OPERATOR role to create energy reports is registered.")
    public void registerEnergyReportPolicy() throws Exception {
        RequestBody policyBody = RequestBody.create(getPolicy("energyReport"), okhttp3.MediaType.parse(MediaType.APPLICATION_JSON));
        Request policyCreationRequest = new Request.Builder()
                .post(policyBody)
                .url(DataServiceEnvironment.PAP_ADDRESS + "/policy")
                .build();
        Response policyCreationResponse = HTTP_CLIENT.newCall(policyCreationRequest).execute();
        assertEquals(HttpStatus.SC_OK, policyCreationResponse.code(), "The policy should have been created.");
        policyCreationResponse.body().close();
        createdPolicies.add(policyCreationResponse.header("Location"));

        Thread.sleep(3000); // Wait for the policy to be fetched by OPA
    }

    @When("organization issues a credential of type operator credential with OPERATOR role to its operator.")
    public void issueOperatorCredentialToEmployee() throws Exception {
        String accessToken = OrganizationEnvironment.loginToConsumerKeycloak(OPERATOR_USER_NAME);
        userWallet.getCredentialFromIssuer(accessToken, OrganizationEnvironment.CONSUMER_KEYCLOAK_ADDRESS, OPERATOR_CREDENTIAL);
    }

    @When("organization issues a credential of type operator credential without OPERATOR role to its employee.")
    public void issueUserCredentialToEmployee() throws Exception {
        String accessToken = OrganizationEnvironment.loginToConsumerKeycloak(EMPLOYEE_USER_NAME);
        userWallet.getCredentialFromIssuer(accessToken, OrganizationEnvironment.CONSUMER_KEYCLOAK_ADDRESS, OPERATOR_CREDENTIAL);
    }

    @When("a valid access token with the operator credential is retrieved.")
    public void issueOperatorAccessToken() throws Exception {
        accessToken = getOperatorAccessToken();
    }

    @Then("employee can create a new energy report.")
    public void createEnergyReport() throws Exception {

        Response creationResponse = createEnergyReportEntity(accessToken);

        assertEquals(HttpStatus.SC_CREATED, creationResponse.code(), "The entity should have been created.");
        creationResponse.body().close();
        createdEntities.add(ENTITY_ID);
    }

    @Then("employee should not be able to create a new energy report.")
    public void denyEnergyReportCreation() throws Exception {

        Response creationResponse = createEnergyReportEntity(accessToken);

        assertEquals(HttpStatus.SC_FORBIDDEN, creationResponse.code(), "The entity should not have been created.");
        creationResponse.body().close();
    }

    private Response createEnergyReportEntity(String token) throws Exception {
        RequestBody requestBody = RequestBody.create(OBJECT_MAPPER.writeValueAsString(StepDefinitions.offerEntity), okhttp3.MediaType.parse(MediaType.APPLICATION_JSON));

        Request creationRequest = new Request.Builder()
                .url(DataServiceEnvironment.DATA_SERVICE_ADDRESS + "/ngsi-ld/v1/entities")
                .header("AUTHORIZATION", "Bearer " + token)
                .post(requestBody)
                .build();
        return  HTTP_CLIENT.newCall(creationRequest).execute();
    }

    private String getPolicy(String policyName) throws IOException {
        InputStream policyInputStream = this.getClass().getResourceAsStream(String.format("/policies/%s.json", policyName));
        StringBuilder sb = new StringBuilder();
        for (int ch; (ch = policyInputStream.read()) != -1; ) {
            sb.append((char) ch);
        }
        return sb.toString();
    }

    private String getOperatorAccessToken() throws Exception {
        OpenIdConfiguration openIdConfiguration = OrganizationEnvironment.getOpenIDConfiguration(StepDefinitions.VERIFIER_ADDRESS);
        assertTrue(openIdConfiguration.getGrantTypesSupported().contains(GRANT_TYPE_VP_TOKEN), "The organization environment should support vp_tokens");
        assertTrue(openIdConfiguration.getResponseModeSupported().contains(RESPONSE_TYPE_DIRECT_POST), "The organization environment should support direct_post");
        assertNotNull(openIdConfiguration.getTokenEndpoint(), "The organization environment should provide a token endpoint.");

        return userWallet.exchangeCredentialForToken(openIdConfiguration, StepDefinitions.OPERATOR_CREDENTIAL, StepDefinitions.DEFAULT_SCOPE);
    }
}
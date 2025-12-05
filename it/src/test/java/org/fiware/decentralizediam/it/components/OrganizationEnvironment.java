package org.fiware.decentralizediam.it.components;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.http.HttpStatus;
import org.fiware.decentralizediam.it.components.model.OpenIdConfiguration;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 * @author <a href="https://github.com/vramperez">Victor Ramperez</a>
 */
public abstract class OrganizationEnvironment {
    public static final String DID_ADDRESS = "http://did-helper.127.0.0.1.nip.io:8080";
    public static final String CONSUMER_KEYCLOAK_ADDRESS = "http://keycloak.127.0.0.1.nip.io:8080";

    private static final String TEST_REALM = "test-realm";
    public static final String EMPLOYEE_USER_NAME = "employee@consumer.org";
    public static final String OPERATOR_USER_NAME = "operator@consumer.org";
    private static final String TEST_USER_PASSWORD = "test";
    public static final String OIDC_WELL_KNOWN_PATH = "/services/data-service/.well-known/openid-configuration";
    private static final OkHttpClient HTTP_CLIENT = TestUtils.OK_HTTP_CLIENT;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Returns an access token to be used with Keycloak.
     */
    public static String loginToConsumerKeycloak(String user) throws NoSuchAlgorithmException, KeyManagementException {
        KeycloakHelper consumerKeycloak = new KeycloakHelper(TEST_REALM, CONSUMER_KEYCLOAK_ADDRESS);
        return consumerKeycloak.getUserToken(user, TEST_USER_PASSWORD);
    }

    public static OpenIdConfiguration getOpenIDConfiguration(String targetHost) throws Exception {
        Request wellKnownRequest = new Request.Builder().get()
                .url(targetHost + OIDC_WELL_KNOWN_PATH)
                .build();
        Response wellKnownResponse = HTTP_CLIENT.newCall(wellKnownRequest).execute();
        assertEquals(HttpStatus.SC_OK, wellKnownResponse.code(), "The oidc config should have been returned.");
        OpenIdConfiguration openIdConfiguration = OBJECT_MAPPER.readValue(wellKnownResponse.body().string(), OpenIdConfiguration.class);
        wellKnownResponse.body().close();
        return openIdConfiguration;
    }
}

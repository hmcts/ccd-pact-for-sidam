package uk.gov.hmcts.sidam;

import java.util.HashMap;
import java.util.Map;

import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslJsonRootValue;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.model.RequestResponsePact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(PactConsumerTestExt.class)
@ExtendWith(SpringExtension.class)
@PactTestFor(providerName = "SIDAMService", port = "8888")
@SpringBootTest({
    // overriding provider address
    "SIDAMService.ribbon.listOfServers: localhost:8888"
})
public class SIDAMPACTTest {
    @Autowired
    private SIDAMApi sidamApi;

    @Pact(state = "get user details from idam", provider = "sidam_user_details", consumer = "ccd_data_store")
    RequestResponsePact getDetailsPact(PactDslWithProvider builder) {
        Map<String, String> headers = new HashMap<>();
        headers.put("access_token", "some-access-token");
        // @formatter:off
        return builder
            .given("provider returns a SIDAM user")
            .uponReceiving("a request for a token")
            .path("/details")
            .method("GET")
            .headers(headers)
            .willRespondWith()
            .status(201)
            .matchHeader("Content-Type", "application/json")
            .body(new PactDslJsonBody()
                .integerType("id", 42)
                .minArrayLike("roles", 1, PactDslJsonRootValue.
                    stringMatcher("TESTER|DEVELOPER|SOLICITOR", "SOLICITOR")))
            .toPact();
        // @formatter:on
    }


    @Pact(state = "Authorise user", provider = "sidam_authorise_user_endpoint", consumer = "ccd_data_store")
    RequestResponsePact authorizeUser(PactDslWithProvider builder) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "some-access-token");
        // @formatter:off
        return builder
            .given("provider returns a SIDAM user")
            .uponReceiving("a request for a token")
            .path("/oauth2/authorize")
            .method("POST")
            .headers(headers)
            .query("client_id=ID10001")
            .willRespondWith()
            .status(201)
            .body(new PactDslJsonBody()
                .stringType("code", "42"))
            .toPact();
    }

    @Pact(state = "get token", provider = "sidam_token", consumer = "ccd_data_store")
    RequestResponsePact getToken(PactDslWithProvider builder) {
        // @formatter:off
        return builder
            .given("provider returns a SIDAM user")
            .uponReceiving("a request for a token")
            .path("/oauth2/token")
            .method("POST")
            .query("code=42&grant_type=grantType&redirect_uri=http://localhost:3501/case-management")
            .willRespondWith()
            .status(201)
            .body(new PactDslJsonBody()
                .stringType("access_token", "some-access-toke"))
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "authorizeUser")
    void verifyAuthorizeUser() {
        SIDAMApi.AuthenticateUserResponse user = sidamApi.authorizeUser("some-access-token",
            null,
            "ID10001",
            null);
        assertThat(user.getCode()).isEqualTo("42");
    }

    @Test
    @PactTestFor(pactMethod = "getDetailsPact")
    void verifyIDAMUser() {
        SIDAMApi.IdamUser user = sidamApi.getUser("some-access-token");
        assertThat(user.getId()).isEqualTo("42");
    }

    @Test
    @PactTestFor(pactMethod = "getToken")
    void verifyITokenEnpoint() {
        SIDAMApi.TokenExchangeResponse token = sidamApi.token("42",
            "grantType",
            "http://localhost:3501/case-management", null, null);
        assertThat(token.getAccessToken()).isEqualTo("some-access-toke");
    }

}

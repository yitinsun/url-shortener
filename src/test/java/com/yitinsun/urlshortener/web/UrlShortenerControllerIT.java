package com.yitinsun.urlshortener.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yitinsun.urlshortener.domain.UrlMapping;
import com.yitinsun.urlshortener.repo.UrlMappingRepository;
import com.yitinsun.urlshortener.web.dto.ShortenRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasLength;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UrlShortenerControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UrlMappingRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    void shortenValidUrlReturns201WithPayload() throws Exception {
        mockMvc.perform(post("/api/v1/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ShortenRequest("https://example.com/system-design", 365))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.longUrl").value("https://example.com/system-design"))
                .andExpect(jsonPath("$.shortUrl").value(org.hamcrest.Matchers.startsWith("http://localhost:8080/short/")))
                .andExpect(jsonPath("$.shortCode").value(hasLength(5)))
                .andExpect(jsonPath("$.expiresAt").value(not(emptyOrNullString())));
    }

    @Test
    void secondPostOfSameLongUrlReturnsSameShortCode() throws Exception {
        String body = objectMapper.writeValueAsString(new ShortenRequest("https://example.com/same", null));
        MvcResult first = mockMvc.perform(post("/api/v1/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        String firstCode = objectMapper.readTree(first.getResponse().getContentAsString()).get("shortCode").asText();

        mockMvc.perform(post("/api/v1/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").value(firstCode));
    }

    @Test
    void htmlMarkupInLongUrlReturns400Json() throws Exception {
        mockMvc.perform(post("/api/v1/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"longUrl\":\"https://example.com/<b>xss</b>\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(containsString("HTML")));
    }

    @Test
    void malformedUrlReturns400ErrorJson() throws Exception {
        mockMvc.perform(post("/api/v1/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"longUrl\":\"not-a-valid-url\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(not(emptyOrNullString())));
    }

    @Test
    void selfLoopReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ShortenRequest("http://localhost:8080/loop", 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getActiveMappingReturns302WithLocation() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ShortenRequest("https://example.com/dest", 10))))
                .andExpect(status().isCreated())
                .andReturn();
        String code = objectMapper.readTree(created.getResponse().getContentAsString()).get("shortCode").asText();

        mockMvc.perform(get("/short/" + code))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/dest"));
    }

    @Test
    void unknownCodeReturnsFriendlyHtml() throws Exception {
        mockMvc.perform(get("/short/zzzzz"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(containsString("We could not find that page")))
                .andExpect(content().string(not(containsString("Whitelabel"))));
    }

    @Test
    void oldRootShortCodePathIsIllegalHtml() throws Exception {
        mockMvc.perform(get("/b9K2x1"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(containsString("We could not find that page")));
    }

    @Test
    void unknownAndRootPathsReturnFriendlyHtml() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("We could not find that page")));
        mockMvc.perform(get("/nope"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("We could not find that page")));
    }

    @Test
    void getShortenIsIllegalHtml() throws Exception {
        mockMvc.perform(get("/api/v1/shorten"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @Test
    void openApiDocsArePublished() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").value(org.hamcrest.Matchers.startsWith("3.")))
                .andExpect(jsonPath("$.info.title").value("URL Shortener API"))
                .andExpect(jsonPath("$.paths['/api/v1/shorten']").exists())
                .andExpect(jsonPath("$.paths['/short/{shortCode}']").doesNotExist());
    }

    @Test
    void swaggerUiAndActuatorAreReachable() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/actuator"))
                .andExpect(status().isOk());
    }

    @Test
    void expiredMappingReturns404() throws Exception {
        UrlMapping expired = new UrlMapping();
        expired.setLongUrl("https://example.com/old");
        expired.setShortCode("exp01");
        expired.setCreatedAt(Instant.now().minus(10, ChronoUnit.DAYS));
        expired.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
        repository.save(expired);

        mockMvc.perform(get("/short/exp01"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(containsString("We could not find that page")));
    }
}

package fi.helsinki.moodi.config;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import fi.helsinki.moodi.integration.moodle.MoodleClient;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

@Configuration
public class MoodleConfig {

    @Autowired
    private Environment environment;

    @Bean
    public MoodleClient moodleClient() {
        final ObjectMapper objectMapper = objectMapper();
        return new MoodleClient(baseUrl(), wstoken(), objectMapper, moodleRestTemplate());
    }

    @Bean
    public RestTemplate moodleRestTemplate() {
        final HttpClient httpClient = HttpClientBuilder.create().build();
        final ClientHttpRequestFactory requestFactory =
                new BufferingClientHttpRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));

        final RestTemplate restTemplate = new RestTemplate(requestFactory);

        restTemplate.setMessageConverters(
                Lists.newArrayList(
//                        new MappingJackson2HttpMessageConverter(objectMapper()),
                        new StringHttpMessageConverter(),
                        new FormHttpMessageConverter()));

        return restTemplate;
    }

    private String baseUrl() {
        return environment.getRequiredProperty("integration.moodle.url");
    }

    private String wstoken() {
        return environment.getRequiredProperty("integration.moodle.wstoken");
    }

    private ObjectMapper objectMapper() {
        return new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}

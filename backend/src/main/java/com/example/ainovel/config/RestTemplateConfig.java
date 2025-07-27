package com.example.ainovel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;

/**
 * Configuration for RestTemplate, including optional proxy settings.
 */
@Configuration
public class RestTemplateConfig {

    @Value("${proxy.enabled:false}")
    private boolean proxyEnabled;

    @Value("${proxy.host:127.0.0.1}")
    private String proxyHost;

    @Value("${proxy.port:7890}")
    private int proxyPort;

    /**
     * Creates a RestTemplate bean. If proxy is enabled in the application properties,
     * it will be configured to use the specified SOCKS proxy.
     *
     * @return A configured RestTemplate instance.
     */
    @Bean
    public RestTemplate restTemplate() {
        if (proxyEnabled) {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(proxyHost, proxyPort));
            requestFactory.setProxy(proxy);
            return new RestTemplate(requestFactory);
        }
        return new RestTemplate();
    }
}

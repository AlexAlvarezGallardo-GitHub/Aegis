package com.aegis.bff;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aegis")
public class BffProperties {

    private ServiceUrl identityService = new ServiceUrl();
    private ServiceUrl walletService = new ServiceUrl();

    public ServiceUrl getIdentityService() { return identityService; }
    public void setIdentityService(ServiceUrl identityService) { this.identityService = identityService; }

    public ServiceUrl getWalletService() { return walletService; }
    public void setWalletService(ServiceUrl walletService) { this.walletService = walletService; }

    public static class ServiceUrl {
        private String url;

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }
}

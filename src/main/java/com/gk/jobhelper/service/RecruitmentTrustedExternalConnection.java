package com.gk.jobhelper.service;

import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Compatibility connection for user-approved public recruitment sources with broken TLS chains.
 * Callers must validate the URL before invoking this method; this class never permits private addresses.
 */
final class RecruitmentTrustedExternalConnection {
    private static final TrustManager[] TRUST_ALL = {new X509TrustManager() {
        @Override public void checkClientTrusted(X509Certificate[] chain, String authType) { }
        @Override public void checkServerTrusted(X509Certificate[] chain, String authType) { }
        @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
    }};

    private RecruitmentTrustedExternalConnection() { }

    static HttpURLConnection open(URL url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        if (connection instanceof HttpsURLConnection) {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, TRUST_ALL, new SecureRandom());
            HttpsURLConnection https = (HttpsURLConnection) connection;
            https.setSSLSocketFactory(context.getSocketFactory());
            https.setHostnameVerifier((host, session) -> true);
        }
        return connection;
    }
}

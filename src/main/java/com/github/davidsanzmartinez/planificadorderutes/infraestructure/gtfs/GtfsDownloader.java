package com.github.davidsanzmartinez.planificadorderutes.infraestructure.gtfs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

@Component
@Slf4j
public class GtfsDownloader {

    private final RestClient restClient;

    public GtfsDownloader() throws Exception {
        // Carga el certificado de data.renfe.com desde resources
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate dataRenfeCert;
        try (InputStream is = getClass().getResourceAsStream("/certs/data-renfe-com.pem")) {
            if (is == null) {
                throw new IllegalStateException("Certificate not found at /certs/data-renfe-com.pem");
            }
            dataRenfeCert = cf.generateCertificate(is);
        }

        // Crea un truststore en memoria con el certificado
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("data.renfe.com", dataRenfeCert);

        // Combina con el truststore por defecto del JRE (para otros dominios)
        TrustManagerFactory defaultTmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        defaultTmf.init((KeyStore) null);

        TrustManagerFactory customTmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        customTmf.init(trustStore);

        // TrustManager que prueba primero los del JRE, luego el nuestro
        X509TrustManager defaultTm = (X509TrustManager) defaultTmf.getTrustManagers()[0];
        X509TrustManager customTm = (X509TrustManager) customTmf.getTrustManagers()[0];

        X509TrustManager combinedTm = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException {
                defaultTm.checkClientTrusted(chain, authType);
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException {
                try {
                    defaultTm.checkServerTrusted(chain, authType);
                } catch (CertificateException e) {
                    customTm.checkServerTrusted(chain, authType);
                }
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return defaultTm.getAcceptedIssuers();
            }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{combinedTm}, new SecureRandom());

        HttpClient httpClient = HttpClient.newBuilder()
                .sslContext(sslContext)
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
        this.restClient = RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();
    }

    public Path downloadAndExtract(String url, Path targetDirectory) throws IOException {
        log.info("Downloading GTFS from {}", url);

        byte[] zipBytes = restClient.get()
                .uri(url)  // ya tiene https://
                .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36")
                .header("Accept", "application/zip, application/octet-stream, */*")
                .retrieve()
                .body(byte[].class);

        if (zipBytes == null || zipBytes.length == 0) {
            throw new IOException("Downloaded file is empty from " + url);
        }

        log.info("Downloaded {} bytes, extracting...", zipBytes.length);

        Files.createDirectories(targetDirectory);

        try (ZipInputStream zis = new ZipInputStream(
                new java.io.ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().endsWith(".txt")) {
                    Path targetFile = targetDirectory.resolve(entry.getName());
                    Files.copy(zis, targetFile,
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    log.info("Extracted: {}", entry.getName());
                }
                zis.closeEntry();
            }
        }

        log.info("Extraction completed to {}", targetDirectory);
        return targetDirectory;
    }
}
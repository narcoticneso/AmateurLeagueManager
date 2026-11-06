package com.amateurleague;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;

public class OpenApiExport {

    public static void main(String[] args) throws Exception {
        try (var context = new SpringApplicationBuilder(LeagueApplication.class).run("--server.port=0", "--spring.datasource.url=jdbc:h2:mem:openapi", "--spring.main.banner-mode=off", "--logging.level.root=warn")) {
            int port = ((WebServerApplicationContext) context).getWebServer().getPort();
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/v3/api-docs")).build();
            Files.writeString(Path.of(args[0]), HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).body());
        }
    }
}

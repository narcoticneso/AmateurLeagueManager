package com.amateurleague;

import jakarta.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class FrontendDevServer {

    private static final Logger log = LoggerFactory.getLogger(FrontendDevServer.class);
    private Process vite;

    @EventListener
    public void start(WebServerInitializedEvent event) throws IOException {
        Path frontend = Path.of("frontend").toAbsolutePath();
        Path nodeDir = frontend.resolve("node");
        Path node = nodeDir.resolve(File.separatorChar == '\\' ? "node.exe" : "node");
        Path npm = nodeDir.resolve("node_modules/npm/bin/npm-cli.js");
        if (!Files.exists(node) || !Files.exists(npm)) {
            throw new IllegalStateException("frontend/node is missing. Start the app with the Maven wrapper.");
        }
        var builder = new ProcessBuilder(node.toString(), npm.toString(), "run", "dev").directory(frontend.toFile()).inheritIO();
        var env = builder.environment();
        String pathKey = env
            .keySet()
            .stream()
            .filter(key -> key.equalsIgnoreCase("PATH"))
            .findFirst()
            .orElse("PATH");
        env.put(pathKey, nodeDir + File.pathSeparator + env.getOrDefault(pathKey, ""));
        env.put("PORT", String.valueOf(event.getWebServer().getPort()));
        vite = builder.start();
        log.info("Started Vite. Open the Local address it prints.");
    }

    @PreDestroy
    public void stop() {
        if (vite == null) {
            return;
        }
        vite.descendants().forEach(ProcessHandle::destroy);
        vite.destroy();
    }
}

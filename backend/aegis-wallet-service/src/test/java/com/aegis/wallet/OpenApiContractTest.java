package com.aegis.wallet;

import com.aegis.wallet.web.controller.WalletController;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that every path and HTTP method declared in the Wallet service OpenAPI
 * contracts (specs/003-create-wallet, specs/004-deposit-funds and
 * specs/009-manage-wallet) is implemented by {@link WalletController}.
 */
class OpenApiContractTest {

    private static final String[] CONTRACTS = {
            "../../specs/003-create-wallet/contracts/wallet-api.yaml",
            "../../specs/004-deposit-funds/contracts/deposit-api.yaml",
            "../../specs/009-manage-wallet/contracts/manage-wallet-api.yaml"
    };

    private static final Map<String, String> MAPPING_ANNOTATIONS = Map.of(
            GetMapping.class.getName(), "GET",
            PostMapping.class.getName(), "POST",
            PutMapping.class.getName(), "PUT",
            PatchMapping.class.getName(), "PATCH",
            DeleteMapping.class.getName(), "DELETE"
    );

    private record Endpoint(String path, String method) {
    }

    @Test
    void contractEndpointsAreImplementedByControllers() throws Exception {
        Set<Endpoint> contractEndpoints = loadContractEndpoints(CONTRACTS);
        assertTrue(contractEndpoints.size() >= 5, "expected the create, deposit and manage endpoints");

        Set<Endpoint> controllerEndpoints = collectControllerEndpoints(WalletController.class);

        List<String> missing = new ArrayList<>();
        for (Endpoint endpoint : contractEndpoints) {
            if (!controllerEndpoints.contains(endpoint)) {
                missing.add(endpoint.method() + " " + endpoint.path());
            }
        }

        assertTrue(missing.isEmpty(),
                "Contract endpoints not implemented by any controller: " + String.join(", ", missing));
    }

    private Set<Endpoint> loadContractEndpoints(String... contractPaths) throws Exception {
        Set<Endpoint> endpoints = new LinkedHashSet<>();
        ObjectMapper yaml = new ObjectMapper(new YAMLFactory());
        for (String contractPath : contractPaths) {
            Path file = resolveRepoPath(contractPath);
            assertTrue(Files.exists(file), "contract file not found: " + file.toAbsolutePath());

            Map<?, ?> root = yaml.readValue(file.toFile(), Map.class);
            Map<?, ?> paths = (Map<?, ?>) root.get("paths");
            for (Map.Entry<?, ?> pathEntry : paths.entrySet()) {
                String path = normalizePath(pathEntry.getKey().toString());
                Map<?, ?> operations = (Map<?, ?>) pathEntry.getValue();
                for (Object operation : operations.keySet()) {
                    String method = operation.toString().toUpperCase();
                    if (isHttpMethod(method)) {
                        endpoints.add(new Endpoint(path, method));
                    }
                }
            }
        }
        return endpoints;
    }

    private Set<Endpoint> collectControllerEndpoints(Class<?>... controllers) {
        Set<Endpoint> endpoints = new LinkedHashSet<>();
        for (Class<?> controller : controllers) {
            String basePath = normalizePath(readMappingValue(controller.getAnnotation(RequestMapping.class)));
            for (Method method : controller.getDeclaredMethods()) {
                for (Annotation annotation : method.getAnnotations()) {
                    String httpMethod = MAPPING_ANNOTATIONS.get(annotation.annotationType().getName());
                    if (httpMethod == null) {
                        continue;
                    }
                    String subPath = readMappingValue(annotation);
                    endpoints.add(new Endpoint(basePath + normalizePath(subPath), httpMethod));
                }
            }
        }
        return endpoints;
    }

    private String readMappingValue(Annotation annotation) {
        if (annotation == null) {
            return "";
        }
        try {
            Object value = annotation.annotationType().getMethod("value").invoke(annotation);
            if (value instanceof String[] strings && strings.length > 0) {
                return strings[0];
            }
        } catch (ReflectiveOperationException ignored) {
            // no value attribute
        }
        return "";
    }

    private String normalizePath(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String path = raw.trim();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return path;
    }

    private boolean isHttpMethod(String method) {
        return switch (method) {
            case "GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS" -> true;
            default -> false;
        };
    }

    private Path resolveRepoPath(String relativeFromModule) {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        for (int i = 0; i < 8; i++) {
            Path candidate = cwd.resolve(relativeFromModule).normalize();
            if (Files.exists(candidate)) {
                return candidate;
            }
            cwd = cwd.getParent();
        }
        throw new IllegalStateException("could not resolve repo path for " + relativeFromModule);
    }
}

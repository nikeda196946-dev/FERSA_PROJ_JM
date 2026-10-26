package com.fersacare.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Public Supabase settings required by the browser for Supabase Auth. */
@RestController
@RequestMapping("/api/v1/config")
public class PublicConfigController {

    private final String supabaseUrl;
    private final String supabaseAnonKey;

    public PublicConfigController(
        @Value("${fersa.supabase.url}") String supabaseUrl,
        @Value("${fersa.supabase.anon-key:}") String supabaseAnonKey
    ) {
        this.supabaseUrl = supabaseUrl;
        this.supabaseAnonKey = supabaseAnonKey;
    }

    @GetMapping
    public Map<String, String> config() {
        return Map.of("supabaseUrl", supabaseUrl, "supabaseAnonKey", supabaseAnonKey);
    }
}

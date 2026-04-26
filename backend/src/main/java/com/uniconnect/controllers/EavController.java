package com.uniconnect.controllers;

import com.uniconnect.services.EavService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * EAV attribute endpoints for extensible post metadata.
 *
 * GET  /posts/{id}/attributes          → get all attributes for a post
 * PUT  /posts/{id}/attributes          → set/update attributes (admin)
 * DELETE /posts/{id}/attributes/{name} → remove an attribute (admin)
 * GET  /attributes                     → list all available attribute names
 */
@RestController
public class EavController {

    @Autowired private EavService eavService;

    // ── GET /posts/:id/attributes ─────────────────────────────
    @GetMapping("/posts/{id}/attributes")
    public ResponseEntity<ApiResponse<Map<String, String>>> getAttributes(
            @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(eavService.getAttributes(id)));
    }

    // ── PUT /posts/:id/attributes (admin/university only) ─────
    @PutMapping("/posts/{id}/attributes")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY')")
    public ResponseEntity<ApiResponse<Void>> setAttributes(
            @PathVariable String id,
            @RequestBody Map<String, String> attributes) {
        eavService.setAttributes(id, attributes);
        return ResponseEntity.ok(ApiResponse.success(null, "Attributes updated"));
    }

    // ── DELETE /posts/:id/attributes/:name ────────────────────
    @DeleteMapping("/posts/{id}/attributes/{name}")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY')")
    public ResponseEntity<ApiResponse<Void>> deleteAttribute(
            @PathVariable String id,
            @PathVariable String name) {
        eavService.deleteAttribute(id, name);
        return ResponseEntity.ok(ApiResponse.success(null, "Attribute removed"));
    }

    // ── GET /attributes (all available attribute definitions) ─
    @GetMapping("/attributes")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listAttributes() {
        return ResponseEntity.ok(ApiResponse.success(eavService.listAvailableAttributes()));
    }
}

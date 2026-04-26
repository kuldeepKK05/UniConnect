package com.uniconnect.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * EAV (Entity–Attribute–Value) service for extensible post metadata.
 *
 * Allows adding arbitrary attributes to posts without schema changes.
 * Examples: prize_pool, team_size, stipend_amount, duration_weeks,
 *           eligibility, certificate, contact_email, etc.
 *
 * Schema (see schema.sql):
 *   eav_entities   (entity_id, entity_type, ref_id)
 *   eav_attributes (attribute_id, name, data_type)
 *   eav_values     (entity_id, attribute_id, value)
 */
@Service
public class EavService {

    @Autowired
    private JdbcTemplate jdbc;

    // ── Get all EAV attributes for a post ─────────────────────

    public Map<String, String> getAttributes(String postId) {
        String sql = """
            SELECT a.name, v.value
            FROM eav_values v
            JOIN eav_entities e ON e.entity_id = v.entity_id
            JOIN eav_attributes a ON a.attribute_id = v.attribute_id
            WHERE e.ref_id = ?::uuid AND e.entity_type = 'POST'
            """;

        Map<String, String> result = new LinkedHashMap<>();
        jdbc.query(sql, rs -> {
            result.put(rs.getString("name"), rs.getString("value"));
        }, postId);
        return result;
    }

    // ── Set (upsert) an attribute for a post ──────────────────

    @Transactional
    public void setAttribute(String postId, String attributeName, String value) {
        // Ensure entity row exists
        String entityId = ensureEntity(postId, "POST");

        // Get or create attribute
        String attrId = getAttributeId(attributeName);
        if (attrId == null) {
            throw new IllegalArgumentException("Unknown attribute: " + attributeName
                    + ". Use one of the predefined attributes.");
        }

        // Upsert value
        String upsert = """
            INSERT INTO eav_values (entity_id, attribute_id, value, updated_at)
            VALUES (?::uuid, ?::uuid, ?, NOW())
            ON CONFLICT (entity_id, attribute_id)
            DO UPDATE SET value = EXCLUDED.value, updated_at = NOW()
            """;
        jdbc.update(upsert, entityId, attrId, value);
    }

    // ── Delete an attribute for a post ────────────────────────

    @Transactional
    public void deleteAttribute(String postId, String attributeName) {
        String sql = """
            DELETE FROM eav_values
            WHERE entity_id = (
                SELECT entity_id FROM eav_entities
                WHERE ref_id = ?::uuid AND entity_type = 'POST' LIMIT 1
            )
            AND attribute_id = (
                SELECT attribute_id FROM eav_attributes WHERE name = ? LIMIT 1
            )
            """;
        jdbc.update(sql, postId, attributeName);
    }

    // ── List all available attribute names ────────────────────

    public List<Map<String, Object>> listAvailableAttributes() {
        return jdbc.queryForList(
                "SELECT name, data_type, description FROM eav_attributes ORDER BY name");
    }

    // ── Batch set multiple attributes ─────────────────────────

    @Transactional
    public void setAttributes(String postId, Map<String, String> attributes) {
        attributes.forEach((name, value) -> setAttribute(postId, name, value));
    }

    // ── Private helpers ───────────────────────────────────────

    private String ensureEntity(String refId, String entityType) {
        String existing = jdbc.query(
                "SELECT entity_id FROM eav_entities WHERE ref_id = ?::uuid AND entity_type = ? LIMIT 1",
                rs -> rs.next() ? rs.getString("entity_id") : null,
                refId, entityType);

        if (existing != null) return existing;

        String newId = UUID.randomUUID().toString();
        jdbc.update(
                "INSERT INTO eav_entities (entity_id, entity_type, ref_id) VALUES (?::uuid, ?, ?::uuid)",
                newId, entityType, refId);
        return newId;
    }

    private String getAttributeId(String name) {
        try {
            return jdbc.queryForObject(
                    "SELECT attribute_id FROM eav_attributes WHERE name = ?",
                    String.class, name);
        } catch (Exception e) {
            return null;
        }
    }
}

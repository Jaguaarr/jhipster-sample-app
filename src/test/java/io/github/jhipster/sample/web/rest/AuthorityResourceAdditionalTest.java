package io.github.jhipster.sample.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jhipster.sample.IntegrationTest;
import io.github.jhipster.sample.domain.Authority;
import io.github.jhipster.sample.repository.AuthorityRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Additional integration tests for the {@link AuthorityResource} REST controller.
 */
@AutoConfigureMockMvc
@IntegrationTest
@WithMockUser(authorities = "ROLE_ADMIN")
class AuthorityResourceAdditionalTest {

    private static final String DEFAULT_NAME = "ROLE_TEST";
    private static final String ENTITY_API_URL = "/api/authorities";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private MockMvc restAuthorityMockMvc;

    private Authority authority;
    private Authority insertedAuthority;

    @BeforeEach
    void initTest() {
        authority = new Authority();
        authority.setName(DEFAULT_NAME);
    }

    @AfterEach
    void cleanup() {
        if (insertedAuthority != null) {
            try {
                authorityRepository.deleteById(insertedAuthority.getName());
            } catch (Exception e) {
                // Ignore if already deleted
            }
            insertedAuthority = null;
        }
    }

    @Test
    @Transactional
    void createAuthorityWithExistingId() throws Exception {
        // Create the Authority with an existing ID
        authority.setName("ROLE_USER"); // This should already exist

        long databaseSizeBeforeCreate = authorityRepository.count();

        // An entity with an existing ID cannot be created, so this API call must fail
        restAuthorityMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(authority)))
            .andExpect(status().isBadRequest());

        // Validate the Authority in the database
        assertThat(authorityRepository.count()).isEqualTo(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void getAllAuthorities() throws Exception {
        // Initialize the database
        insertedAuthority = authorityRepository.saveAndFlush(authority);

        // Get all the authorityList
        restAuthorityMockMvc
            .perform(get(ENTITY_API_URL))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)));
    }

    @Test
    @Transactional
    void getAuthority() throws Exception {
        // Initialize the database
        insertedAuthority = authorityRepository.saveAndFlush(authority);

        // Get the authority
        restAuthorityMockMvc
            .perform(get(ENTITY_API_URL_ID, authority.getName()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME));
    }

    @Test
    @Transactional
    void getNonExistingAuthority() throws Exception {
        // Get the authority
        restAuthorityMockMvc.perform(get(ENTITY_API_URL_ID, "NON_EXISTENT")).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void deleteAuthority() throws Exception {
        // Initialize the database
        insertedAuthority = authorityRepository.saveAndFlush(authority);

        long databaseSizeBeforeDelete = authorityRepository.count();

        // Delete the authority
        restAuthorityMockMvc
            .perform(delete(ENTITY_API_URL_ID, authority.getName()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertThat(authorityRepository.count()).isEqualTo(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    void deleteNonExistingAuthority() throws Exception {
        long databaseSizeBeforeDelete = authorityRepository.count();

        // Delete the authority
        restAuthorityMockMvc
            .perform(delete(ENTITY_API_URL_ID, "NON_EXISTENT").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database size is unchanged
        assertThat(authorityRepository.count()).isEqualTo(databaseSizeBeforeDelete);
    }

    @Test
    @WithMockUser // No admin role
    void testUnauthorizedAccess() throws Exception {
        // Test without admin role
        restAuthorityMockMvc.perform(get(ENTITY_API_URL)).andExpect(status().isForbidden());
    }
}

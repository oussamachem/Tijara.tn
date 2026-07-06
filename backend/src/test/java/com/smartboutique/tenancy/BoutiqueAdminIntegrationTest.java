package com.smartboutique.tenancy;

import com.smartboutique.entity.BoutiqueStatus;
import com.smartboutique.repository.BoutiqueRepository;
import com.smartboutique.repository.UserRepository;
import com.smartboutique.security.JwtService;
import com.smartboutique.support.AbstractPostgresIT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 2 — Espace SUPER_ADMIN : creation de boutiques + admin initial, JWT porte le bon
 * boutique_id, suspension bloque le login, acces reserve au SUPER_ADMIN.
 */
@AutoConfigureMockMvc
@WithUserDetails(value = "superadmin@smartboutique.com", userDetailsServiceBeanName = "customUserDetailsService")
class BoutiqueAdminIntegrationTest extends AbstractPostgresIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private BoutiqueRepository boutiqueRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtService jwtService;

    @BeforeEach @AfterEach
    void clean() {
        userRepository.findAll().stream()
                .filter(u -> u.getEmail().endsWith(".test"))
                .forEach(userRepository::delete);
        boutiqueRepository.deleteAll();
    }

    private String createBoutique(String name, String adminEmail) {
        return "{\"name\":\"" + name + "\",\"adminEmail\":\"" + adminEmail
                + "\",\"adminPassword\":\"Passw0rd!\",\"adminName\":\"Admin " + name + "\"}";
    }

    private String login(String email, String pwd) throws Exception {
        String body = "{\"email\":\"" + email + "\",\"password\":\"" + pwd + "\"}";
        String json = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(json, "$.token");
    }

    @Test
    @DisplayName("SUPER_ADMIN cree 2 boutiques + admins ; chaque admin logge -> JWT avec SON boutique_id")
    void createTwoBoutiques_eachAdminGetsOwnTenant() throws Exception {
        String jsonA = mockMvc.perform(post("/api/admin/boutiques").contentType(MediaType.APPLICATION_JSON)
                        .content(createBoutique("Boutique A", "admina@shopa.test")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("boutique-a"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();
        long idA = ((Number) com.jayway.jsonpath.JsonPath.read(jsonA, "$.id")).longValue();

        String jsonB = mockMvc.perform(post("/api/admin/boutiques").contentType(MediaType.APPLICATION_JSON)
                        .content(createBoutique("Boutique B", "adminb@shopb.test")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long idB = ((Number) com.jayway.jsonpath.JsonPath.read(jsonB, "$.id")).longValue();

        // Chaque admin se connecte : le token porte le boutique_id de SA boutique.
        assertThat(jwtService.extractBoutiqueId(login("admina@shopa.test", "Passw0rd!"))).isEqualTo(idA);
        assertThat(jwtService.extractBoutiqueId(login("adminb@shopb.test", "Passw0rd!"))).isEqualTo(idB);
        assertThat(idA).isNotEqualTo(idB);
    }

    @Test
    @DisplayName("Meme nom deux fois -> slugs distincts (boutique-a, boutique-a-2)")
    void duplicateName_uniqueSlugs() throws Exception {
        mockMvc.perform(post("/api/admin/boutiques").contentType(MediaType.APPLICATION_JSON)
                        .content(createBoutique("Boutique A", "a1@shopa.test")))
                .andExpect(jsonPath("$.slug").value("boutique-a"));
        mockMvc.perform(post("/api/admin/boutiques").contentType(MediaType.APPLICATION_JSON)
                        .content(createBoutique("Boutique A", "a2@shopa.test")))
                .andExpect(jsonPath("$.slug").value("boutique-a-2"));
    }

    @Test
    @DisplayName("Email admin deja pris -> 409")
    void duplicateAdminEmail_conflict() throws Exception {
        mockMvc.perform(post("/api/admin/boutiques").contentType(MediaType.APPLICATION_JSON)
                .content(createBoutique("Shop X", "dup@shopx.test"))).andExpect(status().isCreated());
        mockMvc.perform(post("/api/admin/boutiques").contentType(MediaType.APPLICATION_JSON)
                .content(createBoutique("Shop Y", "dup@shopx.test"))).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Boutique suspendue -> son admin ne peut plus se connecter (403)")
    void suspendedBoutique_blocksAdminLogin() throws Exception {
        String json = mockMvc.perform(post("/api/admin/boutiques").contentType(MediaType.APPLICATION_JSON)
                        .content(createBoutique("Shop S", "admins@shops.test")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = ((Number) com.jayway.jsonpath.JsonPath.read(json, "$.id")).longValue();

        // Avant suspension : login OK.
        login("admins@shops.test", "Passw0rd!");

        mockMvc.perform(post("/api/admin/boutiques/{id}/suspend", id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUSPENDED"));

        // Apres suspension : login refuse (compte considere desactive).
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admins@shops.test\",\"password\":\"Passw0rd!\"}"))
                .andExpect(status().isForbidden());

        // Reactivation -> login de nouveau possible.
        mockMvc.perform(post("/api/admin/boutiques/{id}/reactivate", id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
        assertThat(boutiqueRepository.findById(id).orElseThrow().getStatus()).isEqualTo(BoutiqueStatus.ACTIVE);
        login("admins@shops.test", "Passw0rd!");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Un ADMIN de boutique n'accede pas a l'espace plateforme -> 403")
    void boutiqueAdmin_cannotAccessPlatform() throws Exception {
        mockMvc.perform(get("/api/admin/boutiques")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/boutiques").contentType(MediaType.APPLICATION_JSON)
                .content(createBoutique("Hack", "hack@x.test"))).andExpect(status().isForbidden());
    }
}

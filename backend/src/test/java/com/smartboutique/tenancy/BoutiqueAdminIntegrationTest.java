package com.smartboutique.tenancy;

import com.smartboutique.entity.BoutiqueStatus;
import com.smartboutique.entity.ShopMemberRole;
import com.smartboutique.entity.User;
import com.smartboutique.repository.BoutiqueRepository;
import com.smartboutique.repository.ShopMemberRepository;
import com.smartboutique.repository.UserRepository;
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
 * Phase A — Espace PLATEFORME : le PLATFORM_ADMIN cree des boutiques (chacune avec son OWNER +
 * membership), suspend/reactive. Login = identite (une boutique suspendue n'empeche plus la
 * connexion, seulement l'acces a la boutique). Acces plateforme reserve au PLATFORM_ADMIN.
 */
@AutoConfigureMockMvc
@WithUserDetails(value = "superadmin@smartboutique.com", userDetailsServiceBeanName = "customUserDetailsService")
class BoutiqueAdminIntegrationTest extends AbstractPostgresIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private BoutiqueRepository boutiqueRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ShopMemberRepository shopMemberRepository;

    @BeforeEach @AfterEach
    void clean() {
        userRepository.findAll().stream()
                .filter(u -> u.getEmail().endsWith(".test"))
                .forEach(userRepository::delete);
        boutiqueRepository.deleteAll();
    }

    private String body(String name, String adminEmail) {
        return "{\"name\":\"" + name + "\",\"adminEmail\":\"" + adminEmail
                + "\",\"adminPassword\":\"Passw0rd!\",\"adminName\":\"Admin " + name + "\"}";
    }

    private long create(String name, String adminEmail) throws Exception {
        String json = mockMvc.perform(post("/api/admin/boutiques").contentType(MediaType.APPLICATION_JSON)
                        .content(body(name, adminEmail)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) com.jayway.jsonpath.JsonPath.read(json, "$.id")).longValue();
    }

    private void login(String email, int expectedStatus) throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Passw0rd!\"}"))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    @DisplayName("PLATFORM_ADMIN cree 2 boutiques : chaque proprietaire a une membership OWNER de SA boutique")
    void createTwoBoutiques_eachOwnerIsOwnerMember() throws Exception {
        long idA = create("Boutique A", "owner.a@shop.test");
        long idB = create("Boutique B", "owner.b@shop.test");
        assertThat(idA).isNotEqualTo(idB);

        User ownerA = userRepository.findByEmail("owner.a@shop.test").orElseThrow();
        User ownerB = userRepository.findByEmail("owner.b@shop.test").orElseThrow();
        assertThat(shopMemberRepository.findByShopIdAndUserId(idA, ownerA.getId()))
                .get().extracting(m -> m.getRole()).isEqualTo(ShopMemberRole.OWNER);
        assertThat(shopMemberRepository.findByShopIdAndUserId(idB, ownerB.getId()))
                .get().extracting(m -> m.getRole()).isEqualTo(ShopMemberRole.OWNER);
        // Le proprietaire n'est PAS membre de l'autre boutique.
        assertThat(shopMemberRepository.existsByShopIdAndUserId(idB, ownerA.getId())).isFalse();
        // Chacun peut se connecter (identite).
        login("owner.a@shop.test", 200);
        login("owner.b@shop.test", 200);
    }

    @Test
    @DisplayName("Meme nom deux fois -> slugs distincts (boutique-a, boutique-a-2)")
    void duplicateName_uniqueSlugs() throws Exception {
        mockMvc.perform(post("/api/admin/boutiques").contentType(MediaType.APPLICATION_JSON)
                        .content(body("Boutique A", "a1@shopa.test")))
                .andExpect(jsonPath("$.slug").value("boutique-a"));
        mockMvc.perform(post("/api/admin/boutiques").contentType(MediaType.APPLICATION_JSON)
                        .content(body("Boutique A", "a2@shopa.test")))
                .andExpect(jsonPath("$.slug").value("boutique-a-2"));
    }

    @Test
    @DisplayName("Email proprietaire deja pris -> 409")
    void duplicateOwnerEmail_conflict() throws Exception {
        mockMvc.perform(post("/api/admin/boutiques").contentType(MediaType.APPLICATION_JSON)
                .content(body("Shop X", "dup@shopx.test"))).andExpect(status().isCreated());
        mockMvc.perform(post("/api/admin/boutiques").contentType(MediaType.APPLICATION_JSON)
                .content(body("Shop Y", "dup@shopx.test"))).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Suspension/reactivation : le login (identite) reste possible dans les deux etats")
    void suspendReactivate_loginAlwaysWorks() throws Exception {
        long id = create("Shop S", "owner.s@shop.test");
        login("owner.s@shop.test", 200);

        mockMvc.perform(post("/api/admin/boutiques/{id}/suspend", id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUSPENDED"));
        // Phase A : la suspension bloque l'ACCES a la boutique, pas la connexion (compte global).
        login("owner.s@shop.test", 200);

        mockMvc.perform(post("/api/admin/boutiques/{id}/reactivate", id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
        assertThat(boutiqueRepository.findById(id).orElseThrow().getStatus()).isEqualTo(BoutiqueStatus.ACTIVE);
    }

    @Test
    @WithMockUser(roles = "SHOP_OWNER")
    @DisplayName("Un OWNER de boutique n'accede pas a l'espace plateforme -> 403")
    void shopOwner_cannotAccessPlatform() throws Exception {
        mockMvc.perform(get("/api/admin/boutiques")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/boutiques").contentType(MediaType.APPLICATION_JSON)
                .content(body("Hack", "hack@x.test"))).andExpect(status().isForbidden());
    }
}

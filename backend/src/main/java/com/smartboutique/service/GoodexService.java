package com.smartboutique.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartboutique.entity.Boutique;
import com.smartboutique.entity.Order;
import com.smartboutique.entity.OrderItem;
import com.smartboutique.exception.BusinessException;
import com.smartboutique.exception.ResourceNotFoundException;
import com.smartboutique.repository.BoutiqueRepository;
import com.smartboutique.repository.OrderRepository;
import com.smartboutique.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Intégration transporteur <b>Goodex / Neo Parcel</b> (API REST externe). Création d'un colis à
 * partir d'une commande en ligne (snapshot livraison + COD = total) et rafraîchissement du statut.
 *
 * <p>Identifiants (token permanent + user_id) stockés PAR boutique. Le tenant courant (X-Shop-Id)
 * fixe la boutique et scope la commande (RLS). Aucune donnée d'une autre boutique n'est envoyée.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoodexService {

    private final BoutiqueRepository boutiqueRepository;
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    private static final String DEFAULT_BASE_URL = "https://transport.goodex.tn";
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    /** Gouvernorats acceptés par Goodex (orthographe exacte exigée). */
    private static final Set<String> GOVERNORATS = Set.of(
            "Tunis", "Sousse", "Nabeul", "Ariana", "Manouba", "Monastir", "Mahdia",
            "Ben Arous", "Bizerte", "Kairouan", "Sfax", "Zaghouan", "Beja", "Gabes",
            "Gafsa", "Jendouba", "Kasserine", "Kebili", "Kef", "Medenine",
            "Sidi Bouzid", "Siliana", "Tataouine", "Tozeur");

    /** last_statut (int) -> désignation (cf. doc). Le suivi GET renvoie déjà le libellé en clair. */
    private static final Map<Integer, String> STATUS_LABELS = Map.ofEntries(
            Map.entry(1, "En cours"), Map.entry(2, "Livré"), Map.entry(3, "En dépôt"),
            Map.entry(4, "En transit"), Map.entry(5, "Enlevé"), Map.entry(6, "Retour"),
            Map.entry(7, "Exception"), Map.entry(8, "Payé"), Map.entry(9, "Préparation retour"),
            Map.entry(10, "Non reçue"), Map.entry(11, "Retour payé"), Map.entry(12, "En attente"),
            Map.entry(13, "Préparation livraison"), Map.entry(14, "Dispatch"), Map.entry(15, "Prép. paiement"),
            Map.entry(16, "Err sync"), Map.entry(17, "Retour reçu magasin"), Map.entry(18, "Vérification"),
            Map.entry(19, "Échange"), Map.entry(20, "Livré provisoirement"));

    /**
     * Crée le colis Goodex pour une commande et enregistre le code de suivi (ean) + statut initial.
     * Idempotence : refuse si un colis existe déjà (carrierEan non nul).
     */
    @Transactional
    public void createColis(Long orderId) {
        Boutique b = requireBoutique();
        String token = require(b.getGoodexToken(), "Token Goodex manquant : renseignez-le dans Réglages.");
        String userId = require(b.getGoodexUserId(), "user_id Goodex manquant : renseignez-le dans Réglages.");
        String base = baseUrl(b);

        Order o = orderRepository.findDetailById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Commande", orderId));
        if (o.getCarrierEan() != null) {
            throw new BusinessException("Un colis existe déjà pour cette commande (" + o.getCarrierEan() + ").",
                    HttpStatus.CONFLICT);
        }
        String client = require(o.getDeliveryName(), "Nom du client manquant sur la commande.");
        String gsm1 = require(o.getDeliveryPhone(), "Téléphone du client manquant (à compléter dans son profil).");
        String adresse = require(o.getDeliveryAddress(), "Adresse de livraison manquante (profil client).");
        String gov = require(o.getDeliveryGovernorat(), "Gouvernorat de livraison manquant (profil client).");
        if (!GOVERNORATS.contains(gov)) {
            throw new BusinessException("Gouvernorat non accepté par Goodex : « " + gov + " ».", HttpStatus.BAD_REQUEST);
        }

        Map<String, String> form = new LinkedHashMap<>();
        form.put("token", token);
        form.put("user_id", userId);
        form.put("client", client);
        form.put("cod", o.getTotalAmount().stripTrailingZeros().toPlainString());
        form.put("adresse", adresse);
        form.put("governorat", gov);
        form.put("gsm1", gsm1);
        form.put("gsm2", "");
        form.put("designation", designation(o));
        form.put("nbre", String.valueOf(o.getItems().stream().mapToInt(OrderItem::getQuantity).sum()));
        form.put("remarque", o.getReference());
        form.put("echange", "0");
        form.put("decharge", "0");
        form.put("last_statut", "12");   // statut initial « En attente » (champ exigé par l'API Goodex)

        JsonNode res = postForm(base + "/api/v1/colis/create", form);
        String ean = res.path("ean").asText(null);
        if (ean == null || ean.isBlank()) {
            throw new BusinessException("Réponse Goodex sans code de suivi. " + firstError(res), HttpStatus.BAD_GATEWAY);
        }
        int last = res.path("last_statut").asInt(12);
        o.setCarrierEan(ean);
        o.setCarrierStatus(STATUS_LABELS.getOrDefault(last, "En attente"));
        o.setCarrierStatusAt(LocalDateTime.now());
        orderRepository.save(o);
        log.info("[Goodex] Colis créé pour commande {} -> ean={} statut={}", o.getReference(), ean, o.getCarrierStatus());
    }

    /** Rafraîchit le statut Goodex d'une commande déjà expédiée (GET /colis/etat/{ean}). */
    @Transactional
    public void refreshStatus(Long orderId) {
        Boutique b = requireBoutique();
        String token = require(b.getGoodexToken(), "Token Goodex manquant : renseignez-le dans Réglages.");
        String base = baseUrl(b);

        Order o = orderRepository.findDetailById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Commande", orderId));
        if (o.getCarrierEan() == null) {
            throw new BusinessException("Aucun colis Goodex pour cette commande.", HttpStatus.BAD_REQUEST);
        }
        String url = base + "/api/v1/colis/etat/" + enc(o.getCarrierEan()) + "?token=" + enc(token);
        JsonNode res = getJson(url);
        String statut = res.path("statut").asText(null);
        if (statut == null || statut.isBlank()) {
            throw new BusinessException("Statut Goodex indisponible.", HttpStatus.BAD_GATEWAY);
        }
        o.setCarrierStatus(statut);
        o.setCarrierStatusAt(LocalDateTime.now());
        orderRepository.save(o);
        log.info("[Goodex] Statut commande {} -> {}", o.getReference(), statut);
    }

    // --------------------------------- HTTP ---------------------------------

    private JsonNode postForm(String url, Map<String, String> fields) {
        String boundary = "----sbGoodex" + System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : fields.entrySet()) {
            sb.append("--").append(boundary).append("\r\n")
              .append("Content-Disposition: form-data; name=\"").append(e.getKey()).append("\"\r\n\r\n")
              .append(e.getValue() == null ? "" : e.getValue()).append("\r\n");
        }
        sb.append("--").append(boundary).append("--\r\n");
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString(sb.toString(), StandardCharsets.UTF_8))
                .build();
        return send(req);
    }

    private JsonNode getJson(String url) {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();
        return send(req);
    }

    private JsonNode send(HttpRequest req) {
        try {
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode node;
            try {
                node = objectMapper.readTree(resp.body() == null || resp.body().isBlank() ? "{}" : resp.body());
            } catch (Exception parse) {
                throw new BusinessException("Réponse Goodex illisible (HTTP " + resp.statusCode() + ").",
                        HttpStatus.BAD_GATEWAY);
            }
            if (resp.statusCode() >= 400) {
                throw new BusinessException("Goodex a refusé la requête (HTTP " + resp.statusCode() + "). "
                        + firstError(node), HttpStatus.BAD_GATEWAY);
            }
            return node;
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.warn("[Goodex] Appel échoué : {}", e.toString());
            throw new BusinessException("Impossible de contacter Goodex : " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    // --------------------------------- Helpers ---------------------------------

    private Boutique requireBoutique() {
        Long shopId = TenantContext.get();
        if (shopId == null) throw new BusinessException("Aucune boutique active", HttpStatus.BAD_REQUEST);
        return boutiqueRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Boutique", shopId));
    }

    private static String baseUrl(Boutique b) {
        String u = b.getGoodexBaseUrl();
        if (u == null || u.isBlank()) return DEFAULT_BASE_URL;
        return u.trim().replaceAll("/+$", "");   // sans slash final
    }

    private static String designation(Order o) {
        String d = o.getItems().stream()
                .map(it -> it.getVariant().getProduct().getName())
                .distinct().collect(Collectors.joining(", "));
        if (d.isBlank()) d = "Produit";
        return d.length() > 190 ? d.substring(0, 190) : d;
    }

    /** Extrait un message d'erreur lisible d'une réponse Goodex ({"champ":["message"]} ou {"statut":...}). */
    private static String firstError(JsonNode node) {
        if (node == null) return "";
        if (node.hasNonNull("statut")) return node.get("statut").asText();
        if (node.hasNonNull("message")) return node.get("message").asText();
        var it = node.fields();
        if (it.hasNext()) {
            var e = it.next();
            JsonNode v = e.getValue();
            String msg = v.isArray() && v.size() > 0 ? v.get(0).asText() : v.asText();
            return e.getKey() + " : " + msg;
        }
        return "";
    }

    private static String require(String v, String message) {
        if (v == null || v.isBlank()) throw new BusinessException(message, HttpStatus.BAD_REQUEST);
        return v.trim();
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}

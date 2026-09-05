package com.smartboutique.controller;

import com.smartboutique.dto.ProductOgData;
import com.smartboutique.entity.Boutique;
import com.smartboutique.service.ShopService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * Partage social : page HTML PUBLIQUE d'un produit avec balises <b>Open Graph</b> injectées
 * CÔTÉ SERVEUR (le crawler Facebook/WhatsApp lit le HTML brut, pas le JS de la SPA).
 *
 * <p>URL partageable : {@code /s/{slug}/produit/{productId}}. Le crawler y lit les og:* ; un humain
 * est redirigé (JS) vers la fiche interactive de la SPA {@code /s/{slug}/p/{productId}}. Champs
 * strictement PUBLICS (nom, boutique, prix, catégorie, image) — jamais de coût/marge/stock.</p>
 */
@RestController
@RequiredArgsConstructor
public class ProductShareController {

    private final ShopService shopService;

    /** Base publique absolue (ex. https://soo9.shop). Vide -> dérivée de la requête (dev/tunnel). */
    @Value("${app.public-base-url:}")
    private String configuredBaseUrl;

    @GetMapping(value = "/s/{slug}/produit/{productId}", produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
    public ResponseEntity<String> share(@PathVariable String slug, @PathVariable Long productId,
                                        HttpServletRequest request) {
        Boutique b = shopService.enterShop(slug);          // pose le tenant (404 si boutique inactive/inconnue)
        ProductOgData og;
        try {
            og = shopService.productOgScoped(productId);    // RLS -> 404 si le produit n'est pas de cette boutique
        } finally {
            com.smartboutique.tenancy.TenantContext.clear();
        }

        String base = publicBaseUrl(request);
        String shareUrl = base + "/s/" + enc(slug) + "/produit/" + productId;      // og:url (cette page)
        String spaPath = "/s/" + enc(slug) + "/p/" + productId;                    // redirection humaine (SPA)
        String imgRel = og.imageUrl() != null ? og.imageUrl()
                : (b.getLogoUrl() != null ? b.getLogoUrl() : null);
        String imgAbs = imgRel != null ? base + imgRel : null;

        String title = og.name() + " — " + b.getName();
        String desc = priceLabel(og.price()) + (og.category() != null ? " · " + og.category() : "");

        StringBuilder h = new StringBuilder(2048);
        h.append("<!doctype html><html lang=\"fr\"><head><meta charset=\"utf-8\">")
         .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
         .append("<title>").append(esc(title)).append("</title>")
         .append(meta("og:title", title))
         .append(meta("og:description", desc))
         .append(meta("og:url", shareUrl))
         .append(meta("og:type", "product"))
         .append(meta("og:site_name", b.getName()));
        if (imgAbs != null) {
            h.append(meta("og:image", imgAbs))
             .append(metaName("twitter:image", imgAbs));
        }
        h.append(metaName("twitter:card", imgAbs != null ? "summary_large_image" : "summary"))
         .append(metaName("twitter:title", title))
         .append(metaName("twitter:description", desc))
         .append("<link rel=\"canonical\" href=\"").append(esc(shareUrl)).append("\">")
         .append("</head><body style=\"font-family:system-ui,sans-serif;text-align:center;padding:24px\">");
        // Fallback visible (bots sans JS / no-JS) + redirection des humains vers la SPA.
        if (imgAbs != null) {
            h.append("<img src=\"").append(esc(imgAbs)).append("\" alt=\"").append(esc(og.name()))
             .append("\" style=\"max-width:320px;width:100%;border-radius:12px\">");
        }
        h.append("<h1 style=\"font-size:20px\">").append(esc(og.name())).append("</h1>")
         .append("<p style=\"color:#555\">").append(esc(b.getName())).append("</p>")
         .append("<p style=\"font-size:22px;font-weight:800\">").append(esc(priceLabel(og.price()))).append("</p>")
         .append("<p><a href=\"").append(esc(spaPath)).append("\">Voir le produit &rsaquo;</a></p>")
         .append("<script>location.replace(").append(jsString(spaPath)).append(");</script>")
         .append("</body></html>");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/html;charset=UTF-8")
                .body(h.toString());
    }

    // --------------------------------- helpers ---------------------------------

    private String publicBaseUrl(HttpServletRequest req) {
        if (configuredBaseUrl != null && !configuredBaseUrl.isBlank()) {
            return configuredBaseUrl.trim().replaceAll("/+$", "");
        }
        String proto = firstNonBlank(req.getHeader("X-Forwarded-Proto"), req.getScheme());
        String host = firstNonBlank(req.getHeader("X-Forwarded-Host"), req.getHeader("Host"));
        if (host == null || host.isBlank()) host = req.getServerName() + ":" + req.getServerPort();
        // X-Forwarded-Host peut contenir une liste : on garde le premier hôte.
        if (host.contains(",")) host = host.substring(0, host.indexOf(',')).trim();
        return proto + "://" + host;
    }

    private static String priceLabel(BigDecimal price) {
        if (price == null) return "";
        return price.stripTrailingZeros().toPlainString() + " DT";
    }

    private static String meta(String prop, String content) {
        return "<meta property=\"" + prop + "\" content=\"" + esc(content) + "\">";
    }

    private static String metaName(String name, String content) {
        return "<meta name=\"" + name + "\" content=\"" + esc(content) + "\">";
    }

    /** Échappe pour un attribut/texte HTML (empêche toute cassure/injection dans les meta). */
    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    /** Chaîne littérale JS sûre (pour location.replace). */
    private static String jsString(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("<", "\\u003C").replace(">", "\\u003E") + "\"";
    }

    private static String enc(String s) {
        // slug déjà [a-z0-9-] ; on neutralise tout caractère de contrôle par sécurité.
        return s == null ? "" : s.replaceAll("[^a-zA-Z0-9._~-]", "");
    }

    private static String firstNonBlank(String a, String b) {
        return (a != null && !a.isBlank()) ? a : b;
    }
}

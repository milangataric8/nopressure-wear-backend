package rs.nopressurewear.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.nopressurewear.repository.CategoryRepository;
import rs.nopressurewear.repository.ProductRepository;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SitemapController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Value("${app.frontend-url}")
    private String siteUrl;

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemap() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");

        List.of("", "/products", "/contact", "/privacy-policy", "/terms", "/returns", "/imprint")
            .forEach(path -> appendUrl(sb, siteUrl + path));

        productRepository.findByIsActiveTrueOrderByIdAsc()
            .forEach(p -> appendUrl(sb, siteUrl + "/products/" + p.getId()));

        categoryRepository.findByIsActiveTrueOrderByNameAsc()
            .forEach(c -> appendUrl(sb, siteUrl + "/products?categoryId=" + c.getId()));

        sb.append("</urlset>");
        return sb.toString();
    }

    private void appendUrl(StringBuilder sb, String loc) {
        sb.append("<url><loc>").append(escapeXml(loc)).append("</loc></url>");
    }

    private String escapeXml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}

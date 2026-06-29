package rs.nopressurewear.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.nopressurewear.repository.CategoryRepository;
import rs.nopressurewear.repository.ProductRepository;

import java.util.List;

@RestController
public class SitemapController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Value("${app.frontend-url}")
    private String siteUrl;

    public SitemapController(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemap() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");

        for (String path : List.of("", "/products", "/contact", "/privacy-policy", "/terms", "/returns", "/imprint")) {
            sb.append("<url><loc>").append(siteUrl).append(path).append("</loc></url>");
        }

        productRepository.findByIsActiveTrueOrderByIdAsc().forEach(p ->
            sb.append("<url><loc>").append(siteUrl).append("/products/").append(p.getId()).append("</loc></url>"));

        categoryRepository.findByIsActiveTrueOrderByNameAsc().forEach(c ->
            sb.append("<url><loc>").append(siteUrl).append("/products?categoryId=").append(c.getId()).append("</loc></url>"));

        sb.append("</urlset>");
        return sb.toString();
    }
}

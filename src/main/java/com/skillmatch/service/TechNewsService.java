package com.skillmatch.service;

import com.skillmatch.dto.response.TechNewsItem;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TechNewsService {

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String[] FEEDS = new String[] {
            "https://news.google.com/rss/headlines/section/topic/TECHNOLOGY?hl=en-IN&gl=IN&ceid=IN:en",
            "https://www.theverge.com/rss/index.xml",
            "https://feeds.arstechnica.com/arstechnica/index/"
    };

    public List<TechNewsItem> getLatestTechNews(int limit) {
        int n = Math.max(1, Math.min(30, limit));
        List<TechNewsItem> items = new ArrayList<>();
        for (String feed : FEEDS) {
            try {
                items.addAll(fetchAndParse(feed));
            } catch (Exception ignore) {
                // try next feed
            }
        }
        items.sort(Comparator.comparing((TechNewsItem i) -> parseInstant(i.getPublishedAt())).reversed());
        if (items.size() > n) return items.subList(0, n);
        return items;
    }

    private List<TechNewsItem> fetchAndParse(String feedUrl) throws Exception {
        ResponseEntity<String> res = restTemplate.getForEntity(URI.create(feedUrl), String.class);
        String xml = res.getBody();
        if (xml == null || xml.isBlank()) return List.of();

        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        NodeList nodeList = doc.getElementsByTagName("item");
        List<TechNewsItem> out = new ArrayList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element item = (Element) nodeList.item(i);
            String title = text(item, "title");
            String link = text(item, "link");
            String pubDate = text(item, "pubDate");
            String source = text(item, "source");
            if (source == null || source.isBlank()) {
                source = host(feedUrl);
            }
            if (title == null || title.isBlank() || link == null || link.isBlank()) continue;
            String iso = toIso(pubDate);
            out.add(new TechNewsItem(title.trim(), link.trim(), source.trim(), iso));
        }
        return out;
    }

    private String text(Element parent, String tag) {
        NodeList nl = parent.getElementsByTagName(tag);
        if (nl.getLength() == 0) return "";
        String v = nl.item(0).getTextContent();
        return v == null ? "" : v;
    }

    private String host(String url) {
        try {
            String h = URI.create(url).getHost();
            return h == null ? "Tech News" : h.replace("www.", "");
        } catch (Exception e) {
            return "Tech News";
        }
    }

    private String toIso(String pubDate) {
        try {
            // Most RSS feeds use RFC1123 format
            ZonedDateTime zdt = ZonedDateTime.parse(pubDate, DateTimeFormatter.RFC_1123_DATE_TIME);
            return zdt.toInstant().toString();
        } catch (Exception e) {
            return Instant.now().toString();
        }
    }

    private Instant parseInstant(String iso) {
        try { return Instant.parse(iso); } catch (Exception e) { return Instant.EPOCH; }
    }
}


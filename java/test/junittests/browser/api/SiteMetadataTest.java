package tests.junittests.browser.api;

import balbucio.browser4j.browser.api.SiteMetadata;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SiteMetadataTest {

    @Test
    void shouldExtractAllMetadataFieldsFromDocument() {
        String html = "<html lang='pt-BR'>" +
                "<head>" +
                "<title>Teste</title>" +
                "<link rel='icon' href='https://example.com/favicon.ico'/>" +
                "<link rel='manifest' href='/manifest.json'/>" +
                "<meta name='description' content='descrição de teste'/>" +
                "<meta name='keywords' content='um, dois, tres'/>" +
                "<meta name='theme-color' content='#123456'/>" +
                "<meta name='msapplication-TileColor' content='#654321'/>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1'/>" +
                "<meta name='mobile-web-app-capable' content='yes'/>" +
                "<meta name='google-translate-customization' content='enabled'/>" +
                "<meta name='robots' content='index,follow'/>" +
                "<link rel='canonical' href='https://example.com/page'/>" +
                "</head><body></body></html>";

        Document doc = Jsoup.parse(html);
        SiteMetadata metadata = SiteMetadata.fromDocument(doc, "https://example.com/page");

        assertNotNull(metadata);
        assertEquals("Teste", metadata.getTitle());
        assertEquals("https://example.com/page", metadata.getUrl());
        assertEquals("https://example.com/favicon.ico", metadata.getIcon());
        assertEquals("descrição de teste", metadata.getDescription());
        assertEquals("um, dois, tres", metadata.getKeywords());
        assertEquals("#123456", metadata.getThemeColor());
        assertEquals("#654321", metadata.getBackgroundColor());
        assertEquals("width=device-width, initial-scale=1", metadata.getViewport());
        assertEquals("/manifest.json", metadata.getManifestUrl());
        assertTrue(metadata.isPwaCapable());
        assertEquals("pt-BR", metadata.getLanguage());
        assertTrue(metadata.isAutoTranslationEnabled());
        assertEquals("index,follow", metadata.getRobots());
        assertEquals("https://example.com/page", metadata.getCanonical());
    }

    @Test
    void shouldReturnDefaultsWhenDocumentIsNull() {
        SiteMetadata metadata = SiteMetadata.fromDocument(null, "https://example.com");

        assertNotNull(metadata);
        assertEquals("", metadata.getTitle());
        assertEquals("https://example.com", metadata.getUrl());
        assertEquals("", metadata.getIcon());
        assertEquals("", metadata.getDescription());
        assertEquals("", metadata.getKeywords());
        assertEquals("", metadata.getThemeColor());
        assertEquals("", metadata.getBackgroundColor());
        assertEquals("", metadata.getViewport());
        assertEquals("", metadata.getManifestUrl());
        assertFalse(metadata.isPwaCapable());
        assertEquals("", metadata.getLanguage());
        assertFalse(metadata.isAutoTranslationEnabled());
        assertEquals("", metadata.getRobots());
        assertEquals("", metadata.getCanonical());
    }
}

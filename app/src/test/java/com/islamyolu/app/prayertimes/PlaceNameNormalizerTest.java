package com.islamyolu.app.prayertimes;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PlaceNameNormalizerTest {

    @Test
    public void kurallardakiOrnekler() {
        assertEquals("DEVREKANI", PlaceNameNormalizer.normalize("Devrekâni"));
        assertEquals("MUNCHEN", PlaceNameNormalizer.normalize("München"));
    }

    @Test
    public void turkceHarfler() {
        assertEquals("ISTANBUL", PlaceNameNormalizer.normalize("istanbul"));
        assertEquals("ISTANBUL", PlaceNameNormalizer.normalize("İSTANBUL"));
        assertEquals("IGDIR", PlaceNameNormalizer.normalize("ığdır"));
        assertEquals("SANLIURFA", PlaceNameNormalizer.normalize("Şanlıurfa"));
        assertEquals("CAGLAYANCERIT", PlaceNameNormalizer.normalize("Çağlayancerit"));
        assertEquals("TURKIYE", PlaceNameNormalizer.normalize("TÜRKİYE"));
    }

    @Test
    public void bosluklarVeIsaretlerAtilir() {
        assertEquals("AFYONKARAHISAR", PlaceNameNormalizer.normalize("Afyon-Karahisar"));
        assertEquals("ILEDEFRANCE", PlaceNameNormalizer.normalize("Île-de-France"));
        assertEquals("NEWYORK", PlaceNameNormalizer.normalize("New York"));
        assertEquals("SAOPAULO", PlaceNameNormalizer.normalize("São Paulo"));
    }

    @Test
    public void nullVeBos() {
        assertEquals("", PlaceNameNormalizer.normalize(null));
        assertEquals("", PlaceNameNormalizer.normalize(" - "));
    }
}

package com.spotify.clone

import com.spotify.clone.core.legal.GravitLegalCatalog
import com.spotify.clone.core.legal.LegalDocumentTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GravitLegalCatalogUnitTest {

    @Test
    fun copyrightIsReservedToGravit() {
        assertEquals("GRAVIT", GravitLegalCatalog.OWNER_NAME)
        assertTrue(GravitLegalCatalog.COPYRIGHT_NOTICE.contains("GRAVIT"))
        assertTrue(GravitLegalCatalog.COPYRIGHT_NOTICE.contains("All rights reserved"))
    }

    @Test
    fun allLegalTabsHaveCompleteClauses() {
        assertEquals(5, LegalDocumentTab.entries.size)
        LegalDocumentTab.entries.forEach { tab ->
            val clauses = GravitLegalCatalog.clausesFor(tab)
            assertTrue("Expected clauses for ${tab.name}", clauses.size >= 4)
            clauses.forEach { clause ->
                assertFalse(clause.heading.isBlank())
                assertFalse(clause.body.isBlank())
            }
        }
    }

    @Test
    fun privacyTermsAndCopyrightDiscloseKeyRequirements() {
        val privacyText = GravitLegalCatalog.clausesFor(LegalDocumentTab.PRIVACY).joinToString(" ") { "${it.heading} ${it.body}" }
        val termsText = GravitLegalCatalog.clausesFor(LegalDocumentTab.TERMS).joinToString(" ") { "${it.heading} ${it.body}" }
        val copyrightText = GravitLegalCatalog.clausesFor(LegalDocumentTab.COPYRIGHT).joinToString(" ") { "${it.heading} ${it.body}" }
        val permissionText = GravitLegalCatalog.clausesFor(LegalDocumentTab.PERMISSIONS).joinToString(" ") { "${it.heading} ${it.body}" }

        assertTrue(privacyText.contains("GRAVIT"))
        assertTrue(privacyText.contains("local", ignoreCase = true))
        assertTrue(termsText.contains("AS IS"))
        assertTrue(copyrightText.contains("DMCA"))
        assertTrue(copyrightText.contains(GravitLegalCatalog.LEGAL_EMAIL))
        assertTrue(permissionText.contains("INTERNET"))
        assertTrue(permissionText.contains("READ_MEDIA_AUDIO"))
        assertTrue(permissionText.contains("REQUEST_INSTALL_PACKAGES"))
    }
}

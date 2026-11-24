package org.dueattendant149.kwhale.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServerUrlTest {

    @Test
    fun bareDomainGetsHttpsAndV1() {
        assertEquals(
            "https://music.example.org/v1",
            ServerUrl.normalize("music.example.org"),
        )
    }

    @Test
    fun fullUrlKeepsSchemeAndTrimsSlash() {
        assertEquals(
            "https://music.example.org/v1",
            ServerUrl.normalize("https://music.example.org/v1/"),
        )
    }

    @Test
    fun pathIsPreservedBeforeV1() {
        assertEquals(
            "https://host/kwhale/v1",
            ServerUrl.normalize("https://host/kwhale"),
        )
    }

    @Test
    fun httpIsAllowedForDebugAndLan() {
        assertEquals(
            "http://192.168.1.100:19001/v1",
            ServerUrl.normalize("http://192.168.1.100:19001"),
        )
    }

    @Test
    fun emptyUsesFallback() {
        assertEquals(
            "https://fallback.example/v1",
            ServerUrl.normalize("  ", "https://fallback.example/v1"),
        )
    }

    @Test
    fun garbageRejected() {
        assertNull(ServerUrl.normalize("ftp://host/file"))
        assertNull(ServerUrl.normalize("https://"))
        assertNull(ServerUrl.normalize("://broken"))
    }
}

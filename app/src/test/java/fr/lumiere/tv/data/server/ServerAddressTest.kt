package fr.lumiere.tv.data.server

import org.junit.Assert.assertEquals
import org.junit.Test

class ServerAddressTest {

    private fun ok(input: String): String =
        (ServerAddress.parse(input) as ParseResult.Ok).address.baseUrl

    private fun invalid(input: String): String =
        (ServerAddress.parse(input) as ParseResult.Invalid).message

    @Test
    fun ipSeuleRecoitHttpEtLePortParDefaut() = assertEquals("http://1.2.3.4:8484", ok("1.2.3.4"))

    @Test
    fun espacesAutourIgnores() = assertEquals("http://mon.dyndns.org:9000", ok("  mon.dyndns.org:9000  "))

    @Test
    fun barreFinaleRetiree() = assertEquals("http://exemple.fr:8484", ok("http://exemple.fr:8484/"))

    @Test
    fun httpsSansPortGardeLePortImplicite() = assertEquals("https://exemple.fr", ok("https://exemple.fr"))

    @Test
    fun majusculesNormalisees() = assertEquals("http://exemple.fr:8484", ok("HTTP://Exemple.FR"))

    @Test
    fun adresseVide() = assertEquals("Saisissez l'adresse du serveur.", invalid(""))

    @Test
    fun portNonNumerique() = assertEquals("Le port doit être un nombre entre 1 et 65535.", invalid("exemple.fr:abc"))

    @Test
    fun portTropGrand() = assertEquals("Le port doit être un nombre entre 1 et 65535.", invalid("exemple.fr:70000"))

    @Test
    fun schemaNonHttp() = assertEquals("Adresse en http:// ou https:// uniquement.", invalid("ftp://exemple.fr"))
}

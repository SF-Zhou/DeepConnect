package com.deepconnect.vpn.vpn

/** Small helpers for the address / netmask strings OpenConnect reports. */
object IpUtils {

    fun ipv4PrefixLength(netmask: String?): Int {
        val mask = netmask?.trim().orEmpty()
        if (mask.isEmpty()) return 32
        val octets = mask.split('.')
        if (octets.size == 4) {
            var bits = 0
            for (octet in octets) {
                val value = octet.toIntOrNull() ?: return 32
                bits += Integer.bitCount(value and 0xff)
            }
            return bits.coerceIn(0, 32)
        }
        return mask.toIntOrNull()?.coerceIn(0, 32) ?: 32
    }

    fun ipv6PrefixLength(netmask: String?): Int {
        val mask = netmask?.trim().orEmpty()
        if (mask.isEmpty()) return 128
        if (!mask.contains(':')) {
            return mask.toIntOrNull()?.coerceIn(0, 128) ?: 128
        }
        return runCatching {
            val (address, _) = parseIpv6(mask)
            var bits = 0
            for (word in address) {
                bits += Integer.bitCount(word and 0xffff)
            }
            bits
        }.getOrElse { 128 }
    }

    /**
     * Parses a route as reported by OpenConnect into (address, prefix length).
     *
     * Cisco/ocserv send split routes with a *netmask*, not a prefix length:
     *   "192.0.2.0/255.255.255.0", "192.0.2.57/255.255.255.255",
     *   "2001:db8::/ffff:ffff:ffff:ffff::"
     * while plain CIDR ("192.0.2.0/24") occurs too, so accept both spellings.
     */
    fun parseCidr(cidr: String): Pair<String, Int>? {
        val trimmed = cidr.trim()
        val slash = trimmed.indexOf('/')
        if (slash <= 0) return null
        val address = trimmed.substring(0, slash)
        val suffix = trimmed.substring(slash + 1).trim()
        val prefix = when {
            suffix.isEmpty() -> return null
            suffix.contains('.') -> ipv4PrefixLength(suffix)   // dotted IPv4 netmask
            suffix.contains(':') -> ipv6PrefixLength(suffix)   // IPv6 netmask
            else -> suffix.toIntOrNull() ?: return null        // plain prefix length
        }
        return address to prefix
    }

    fun isIpv4(text: String): Boolean = text.contains('.') && !text.contains(':')

    private fun parseIpv6(text: String): Pair<IntArray, Int> {
        val words = IntArray(8)
        val (head, tail) = if (text.contains("::")) {
            val parts = text.split("::", limit = 2)
            parts[0] to parts[1]
        } else {
            text to ""
        }
        var index = 0
        if (head.isNotEmpty()) {
            for (group in head.split(':')) {
                if (index >= 8) throw IllegalArgumentException("bad IPv6 $text")
                words[index++] = group.toInt(16)
            }
        }
        if (tail.isNotEmpty()) {
            val tailGroups = tail.split(':')
            var tailIndex = 8 - tailGroups.size
            if (tailIndex < index) tailIndex = index
            for (group in tailGroups) {
                if (tailIndex >= 8) throw IllegalArgumentException("bad IPv6 $text")
                words[tailIndex++] = group.toInt(16)
            }
        }
        return words to index
    }
}

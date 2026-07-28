package app.tackcall.repeater

/**
 * BLE-yhteystilan tilakone (Android-kuoren valmistusohje v4, §5/§7).
 *
 * scanning -> connecting -> connected -> (disconnected -> auto-reconnect: takaisin scanning).
 * UI (§7, variant A) yhdistää SCANNING ja CONNECTING samaan keltaiseen
 * "Etsitään laitetta…" -tilaan — käyttäjän ei tarvitse erottaa niitä.
 */
enum class ConnectionState {
    SCANNING,
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
}

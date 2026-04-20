package com.flowpay.app.features.signal_check.domain

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.CellSignalStrengthGsm
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import android.telephony.CellSignalStrengthWcdma
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

// ──────────────────────────────────────────────
// Data classes
// ──────────────────────────────────────────────

enum class WifiBand { GHz2_4, GHz5, GHz6, UNKNOWN }
enum class CellBand { LOW_BAND, MID_BAND, HIGH_BAND, MMWAVE, UNKNOWN }

data class WifiSignalData(
    val rssiDbm: Int?,
    val frequencyMhz: Int?,
    val band: WifiBand,
    val qualityScore: Int,     // 0..100
    val qualityLabel: String,
    val bandLabel: String
)

data class CellularSignalData(
    val rssiDbm: Int?,
    val networkType: String,
    val frequencyBand: CellBand,
    val qualityScore: Int,     // 0..100
    val qualityLabel: String,
    val bandLabel: String
)

data class SignalComparisonResult(
    val wifi: WifiSignalData,
    val cellular: CellularSignalData,
    val wifiPenetrationScore: Float,
    val cellularPenetrationScore: Float,
    val comparisonHeadline: String,
    val comparisonExplanation: String
)

// ──────────────────────────────────────────────
// Analyzer
// ──────────────────────────────────────────────

class SignalStrengthAnalyzer(private val context: Context) {

    private val wifiManager: WifiManager by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    private val telephonyManager: TelephonyManager by lazy {
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    }

    fun analyze(): SignalComparisonResult {
        val wifi = readWifiSignal()
        val cellular = readCellularSignal()
        val wifiPen = calculatePenetrationScore(wifi.qualityScore, wifiPenetrationMultiplier(wifi.band))
        val cellPen = calculatePenetrationScore(cellular.qualityScore, cellPenetrationMultiplier(cellular.frequencyBand))
        val (headline, explanation) = generateComparisonText(wifi, cellular, wifiPen, cellPen)
        return SignalComparisonResult(wifi, cellular, wifiPen, cellPen, headline, explanation)
    }

    // ── WiFi ──────────────────────────────────

    fun readWifiSignal(): WifiSignalData {
        return try {
            val info = wifiManager.connectionInfo
            if (info == null || info.networkId == -1) {
                return noWifiSignal()
            }
            val rssi = info.rssi
            val freq = info.frequency
            val band = bandFromFrequency(freq)
            val score = normalizeWifiQuality(rssi)
            WifiSignalData(
                rssiDbm = rssi,
                frequencyMhz = freq,
                band = band,
                qualityScore = score,
                qualityLabel = qualityLabel(score),
                bandLabel = bandLabel(band)
            )
        } catch (e: SecurityException) {
            noWifiSignal()
        }
    }

    private fun noWifiSignal() = WifiSignalData(
        rssiDbm = null, frequencyMhz = null, band = WifiBand.UNKNOWN,
        qualityScore = 0, qualityLabel = "No Signal", bandLabel = "—"
    )

    private fun bandFromFrequency(freqMhz: Int): WifiBand = when {
        freqMhz < 2500  -> WifiBand.GHz2_4
        freqMhz < 5925  -> WifiBand.GHz5
        freqMhz > 5925  -> WifiBand.GHz6
        else            -> WifiBand.UNKNOWN
    }

    private fun bandLabel(band: WifiBand): String = when (band) {
        WifiBand.GHz2_4 -> "2.4 GHz"
        WifiBand.GHz5   -> "5 GHz"
        WifiBand.GHz6   -> "6 GHz"
        WifiBand.UNKNOWN -> "Unknown"
    }

    // ── Cellular ──────────────────────────────

    fun readCellularSignal(): CellularSignalData {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return noCellularSignal("Permission denied")

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return noCellularSignal("N/A")

        return try {
            val signalStrength = telephonyManager.signalStrength
                ?: return noCellularSignal("Unavailable")

            val cellSignals = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                signalStrength.cellSignalStrengths
            } else {
                emptyList()
            }

            if (cellSignals.isEmpty()) return noCellularSignal("Unavailable")

            // Priority: 5G NR → LTE → WCDMA → GSM
            val nrSignal = cellSignals.filterIsInstance<CellSignalStrengthNr>().firstOrNull()
            val lteSignal = cellSignals.filterIsInstance<CellSignalStrengthLte>().firstOrNull()
            val wcdmaSignal = cellSignals.filterIsInstance<CellSignalStrengthWcdma>().firstOrNull()
            val gsmSignal = cellSignals.filterIsInstance<CellSignalStrengthGsm>().firstOrNull()

            val networkTypeName = networkTypeName()

            when {
                nrSignal != null -> {
                    val dbm = nrSignal.dbm
                    if (dbm == Int.MAX_VALUE || dbm < -150) return noCellularSignal(networkTypeName)
                    val score = normalizeCellularQuality(dbm)
                    CellularSignalData(
                        rssiDbm = dbm,
                        networkType = "5G NR",
                        frequencyBand = CellBand.MID_BAND,
                        qualityScore = score,
                        qualityLabel = qualityLabel(score),
                        bandLabel = "5G"
                    )
                }
                lteSignal != null -> {
                    val dbm = lteSignal.dbm
                    if (dbm == Int.MAX_VALUE || dbm < -150) return noCellularSignal(networkTypeName)
                    val score = normalizeCellularQuality(dbm)
                    CellularSignalData(
                        rssiDbm = dbm,
                        networkType = "LTE",
                        frequencyBand = CellBand.MID_BAND,
                        qualityScore = score,
                        qualityLabel = qualityLabel(score),
                        bandLabel = "LTE"
                    )
                }
                wcdmaSignal != null -> {
                    val dbm = wcdmaSignal.dbm
                    if (dbm == Int.MAX_VALUE || dbm < -150) return noCellularSignal(networkTypeName)
                    val score = normalizeCellularQuality(dbm)
                    CellularSignalData(
                        rssiDbm = dbm,
                        networkType = "HSPA",
                        frequencyBand = CellBand.MID_BAND,
                        qualityScore = score,
                        qualityLabel = qualityLabel(score),
                        bandLabel = "3G"
                    )
                }
                gsmSignal != null -> {
                    val dbm = gsmSignal.dbm
                    if (dbm == Int.MAX_VALUE || dbm < -150) return noCellularSignal(networkTypeName)
                    val score = normalizeCellularQuality(dbm)
                    CellularSignalData(
                        rssiDbm = dbm,
                        networkType = "GSM",
                        frequencyBand = CellBand.LOW_BAND,
                        qualityScore = score,
                        qualityLabel = qualityLabel(score),
                        bandLabel = "2G"
                    )
                }
                else -> noCellularSignal("Unavailable")
            }
        } catch (e: SecurityException) {
            noCellularSignal("Permission denied")
        } catch (e: Exception) {
            noCellularSignal("Unavailable")
        }
    }

    private fun noCellularSignal(reason: String) = CellularSignalData(
        rssiDbm = null, networkType = reason, frequencyBand = CellBand.UNKNOWN,
        qualityScore = 0, qualityLabel = "N/A", bandLabel = "—"
    )

    private fun networkTypeName(): String {
        return try {
            when (telephonyManager.dataNetworkType) {
                TelephonyManager.NETWORK_TYPE_NR -> "5G NR"
                TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
                TelephonyManager.NETWORK_TYPE_HSPAP,
                TelephonyManager.NETWORK_TYPE_HSPA,
                TelephonyManager.NETWORK_TYPE_HSDPA,
                TelephonyManager.NETWORK_TYPE_HSUPA,
                TelephonyManager.NETWORK_TYPE_UMTS -> "HSPA"
                TelephonyManager.NETWORK_TYPE_GSM,
                TelephonyManager.NETWORK_TYPE_EDGE,
                TelephonyManager.NETWORK_TYPE_GPRS -> "GSM"
                else -> "Unknown"
            }
        } catch (e: SecurityException) {
            "Unknown"
        }
    }

    // ── Quality normalization ──────────────────

    fun normalizeWifiQuality(rssiDbm: Int): Int {
        // Practical WiFi range: -30 dBm (excellent) to -90 dBm (unusable)
        return ((rssiDbm + 90) / 60f * 100).roundToInt().coerceIn(0, 100)
    }

    fun normalizeCellularQuality(rssiDbm: Int): Int {
        // Practical cellular range: -50 dBm (excellent) to -110 dBm (very poor)
        return ((rssiDbm + 110) / 60f * 100).roundToInt().coerceIn(0, 100)
    }

    fun qualityLabel(score: Int): String = when {
        score >= 75 -> "Excellent"
        score >= 50 -> "Good"
        score >= 25 -> "Fair"
        score > 0   -> "Poor"
        else        -> "No Signal"
    }

    // ── Penetration scoring ────────────────────

    private fun wifiPenetrationMultiplier(band: WifiBand): Float = when (band) {
        WifiBand.GHz2_4  -> 1.00f
        WifiBand.GHz5    -> 0.60f
        WifiBand.GHz6    -> 0.40f
        WifiBand.UNKNOWN -> 0.70f
    }

    private fun cellPenetrationMultiplier(band: CellBand): Float = when (band) {
        CellBand.LOW_BAND  -> 1.00f
        CellBand.MID_BAND  -> 0.75f
        CellBand.HIGH_BAND -> 0.50f
        CellBand.MMWAVE    -> 0.20f
        CellBand.UNKNOWN   -> 0.75f
    }

    private fun calculatePenetrationScore(qualityScore: Int, multiplier: Float): Float {
        return (qualityScore.toFloat() * multiplier).coerceIn(0f, 100f)
    }

    // ── Comparison text ────────────────────────

    private fun generateComparisonText(
        wifi: WifiSignalData,
        cell: CellularSignalData,
        wifiPen: Float,
        cellPen: Float
    ): Pair<String, String> {
        val headline = when {
            wifi.rssiDbm == null && cell.rssiDbm == null ->
                "No signal detected on either network"
            wifi.rssiDbm == null ->
                "WiFi is off — cellular only"
            cell.rssiDbm == null ->
                "Cellular unavailable — WiFi only"
            else -> {
                val diff = abs(wifiPen - cellPen)
                val maxPen = max(wifiPen, cellPen)
                if (maxPen < 1f || diff < 5f) {
                    "WiFi and Cellular have similar penetration here"
                } else {
                    val winner = if (wifiPen >= cellPen) "WiFi" else "Cellular"
                    val loser = if (winner == "WiFi") "Cellular" else "WiFi"
                    val pct = (diff / maxPen * 100).roundToInt()
                    "$loser penetration is ~${pct}% lower than $winner"
                }
            }
        }

        val explanation = buildExplanation(wifi.band, cell.frequencyBand, wifi.rssiDbm, cell.rssiDbm)
        return Pair(headline, explanation)
    }

    private fun buildExplanation(
        wifiBand: WifiBand,
        cellBand: CellBand,
        wifiRssi: Int?,
        cellRssi: Int?
    ): String {
        if (wifiRssi == null && cellRssi == null) {
            return "Enable WiFi or cellular to compare signal penetration for your current location."
        }
        if (wifiRssi == null) {
            return "WiFi is off. Cellular signal is your only connection. Enable WiFi to compare."
        }
        if (cellRssi == null) {
            return "Cellular signal data is unavailable. Check that mobile data is enabled and permissions are granted."
        }

        return when (wifiBand) {
            WifiBand.GHz2_4 -> when (cellBand) {
                CellBand.LOW_BAND ->
                    "Both 2.4 GHz WiFi and low-band cellular penetrate walls well. " +
                    "Cellular low-band signals have a slight edge for deep indoor or multi-storey coverage."
                CellBand.MID_BAND ->
                    "2.4 GHz WiFi and mid-band cellular offer comparable wall penetration. " +
                    "Performance should be similar throughout most buildings."
                CellBand.HIGH_BAND, CellBand.MMWAVE ->
                    "2.4 GHz WiFi penetrates walls better than high-frequency cellular. " +
                    "WiFi is the more reliable option for through-wall coverage here."
                CellBand.UNKNOWN ->
                    "2.4 GHz WiFi offers good wall penetration. " +
                    "Cellular performance through walls depends on the frequency band your carrier is using."
            }
            WifiBand.GHz5 -> when (cellBand) {
                CellBand.LOW_BAND ->
                    "5 GHz WiFi weakens quickly through walls. Cellular low-band signals " +
                    "reach much further indoors — ideal for multi-storey buildings."
                else ->
                    "5 GHz WiFi has faster peak speeds but limited wall penetration. " +
                    "Move closer to your router or switch to 2.4 GHz for better indoor range."
            }
            WifiBand.GHz6 ->
                "6 GHz WiFi (Wi-Fi 6E) is very fast but has minimal wall penetration. " +
                "It works best in the same room as your router. Cellular is more reliable across rooms."
            WifiBand.UNKNOWN ->
                "Signal penetration depends on frequency band. Low-frequency signals travel " +
                "further through walls and floors than high-frequency ones."
        }
    }
}

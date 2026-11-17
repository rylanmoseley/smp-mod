package dev.rmoseley.smpmod.server

import dev.rmoseley.smpmod.SmpMod
import dev.rmoseley.smpmod.config.SmpConfig
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture

/**
 * Asynchronous Discord webhook dispatcher for realm milestones, town expansions,
 * cooperative raid victories, public salvage auctions, and civil crimes.
 * Uses native Java 21 HttpClient for zero-dependency, non-blocking network I/O.
 */
object DiscordWebhookDispatcher {

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    /**
     * Sends an embedded notification to the configured Discord webhook.
     */
    fun sendEmbed(
        title: String,
        description: String,
        color: Int = 0x5865F2,
        fields: List<Pair<String, String>> = emptyList(),
        footerText: String = SmpConfig.COMMON.discordServerName.get()
    ) {
        val webhookUrl = SmpConfig.COMMON.discordWebhookUrl.get()
        if (webhookUrl.isBlank() || !webhookUrl.startsWith("https://")) {
            return
        }

        CompletableFuture.runAsync {
            try {
                val jsonPayload = buildEmbedJson(title, description, color, fields, footerText)
                val request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(8))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build()

                httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .thenAccept { response ->
                        if (response.statusCode() !in 200..299) {
                            SmpMod.LOGGER.debug("Discord webhook response: ${response.statusCode()}")
                        }
                    }
                    .exceptionally { ex ->
                        SmpMod.LOGGER.debug("Failed to dispatch Discord webhook: ${ex.message}")
                        null
                    }
            } catch (e: Exception) {
                SmpMod.LOGGER.debug("Error creating Discord webhook request: ${e.message}")
            }
        }
    }

    // =========================================================================
    // Specialized Realm Event Dispatchers
    // =========================================================================

    fun notifyTownMilestone(townTierName: String, ownerName: String, chunkCount: Int) {
        if (!SmpConfig.COMMON.discordNotifyTownMilestones.get()) return

        sendEmbed(
            title = "🏛️ Settlement Expansion: $townTierName Established!",
            description = "**$ownerName** has expanded their contiguous settlement to **$chunkCount chunks**, reaching **$townTierName** tier!",
            color = 0xF1C40F, // Gold
            fields = listOf(
                Pair("Founder", ownerName),
                Pair("Contiguous Territory", "$chunkCount chunks"),
                Pair("Municipal Status", "Civic Upgrades & Defense Perks Unlocked")
            )
        )
    }

    fun notifyDragonDefeated(playerNames: List<String>) {
        if (!SmpConfig.COMMON.discordNotifyDragonVictories.get()) return

        val partyList = playerNames.joinToString(", ")
        sendEmbed(
            title = "🐲 Realm Victory: Ender Dragon Slain!",
            description = "The cooperative raid team has vanquished the **Ender Dragon**!\n\n**Heroic Raiders:** $partyList",
            color = 0x9B59B6, // Purple
            fields = listOf(
                Pair("Raid Party Size", "${playerNames.size} adventurers"),
                Pair("Outcome", "Realm Sanctified & Dragon Gate Opened")
            )
        )
    }

    fun notifyPublicSalvage(itemName: String, originalOwner: String, buybackPrice: Long) {
        if (!SmpConfig.COMMON.discordNotifyPublicSalvage.get()) return

        sendEmbed(
            title = "⚖️ Public Salvage Auction: Unclaimed Gear Available",
            description = "An unclaimed gear piece has exceeded its 24-hour exclusive window and is now listed on the **Public Salvage Market** at 1.25x markup!",
            color = 0x1ABC9C, // Teal
            fields = listOf(
                Pair("Item", itemName),
                Pair("Original Owner", originalOwner),
                Pair("Public Buyback Price", "$buybackPrice coins"),
                Pair("In-Game Command", "`/buyback public`")
            )
        )
    }

    fun notifyVillagerCrime(killerName: String, killCount: Int) {
        if (!SmpConfig.COMMON.discordNotifyVillagerCrimes.get()) return

        sendEmbed(
            title = "🩸 Crime Against Civilization: Villager Slain!",
            description = "**$killerName** has murdered an innocent merchant! Total Villager Kills: **$killCount**.\n\nThey have been branded a **Villager Slayer** realm-wide with price penalties and the Curse of Unluck.",
            color = 0xE74C3C, // Red
            fields = listOf(
                Pair("Perpetrator", killerName),
                Pair("Lifetime Kills", "$killCount"),
                Pair("Realm Penalty", "+${(killCount * 5).coerceAtMost(30)} emerald trade surcharge")
            )
        )
    }

    fun notifyAdvancement(playerName: String, advancementTitle: String, tierName: String, rewards: String) {
        if (!SmpConfig.COMMON.discordNotifyAdvancements.get()) return

        sendEmbed(
            title = "🏆 Realm Milestone: $advancementTitle",
            description = "**$playerName** has unlocked the **$tierName** milestone: **$advancementTitle**!",
            color = 0x3498DB, // Blue
            fields = if (rewards.isNotBlank()) listOf(Pair("Rewards Distributed", rewards)) else emptyList()
        )
    }

    // =========================================================================
    // JSON Construction Helpers
    // =========================================================================

    private fun buildEmbedJson(
        title: String,
        description: String,
        color: Int,
        fields: List<Pair<String, String>>,
        footerText: String
    ): String {
        val sb = StringBuilder()
        sb.append("{")
        sb.append("\"embeds\":[{")
        sb.append("\"title\":${escapeJson(title)},")
        sb.append("\"description\":${escapeJson(description)},")
        sb.append("\"color\":$color,")
        sb.append("\"timestamp\":\"${Instant.now()}\",")

        if (fields.isNotEmpty()) {
            sb.append("\"fields\":[")
            fields.forEachIndexed { i, (name, value) ->
                sb.append("{")
                sb.append("\"name\":${escapeJson(name)},")
                sb.append("\"value\":${escapeJson(value)},")
                sb.append("\"inline\":true")
                sb.append("}")
                if (i < fields.size - 1) sb.append(",")
            }
            sb.append("],")
        }

        sb.append("\"footer\":{\"text\":${escapeJson(footerText)}}")
        sb.append("}]")
        sb.append("}")
        return sb.toString()
    }

    private fun escapeJson(input: String): String {
        val escaped = input
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\u000C", "\\f")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return "\"$escaped\""
    }
}

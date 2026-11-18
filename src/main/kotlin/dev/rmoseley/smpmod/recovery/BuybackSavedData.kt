package dev.rmoseley.smpmod.recovery

import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.saveddata.SavedData
import org.apache.logging.log4j.LogManager

/**
 * World-level SavedData persistence layer for the Ownership-Bound UUID Buyback Ledger.
 * Ensures pending recovery buybacks, escalating death fees, and item provenance
 * survive server restarts, crashes, and automated updates.
 *
 * Saves to world/data/smpmod_buyback_ledger.dat in the Overworld storage directory.
 */
class BuybackSavedData : SavedData() {

    companion object {
        private val LOGGER = LogManager.getLogger("smpmod-buyback-data")
        const val FILE_ID: String = "smpmod_buyback_ledger"
        private var activeInstance: BuybackSavedData? = null

        fun factory(): Factory<BuybackSavedData> {
            return Factory(
                { BuybackSavedData() },
                { tag, provider ->
                    val data = BuybackSavedData()
                    data.load(tag, provider)
                    data
                }
            )
        }

        fun init(level: ServerLevel): BuybackSavedData {
            val overworld = level.server.overworld()
            val data = overworld.dataStorage.computeIfAbsent(factory(), FILE_ID)
            activeInstance = data
            LOGGER.info("BuybackSavedData initialized for Overworld. Loaded ${BuybackLedgerManager.getAllEntries().size} active entries.")
            return data
        }

        fun getInstance(): BuybackSavedData? = activeInstance

        fun markDirty() {
            activeInstance?.setDirty()
        }
    }

    override fun save(tag: CompoundTag, registries: HolderLookup.Provider): CompoundTag {
        val listTag = ListTag()
        val allEntries = BuybackLedgerManager.getAllEntries()
        LOGGER.info("Persisting ${allEntries.size} buyback ledger entries to $FILE_ID.dat...")

        for (entry in allEntries) {
            val entryTag = CompoundTag()
            entryTag.putUUID("itemUuid", entry.itemUuid)
            entryTag.putUUID("registeredOwnerUuid", entry.registeredOwnerUuid)
            entryTag.putString("registeredOwnerName", entry.registeredOwnerName)
            entryTag.putInt("deathCount", entry.deathCount)
            entryTag.putLong("deathTimestampEpochSeconds", entry.deathTimestampEpochSeconds)
            entryTag.putLong("baseCost", entry.baseCost)
            entryTag.putDouble("catalystDiscount", entry.catalystDiscount)

            val itemTag = CompoundTag()
            entry.itemStack.save(registries, itemTag)
            entryTag.put("itemStack", itemTag)

            listTag.add(entryTag)
        }

        tag.put("entries", listTag)
        return tag
    }

    fun load(tag: CompoundTag, registries: HolderLookup.Provider) {
        val listTag = tag.getList("entries", Tag.TAG_COMPOUND.toInt())
        val entries = mutableListOf<BuybackLedgerManager.BuybackEntry>()
        LOGGER.info("Deserializing ${listTag.size} buyback ledger entries from $FILE_ID.dat...")

        for (i in 0 until listTag.size) {
            val entryTag = listTag.getCompound(i)
            if (entryTag.hasUUID("itemUuid") && entryTag.hasUUID("registeredOwnerUuid")) {
                val itemUuid = entryTag.getUUID("itemUuid")
                val ownerUuid = entryTag.getUUID("registeredOwnerUuid")
                val ownerName = entryTag.getString("registeredOwnerName")
                val deathCount = entryTag.getInt("deathCount")
                val deathTimestamp = entryTag.getLong("deathTimestampEpochSeconds")
                val baseCost = entryTag.getLong("baseCost")
                val discount = entryTag.getDouble("catalystDiscount")

                val itemCompound = entryTag.getCompound("itemStack")
                val itemStack = ItemStack.parseOptional(registries, itemCompound)

                if (!itemStack.isEmpty) {
                    entries.add(
                        BuybackLedgerManager.BuybackEntry(
                            itemUuid = itemUuid,
                            itemStack = itemStack,
                            registeredOwnerUuid = ownerUuid,
                            registeredOwnerName = ownerName,
                            deathCount = deathCount,
                            deathTimestampEpochSeconds = deathTimestamp,
                            baseCost = baseCost,
                            catalystDiscount = discount
                        )
                    )
                } else {
                    LOGGER.warn("Skipping corrupt or unparseable item stack for buyback UUID: $itemUuid")
                }
            }
        }

        BuybackLedgerManager.loadEntries(entries)
    }
}

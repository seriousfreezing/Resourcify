/*
 * This file is part of Resourcify
 * Copyright (C) 2023 DeDiamondPro
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License Version 3 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.dediamondpro.resourcify.platform

import com.google.common.collect.Lists
import dev.dediamondpro.resourcify.mixins.AbstractResourcePackAccessor
import gg.essential.universal.UMinecraft
import net.minecraft.client.resources.AbstractResourcePack
import net.minecraftforge.common.ForgeVersion
import java.io.File

object Platform {
    fun getMcVersion(): String {
        return ForgeVersion.mcVersion
    }

    fun getSelectedResourcePacks(): List<File> {
        return UMinecraft.getMinecraft().resourcePackRepository.repositoryEntries.mapNotNull {
            if (it.resourcePack !is AbstractResourcePack) return@mapNotNull null
            (it.resourcePack as AbstractResourcePackAccessor).resourcePackFile
        }
    }

    fun reloadResources() {
        UMinecraft.getMinecraft().refreshResources()
    }

    fun closeResourcePack(pack: File) {
        UMinecraft.getMinecraft().resourcePackRepository.repositoryEntriesAll.firstOrNull {
            if (it.resourcePack !is AbstractResourcePack) return@firstOrNull false
            (it.resourcePack as AbstractResourcePackAccessor).resourcePackFile == pack
        }?.closeResourcePack()
    }

    fun replaceResourcePack(oldPack: File, newPack: File) {
        val repo = UMinecraft.getMinecraft().resourcePackRepository
        repo.updateRepositoryEntriesAll()
        val packs = Lists.newArrayList(repo.repositoryEntries)
        val old = repo.repositoryEntriesAll.firstOrNull {
            if (it.resourcePack !is AbstractResourcePack) return@firstOrNull false
            (it.resourcePack as AbstractResourcePackAccessor).resourcePackFile == oldPack
        }
        old?.let {
            packs.remove(it)
            it.closeResourcePack()
        }
        packs.add(repo.repositoryEntriesAll.firstOrNull {
            if (it.resourcePack !is AbstractResourcePack) return@firstOrNull false
            (it.resourcePack as AbstractResourcePackAccessor).resourcePackFile == newPack
        } ?: return)
        repo.setRepositories(packs)
        UMinecraft.getMinecraft().gameSettings.saveOptions()
    }
}
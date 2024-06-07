/*
 * This file is part of Resourcify
 * Copyright (C) 2024 DeDiamondPro
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

package dev.dediamondpro.resourcify.services

import dev.dediamondpro.minemark.elementa.style.MarkdownStyle
import dev.dediamondpro.resourcify.services.ads.DefaultAdProvider
import dev.dediamondpro.resourcify.services.ads.IAdProvider
import dev.dediamondpro.resourcify.util.ElementaUtils
import java.util.concurrent.CompletableFuture

interface IService {
    fun getName(): String

    fun search(
        query: String,
        sortBy: String,
        minecraftVersions: List<String>,
        categories: List<String>,
        offset: Int,
        type: ProjectType
    ): ISearchData?

    fun getMinecraftVersions(): CompletableFuture<Map<String, String>>

    fun canSelectMultipleMinecraftVersions(): Boolean = true

    /**
     * The categories supported by the service for a given project type
     * Key = id
     * Value = display name
     */
    fun getCategories(type: ProjectType): CompletableFuture<Map<String, Map<String, String>>>

    /**
     * The search options supported by the service
     * Key = id
     * Value = display name
     */
    fun getSortOptions(): Map<String, String>

    fun getAdProvider(): IAdProvider = DefaultAdProvider

    fun getMarkdownStyle(): MarkdownStyle = ElementaUtils.defaultMarkdownStyle
}
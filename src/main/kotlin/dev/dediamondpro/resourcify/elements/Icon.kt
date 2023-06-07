/*
 * Copyright (C) 2023 DeDiamondPro. - All Rights Reserved
 */

package dev.dediamondpro.resourcify.elements

import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIImage
import gg.essential.elementa.constraints.ColorConstraint
import gg.essential.elementa.dsl.*
import gg.essential.elementa.utils.ResourceCache
import java.awt.Color

class Icon(icon: String, shadow: Boolean, color: ColorConstraint) : UIContainer() {
    constructor(icon: String, shadow: Boolean, color: Color = Color.WHITE) : this(icon, shadow, color.toConstraint())

    init {
        (if (shadow) ShadowImage(icon, iconCache, color)
        else UIImage.ofResourceCached(icon, iconCache)).constrain {
            x = 0.pixels()
            y = 0.pixels()
            width = 100.percent()
            height = 100.percent()
            this.color = color
        } childOf this
    }

    companion object {
        private val iconCache = ResourceCache()
    }
}
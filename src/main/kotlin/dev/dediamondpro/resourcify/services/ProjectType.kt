package dev.dediamondpro.resourcify.services

import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.XConstraint
import gg.essential.elementa.constraints.YConstraint
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.dsl.plus

//#if MC >= 11600
//$$ import dev.dediamondpro.resourcify.mixins.PackScreenAccessor
//$$ import net.minecraft.client.gui.screen.Screen
//$$ import java.io.File
//#endif

enum class ProjectType(
    val displayName: String,
    val plusX: XConstraint = CenterConstraint()
            //#if MC >= 12005
            //$$ + 205.pixels(),
            //#else
            + 194.pixels(),
    //#endif
    val plusY: YConstraint = 10.pixels(),
    val hasUpdateButton: Boolean = true
) {
    RESOURCE_PACK("resourcify.type.resource_packs"),
    // 1.8.9 only
    AYCY_RESOURCE_PACK("resourcify.type.resource_packs", plusX = 10.pixels(true)),
    DATA_PACK("resourcify.type.data_packs", hasUpdateButton = false),
    IRIS_SHADER("resourcify.type.shaders", CenterConstraint() + 144.pixels(), 6.pixels()),
    OPTIFINE_SHADER("resourcify.type.shaders", plusX = 10.pixels(true));

    //#if MC >= 11600
    //$$ fun getDirectory(screen: Screen): File {
    //$$     return when(this) {
    //$$         //#if MC < 11904
    //$$         RESOURCE_PACK -> (screen as PackScreenAccessor).directory
    //$$         DATA_PACK -> (screen as PackScreenAccessor).directory
    //$$         //#else
    //$$         //$$ RESOURCE_PACK -> (screen as PackScreenAccessor).directory.toFile()
    //$$         //$$ DATA_PACK -> (screen as PackScreenAccessor).directory.toFile()
    //$$         //#endif
    //$$         IRIS_SHADER -> File("./shaderpacks")
    //$$         OPTIFINE_SHADER -> File("./shaderpacks")
    //$$         else -> error("Unknown project type: $this")
    //$$     }
    //$$ }
    //#endif
}
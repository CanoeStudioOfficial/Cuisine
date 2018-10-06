package snownee.cuisine.client.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverride;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.client.model.PerspectiveMapWrapper;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.common.util.Constants;
import snownee.cuisine.util.ItemNBTUtil;

import javax.annotation.Nullable;
import javax.vecmath.Vector3f;
import java.util.Collections;

public class ChoppingBoardOverride extends ItemOverrideList
{
    /*
     * The correct TRSRTransformation data for correctly rendering a chopping board.
     * The core data is cited from Connected Texture Mod (CTM) with permission from
     * tterrag. In addition to those data, we do an additional transform to scale
     * in order to make them look like a "board". We also call blockCenterToCorner
     * to make sure everything is sane for an ItemBlock.
     *
     * Permission:
     * http://tritusk.info/pics/tterrag-permission-default-block-transform-data.jpg
     *
     * Reference:
     * https://github.com/Chisel-Team/ConnectedTexturesMod/blob/1.10/dev/src/main/
     * java/team/chisel/ctm/client/model/AbstractCTMBakedModel.java#L245-L253
     *
     * TODO: Awaiting bs2609's Pull Request (GitHub MinecraftForge/MinecraftForge#5180)
     */
    private static final ImmutableMap<ItemCameraTransforms.TransformType, TRSRTransformation> CHOPPING_BOARD_TRANSFORMS = ImmutableMap.<ItemCameraTransforms.TransformType, TRSRTransformation>builder()
            .put(ItemCameraTransforms.TransformType.GUI, TRSRTransformation.blockCenterToCorner(
                    of(0, 0, 0, 30, 45, 0, 0.625f)
                            .compose(new TRSRTransformation(new Vector3f(0F, -.4F, 0F), null, new Vector3f(0.75F, 0.25F, 0.75F), null))
            ))
            .put(ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND, TRSRTransformation.blockCenterToCorner(
                    of(0, 2.5f, 0, 75, 45, 0, 0.375f)
                            .compose(new TRSRTransformation(new Vector3f(0F, -.4F, 0F), null, new Vector3f(0.75F, 0.25F, 0.75F), null))
            ))
            .put(ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND, TRSRTransformation.blockCenterToCorner(
                    of(0, 2.5f, 0, 75, 45, 0, 0.375f)
                            .compose(new TRSRTransformation(new Vector3f(0F, -.4F, 0F), null, new Vector3f(0.75F, 0.25F, 0.75F), null))
            ))
            .put(ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND, TRSRTransformation.blockCenterToCorner(
                    of(0, 0, 0, 0, 45, 0, 0.4f)
                            .compose(new TRSRTransformation(new Vector3f(0F, -.4F, 0F), null, new Vector3f(0.75F, 0.25F, 0.75F), null))
            ))
            .put(ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND, TRSRTransformation.blockCenterToCorner(
                    of(0, 0, 0, 0, 225, 0, 0.4f)
                            .compose(new TRSRTransformation(new Vector3f(0F, -.4F, 0F), null, new Vector3f(0.75F, 0.25F, 0.75F), null))
            ))
            .put(ItemCameraTransforms.TransformType.GROUND, TRSRTransformation.blockCenterToCorner(
                    of(0, 2, 0, 0, 0, 0, 0.25f)
                            .compose(new TRSRTransformation(new Vector3f(0F, -.4F, 0F), null, new Vector3f(0.75F, 0.25F, 0.75F), null))
            ))
            .put(ItemCameraTransforms.TransformType.FIXED, TRSRTransformation.blockCenterToCorner(
                    of(0, 0, 0, 0, 0, 0, 0.5f)
                            .compose(new TRSRTransformation(new Vector3f(0F, -.4F, 0F), null, new Vector3f(0.75F, 0.25F, 0.75F), null))
            ))
            .build();

    /**
     * Get a {@code TRSRTransformation} object using supplied data. Only uniform scale is supported.
     * This is NOT a general purpose short-cut for getting a TRSRTransformation! It's here for mere
     * purpose of being a human-readable shortcut.
     *
     * @param tx Translation x in pixel
     * @param ty Translation y in pixel
     * @param tz Translation z in pixel
     * @param ax Angle x in degree
     * @param ay Angle y in degree
     * @param az Angle z in degree
     * @param scale Uniform scale quantity
     *
     * @return The correct TRSRTransformation object
     */
    private static TRSRTransformation of(float tx, float ty, float tz, float ax, float ay, float az, float scale)
    {
        return new TRSRTransformation(
                new Vector3f(tx / 16, ty / 16, tz / 16), // translation
                TRSRTransformation.quatFromXYZDegrees(new Vector3f(ax, ay, az)), // left-rotation
                new Vector3f(scale, scale, scale), // uniform scale
                null // we don't do right rotation here
        );
    }

    ChoppingBoardOverride()
    {
        super(Collections.emptyList());
    }

    @Override
    public IBakedModel handleItemState(IBakedModel originalModel, ItemStack stack, @Nullable World world, @Nullable EntityLivingBase entity)
    {
        NBTTagCompound tag = ItemNBTUtil.getCompound(stack, "BlockEntityTag", true);
        IBlockState cover;
        if (tag != null && tag.hasKey("cover", Constants.NBT.TAG_COMPOUND))
        {
            ItemStack coverData = new ItemStack(tag.getCompoundTag("cover"));
            cover = Block.getBlockFromItem(coverData.getItem()).getStateFromMeta(coverData.getMetadata());
        }
        else
        {
            cover = Blocks.LOG.getDefaultState(); // Oak wood, with growth ring facing up
        }
        return new PerspectiveMapWrapper(Minecraft.getMinecraft().getBlockRendererDispatcher().getModelForState(cover), CHOPPING_BOARD_TRANSFORMS);

    }

    @Override
    public ImmutableList<ItemOverride> getOverrides()
    {
        return ImmutableList.of();
    }
}

package snownee.cuisine.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;
import snownee.cuisine.api.Ingredient;
import snownee.cuisine.client.CulinaryRenderHelper;
import snownee.cuisine.fluids.CuisineFluids;
import snownee.cuisine.tiles.utensils.TileWok;
import snownee.cuisine.tiles.utensils.TileWok.SeasoningInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class TESRWok extends TESRHeatable<TileWok>
{
    @Override
    public void render(TileWok tile, double x, double y, double z, float partialTicks, int destroyStage, float alpha)
    {
        super.render(tile, x, y, z, partialTicks, destroyStage, alpha);
        Minecraft mc = Minecraft.getMinecraft();
        if (y > mc.player.eyeHeight)
        {
            return;
        }

        GlStateManager.pushMatrix();
        GlStateManager.enableAlpha();
        GlStateManager.translate(x, y, z);

        Map<Ingredient, ItemStack> contents = tile.getWokContents();
        if (!contents.isEmpty())
        {
            GlStateManager.pushMatrix();
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableLighting();

            GlStateManager.translate(0.5, 0.1, 0.5);
            int count = 0;

            for (Entry<Ingredient, ItemStack> entry : contents.entrySet())
            {
                GlStateManager.pushMatrix();
                ItemStack stack = entry.getValue();
                int seed = stack.hashCode() + tile.actionCycle * 439;

                GlStateManager.scale(0.5, 0.5, 0.5);
                GlStateManager.translate(((seed % 100) - 50) / 150D, 0.1 + count / 1000D, ((seed % 56) - 28) / 84D);
                GlStateManager.rotate(360 * (seed % 943) / 943F, 0, 1, 0);
                GlStateManager.rotate(90, 1, 0, 0);

                RenderHelper.enableStandardItemLighting();
                CulinaryRenderHelper.renderIngredient(mc, stack, entry.getKey().getDoneness());
                RenderHelper.disableStandardItemLighting();
                GlStateManager.popMatrix();

                count++;
            }
            GlStateManager.popMatrix();
        }
        SeasoningInfo seasoningInfo = tile.seasoningInfo;
        if (seasoningInfo != null && seasoningInfo.volume > 0)
        {
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.pushMatrix();
            RenderHelper.disableStandardItemLighting();
            mc.renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
            TextureAtlasSprite still = mc.getTextureMapBlocks().getTextureExtry(CuisineFluids.JUICE.getStill().toString());
            if (still == null)
            {
                still = mc.getTextureMapBlocks().getMissingSprite();
            }

            int brightness = mc.world.getCombinedLight(tile.getPos(), CuisineFluids.JUICE.getLuminosity());
            int lx = brightness >> 0x10 & 0xFFFF;
            int ly = brightness & 0xFFFF;

            int a = seasoningInfo.color >> 24 & 0xFF;
            int r = seasoningInfo.color >> 16 & 0xFF;
            int g = seasoningInfo.color >> 8 & 0xFF;
            int b = seasoningInfo.color & 0xFF;

            double height = 0.35 + MathHelper.clamp(seasoningInfo.volume, 1, 10) * 200 / 18000D;

            buffer.pos(0.0625, height, 0.0625).color(r, g, b, a).tex(still.getMinU(), still.getMinV()).lightmap(lx, ly).endVertex();
            buffer.pos(0.0625, height, 0.9375).color(r, g, b, a).tex(still.getMinU(), still.getMaxV()).lightmap(lx, ly).endVertex();
            buffer.pos(0.9375, height, 0.9375).color(r, g, b, a).tex(still.getMaxU(), still.getMaxV()).lightmap(lx, ly).endVertex();
            buffer.pos(0.9375, height, 0.0625).color(r, g, b, a).tex(still.getMaxU(), still.getMinV()).lightmap(lx, ly).endVertex();

            tessellator.draw();
            RenderHelper.enableStandardItemLighting();
            GlStateManager.popMatrix();
        }
        GlStateManager.popMatrix();
    }

    @Override
    protected List<IngredientInfo> getIngredientInfo(TileWok tile)
    {
        Map<Ingredient, ItemStack> contents = tile.getWokContents();
        List<IngredientInfo> infos = new ArrayList<>(contents.size());
        for (Entry<Ingredient, ItemStack> entry : contents.entrySet())
        {
            infos.add(new IngredientInfo(entry.getValue(), entry.getKey().getDoneness()));
        }
        return infos;
    }
}

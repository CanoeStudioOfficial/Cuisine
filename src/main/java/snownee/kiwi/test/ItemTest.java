package snownee.kiwi.test;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import snownee.kiwi.client.AdvancedFontRenderer;
import snownee.kiwi.item.ItemMod;
import snownee.kiwi.util.NBTHelper;

public class ItemTest extends ItemMod
{
    public ItemTest(String name)
    {
        super(name);
    }

    @Override
    public FontRenderer getFontRenderer(ItemStack stack)
    {
        return AdvancedFontRenderer.INSTANCE;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn)
    {
        ItemStack stack = playerIn.getHeldItem(handIn);
        NBTTagCompound tag = NBTHelper.of(stack).setInt("Fluid.Amount", 1000).getTag("Fluid");
        System.out.println(tag);
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }
}

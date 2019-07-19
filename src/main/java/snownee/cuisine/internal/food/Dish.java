package snownee.cuisine.internal.food;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import snownee.cuisine.Cuisine;
import snownee.cuisine.CuisineConfig;
import snownee.cuisine.CuisineRegistry;
import snownee.cuisine.api.CompositeFood;
import snownee.cuisine.api.CookingVessel;
import snownee.cuisine.api.CulinaryHub;
import snownee.cuisine.api.Effect;
import snownee.cuisine.api.EffectCollector;
import snownee.cuisine.api.Form;
import snownee.cuisine.api.Ingredient;
import snownee.cuisine.api.IngredientTrait;
import snownee.cuisine.api.Material;
import snownee.cuisine.api.MaterialCategory;
import snownee.cuisine.api.Seasoning;
import snownee.cuisine.api.Spice;
import snownee.cuisine.api.prefab.DefaultCookedCollector;
import snownee.cuisine.api.util.SkillUtil;
import snownee.cuisine.internal.CuisinePersistenceCenter;
import snownee.cuisine.internal.CuisineSharedSecrets;
import snownee.kiwi.util.NBTHelper;

/**
 * Dish represents a specific food preparation which are made from combination of
 * various ingredients and seasonings. Typically, a cookware like wok is capable
 * to produce an instance of this.
 */
public class Dish extends CompositeFood
{
    public static final ResourceLocation DISH_ID = new ResourceLocation(Cuisine.MODID, "dish");

    private String modelType;

    public Dish(List<Ingredient> ingredients, List<Seasoning> seasonings, List<Effect> effects, int hungerHeal, float saturation)
    {
        super(ingredients, seasonings, effects, hungerHeal, saturation);
    }

    @Override
    public ResourceLocation getIdentifier()
    {
        return DISH_ID;
    }

    @Override
    public Collection<String> getKeywords()
    {
        // TODO Anything... else?
        return Arrays.asList("east-asian", "wok");
    }

    @Override
    public String getOrComputeModelType()
    {
        if (this.modelType != null)
        {
            return this.modelType;
        }

        if (ingredients.stream().anyMatch(i -> i.getMaterial().isUnderCategoryOf(MaterialCategory.FISH)))
        {
            this.modelType = "fish0";
        }
        else if (ingredients.stream().anyMatch(i -> i.getMaterial() == CulinaryHub.CommonMaterials.RICE))
        {
            this.modelType = "rice0";
        }
        else if (ingredients.stream().allMatch(i -> i.getMaterial().isUnderCategoryOf(MaterialCategory.MEAT)))
        {
            this.modelType = Math.random() >= 0.5 ? "meat1" : "meat0";
        }
        else if (ingredients.stream().allMatch(i -> i.getMaterial().isUnderCategoryOf(MaterialCategory.VEGETABLES)))
        {
            this.modelType = Math.random() >= 0.5 ? "veges0" : "veges1";
        }
        else
        {
            this.modelType = Math.random() >= 0.5 ? "mixed0" : "mixed1";
        }

        return this.modelType;
    }

    @Override
    public void setModelType(String type)
    {
        this.modelType = type;
    }

    @Override
    public ItemStack getBaseItem()
    {
        return new ItemStack(CuisineRegistry.DISH);
    }

    @Override
    public void onEaten(ItemStack stack, World worldIn, EntityPlayer player)
    {
        super.onEaten(stack, worldIn, player);

        if (!worldIn.isRemote && CuisineConfig.HARDCORE.enable && CuisineConfig.HARDCORE.badSkillPunishment)
        {
            int countPlain = (int) getIngredients().stream().filter(i -> i.getAllTraits().contains(IngredientTrait.PLAIN) || i.getAllTraits().contains(IngredientTrait.UNDERCOOKED)).count();
            int countOvercooked = (int) getIngredients().stream().filter(i -> i.getAllTraits().contains(IngredientTrait.OVERCOOKED)).count();

            if (countPlain / (float) getIngredients().size() > 0.8F)
            {
                Potion potion = worldIn.rand.nextBoolean() ? MobEffects.MINING_FATIGUE : MobEffects.WEAKNESS;
                player.addPotionEffect(new PotionEffect(potion, 300 * getIngredients().size()));
            }
            if (countOvercooked / (float) getIngredients().size() > 0.8F)
            {
                Potion potion = worldIn.rand.nextBoolean() ? MobEffects.POISON : MobEffects.NAUSEA;
                player.addPotionEffect(new PotionEffect(potion, 100 * getIngredients().size()));
            }
        }
    }

    public static NBTTagCompound serialize(Dish dish)
    {
        NBTTagCompound data = new NBTTagCompound();
        NBTTagList ingredientList = new NBTTagList();

        for (Ingredient ingredient : dish.ingredients)
        {
            ingredientList.appendTag(CuisinePersistenceCenter.serialize(ingredient));
        }
        data.setTag(CuisineSharedSecrets.KEY_INGREDIENT_LIST, ingredientList);

        NBTTagList seasoningList = new NBTTagList();
        for (Seasoning seasoning : dish.seasonings)
        {
            seasoningList.appendTag(CuisinePersistenceCenter.serialize(seasoning));
        }
        data.setTag(CuisineSharedSecrets.KEY_SEASONING_LIST, seasoningList);

        NBTTagList effectList = new NBTTagList();
        for (Effect effect : dish.effects)
        {
            effectList.appendTag(new NBTTagString(effect.getID()));
        }
        data.setTag(CuisineSharedSecrets.KEY_EFFECT_LIST, effectList);

        String modelType = dish.getOrComputeModelType();
        if (modelType != null)
        {
            data.setString("type", modelType);
        }

        data.setInteger(CuisineSharedSecrets.KEY_FOOD_LEVEL, dish.getFoodLevel());
        data.setFloat(CuisineSharedSecrets.KEY_SATURATION_MODIFIER, dish.getSaturationModifier());
        data.setInteger(CuisineSharedSecrets.KEY_SERVES, dish.getServes());
        data.setInteger(CuisineSharedSecrets.KEY_MAX_SERVES, dish.getMaxServes());
        data.setFloat(CuisineSharedSecrets.KEY_USE_DURATION, dish.getUseDurationModifier());
        return data;
    }

    public static Dish deserialize(NBTTagCompound data)
    {
        NBTHelper helper = NBTHelper.of(data);
        ArrayList<Ingredient> ingredients = new ArrayList<>();
        ArrayList<Seasoning> seasonings = new ArrayList<>();
        ArrayList<Effect> effects = new ArrayList<>();
        NBTTagList ingredientList = data.getTagList(CuisineSharedSecrets.KEY_INGREDIENT_LIST, Constants.NBT.TAG_COMPOUND);
        for (NBTBase baseTag : ingredientList)
        {
            if (baseTag.getId() == Constants.NBT.TAG_COMPOUND)
            {
                ingredients.add(CuisinePersistenceCenter.deserializeIngredient((NBTTagCompound) baseTag));
            }
        }

        NBTTagList seasoningList = data.getTagList(CuisineSharedSecrets.KEY_SEASONING_LIST, Constants.NBT.TAG_COMPOUND);
        for (NBTBase baseTag : seasoningList)
        {
            if (baseTag.getId() == Constants.NBT.TAG_COMPOUND)
            {
                seasonings.add(CuisinePersistenceCenter.deserializeSeasoning((NBTTagCompound) baseTag));
            }
        }

        NBTTagList effectList = data.getTagList(CuisineSharedSecrets.KEY_EFFECT_LIST, Constants.NBT.TAG_STRING);
        for (NBTBase baseTag : effectList)
        {
            if (baseTag.getId() == Constants.NBT.TAG_STRING)
            {
                effects.add(CulinaryHub.API_INSTANCE.findEffect(((NBTTagString) baseTag).getString()));
            }
        }

        int serves = helper.getInt(CuisineSharedSecrets.KEY_SERVES);
        int maxServes = helper.getInt(CuisineSharedSecrets.KEY_MAX_SERVES);
        float duration = helper.getFloat(CuisineSharedSecrets.KEY_USE_DURATION, 1);
        int foodLevel = helper.getInt(CuisineSharedSecrets.KEY_FOOD_LEVEL);
        float saturation = helper.getFloat(CuisineSharedSecrets.KEY_SATURATION_MODIFIER);

        Dish dish = new Dish(ingredients, seasonings, effects, foodLevel, saturation);
        dish.setMaxServes(maxServes);
        dish.setServes(serves);
        dish.setUseDurationModifier(duration);

        dish.setModelType(helper.getString("type"));

        return dish;
    }

    public static final class Builder extends CompositeFood.Builder<Dish>
    {
        private Dish completed;
        private int water, oil, temperature;
        private Random rand = new Random();

        private Builder()
        {
            this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

        Builder(List<Ingredient> ingredients, List<Seasoning> seasonings, List<Effect> effects)
        {
            super(ingredients, seasonings, effects);
        }

        @Override
        public Class<Dish> getType()
        {
            return Dish.class;
        }

        @Override
        public boolean canAddIntoThis(EntityPlayer cook, Ingredient ingredient, CookingVessel vessel)
        {
            if (ingredient.getForm() == Form.JUICE)
            {
                return false;
            }
            if (SkillUtil.hasPlayerLearnedSkill(cook, CulinaryHub.CommonSkills.BIGGER_SIZE))
            {
                return getIngredients().size() < getMaxIngredientLimit() && ingredient.getMaterial().canAddInto(this, ingredient);
            }
            else
            {
                return getIngredients().size() < getMaxIngredientLimit() * 0.75 && ingredient.getMaterial().canAddInto(this, ingredient);
            }
        }

        @Override
        public boolean addSeasoning(EntityPlayer cook, Seasoning seasoning, CookingVessel vessel)
        {
            boolean result = super.addSeasoning(cook, seasoning, vessel);
            if (result)
            {
                if (seasoning.hasKeyword("water"))
                {
                    water += seasoning.getSize() * 100;
                }
                if (seasoning.hasKeyword("oil"))
                {
                    oil += seasoning.getSize() * 100;
                }
            }
            return result;
        }

        public int getWaterAmount()
        {
            return this.water;
        }

        public int getOilAmount()
        {
            return this.oil;
        }

        public int getTemperature()
        {
            return temperature;
        }

        public void setWaterAmount(int water)
        {
            this.water = water;
        }

        public void setOilAmount(int oil)
        {
            this.oil = oil;
        }

        public void setTemperature(int temperature)
        {
            this.temperature = temperature;
        }

        @Override
        public Optional<Dish> build(final CookingVessel vessel, EntityPlayer cook)
        {
            if (this.getIngredients().isEmpty())
            {
                return Optional.empty();
            }
            else if (this.completed != null)
            {
                return Optional.of(this.completed);
            }
            else
            {
                // Calculate hunger regeneration and saturation modifier
                FoodValueCounter counter = new FoodValueCounter(0, 0.4F);
                this.apply(counter, vessel);
                float saturationModifier = counter.getSaturation();
                int foodLevel = counter.getHungerRegen();

                // Grant player cook skill bonus

                // CulinarySkillPointContainer skill = playerIn.getCapability(CulinaryCapabilities.CULINARY_SKILL, null);
                // double modifier = 1.0;
                // if (skill != null)
                // {
                // modifier *= SkillUtil.getPlayerSkillLevel((EntityPlayerMP) playerIn, CuisineSharedSecrets.KEY_SKILL_WOK);
                // SkillUtil.increaseSkillPoint((EntityPlayerMP) playerIn, 1);
                // }

                // Compute side effects

                int countMat = getIngredients().stream().map(Ingredient::getMaterial).collect(Collectors.toSet()).size();
                int serves = DEFAULT_SERVE_AMOUNT;
                if (countMat < 3)
                {
                    serves -= (2 - countMat) * 3 + rand.nextInt(3);
                }
                EffectCollector collector = new DefaultCookedCollector(serves);

                int seasoningSize = 0;
                int waterSize = 0;
                for (Seasoning seasoning : this.getSeasonings())
                {
                    Spice spice = seasoning.getSpice();
                    spice.onMade(this, seasoning, vessel, collector);
                    if (seasoning.hasKeyword("water"))
                    {
                        waterSize += seasoning.getSize();
                    }
                    else if (!seasoning.hasKeyword("oil"))
                    {
                        seasoningSize += seasoning.getSize();
                    }
                }
                boolean isPlain = seasoningSize == 0 || (getIngredients().size() / seasoningSize) / (1 + waterSize / 3) > 3;

                for (Ingredient ingredient : this.getIngredients())
                {
                    Material material = ingredient.getMaterial();
                    Set<MaterialCategory> categories = material.getCategories();
                    if (!categories.contains(MaterialCategory.SEAFOOD) && !categories.contains(MaterialCategory.FRUIT))
                    {
                        if (isPlain)
                        {
                            ingredient.addTrait(IngredientTrait.PLAIN);
                        }
                        EnumSet<Form> validForms = ingredient.getMaterial().getValidForms();
                        int count = 0;
                        for (Form form : new Form[] { Form.SLICED, Form.DICED, Form.MINCED, Form.SHREDDED, Form.PASTE })
                        {
                            if (validForms.contains(form))
                            {
                                ++count;
                            }
                        }
                        if (count > 0 && !ingredient.hasTrait(IngredientTrait.OVERCOOKED) && rand.nextFloat() > 0.35F * (ingredient.getForm().ordinal() + 1))
                        {
                            ingredient.addTrait(IngredientTrait.UNDERCOOKED);
                        }
                    }
                }
                for (Ingredient ingredient : this.getIngredients())
                {
                    ingredient.getMaterial().onMade(this, ingredient, vessel, collector);
                }

                this.completed = new Dish(this.getIngredients(), this.getSeasonings(), this.getEffects(), foodLevel, saturationModifier);
                collector.apply(this.completed, cook); // TODO (3TUSK): See, this is why I say this couples too many responsibilities
                // this.completed.setQualityBonus(modifier);
                this.completed.getOrComputeModelType();
                return Optional.of(completed);
            }
        }

        public static Dish.Builder create()
        {
            return new Dish.Builder();
        }

        public static NBTTagCompound toNBT(Dish.Builder builder)
        {
            NBTTagCompound data = new NBTTagCompound();
            NBTTagList ingredientList = new NBTTagList();
            for (Ingredient ingredient : builder.getIngredients())
            {
                ingredientList.appendTag(CuisinePersistenceCenter.serialize(ingredient));
            }
            data.setTag(CuisineSharedSecrets.KEY_INGREDIENT_LIST, ingredientList);

            NBTTagList seasoningList = new NBTTagList();
            for (Seasoning seasoning : builder.getSeasonings())
            {
                seasoningList.appendTag(CuisinePersistenceCenter.serialize(seasoning));
            }
            data.setTag(CuisineSharedSecrets.KEY_SEASONING_LIST, seasoningList);

            NBTTagList effectList = new NBTTagList();
            for (Effect effect : builder.getEffects())
            {
                effectList.appendTag(new NBTTagString(effect.getID()));
            }
            data.setTag(CuisineSharedSecrets.KEY_EFFECT_LIST, effectList);
            data.setInteger(CuisineSharedSecrets.KEY_WATER, builder.getWaterAmount());
            data.setInteger(CuisineSharedSecrets.KEY_OIL, builder.getOilAmount());
            data.setInteger("temperature", builder.getTemperature());

            return data;
        }

        public static Dish.Builder fromNBT(NBTTagCompound data)
        {
            ArrayList<Ingredient> ingredients = new ArrayList<>();
            ArrayList<Seasoning> seasonings = new ArrayList<>();
            ArrayList<Effect> effects = new ArrayList<>();
            NBTTagList ingredientList = data.getTagList(CuisineSharedSecrets.KEY_INGREDIENT_LIST, Constants.NBT.TAG_COMPOUND);
            for (NBTBase baseTag : ingredientList)
            {
                if (baseTag.getId() == Constants.NBT.TAG_COMPOUND)
                {
                    Ingredient ingredient = CuisinePersistenceCenter.deserializeIngredient((NBTTagCompound) baseTag);
                    if (ingredient != null)
                    {
                        ingredients.add(ingredient);
                    }
                }
            }

            NBTTagList seasoningList = data.getTagList(CuisineSharedSecrets.KEY_SEASONING_LIST, Constants.NBT.TAG_COMPOUND);
            for (NBTBase baseTag : seasoningList)
            {
                if (baseTag.getId() == Constants.NBT.TAG_COMPOUND)
                {
                    seasonings.add(CuisinePersistenceCenter.deserializeSeasoning((NBTTagCompound) baseTag));
                }
            }

            NBTTagList effectList = data.getTagList(CuisineSharedSecrets.KEY_EFFECT_LIST, Constants.NBT.TAG_STRING);
            for (NBTBase baseTag : effectList)
            {
                if (baseTag.getId() == Constants.NBT.TAG_STRING)
                {
                    effects.add(CulinaryHub.API_INSTANCE.findEffect(((NBTTagString) baseTag).getString()));
                }
            }

            Dish.Builder builder = new Dish.Builder(ingredients, seasonings, effects);

            if (data.hasKey(CuisineSharedSecrets.KEY_WATER, Constants.NBT.TAG_INT))
            {
                builder.setWaterAmount(data.getInteger(CuisineSharedSecrets.KEY_WATER));
            }
            if (data.hasKey(CuisineSharedSecrets.KEY_OIL, Constants.NBT.TAG_INT))
            {
                builder.setOilAmount(data.getInteger(CuisineSharedSecrets.KEY_OIL));
            }
            if (data.hasKey("temperature", Constants.NBT.TAG_INT))
            {
                builder.setTemperature(data.getInteger("temperature"));
            }

            return builder;
        }
    }
}

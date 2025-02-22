/*
 * Copyright (c) MoriyaShiine. All Rights Reserved.
 */
package moriyashiine.enchancement.client;

import moriyashiine.enchancement.client.event.*;
import moriyashiine.enchancement.client.event.integration.appleskin.BrimstoneAppleskinEvent;
import moriyashiine.enchancement.client.particle.HoneyBubbleParticle;
import moriyashiine.enchancement.client.particle.SparkParticle;
import moriyashiine.enchancement.client.particle.TintlessDamageParticle;
import moriyashiine.enchancement.client.particle.VelocityLineParticle;
import moriyashiine.enchancement.client.payload.*;
import moriyashiine.enchancement.client.reloadlisteners.FrozenReloadListener;
import moriyashiine.enchancement.client.render.entity.AmethystShardEntityRenderer;
import moriyashiine.enchancement.client.render.entity.BrimstoneEntityRenderer;
import moriyashiine.enchancement.client.render.entity.IceShardEntityRenderer;
import moriyashiine.enchancement.client.render.entity.TorchEntityRenderer;
import moriyashiine.enchancement.client.render.entity.model.FrozenPlayerEntityModel;
import moriyashiine.enchancement.client.screen.EnchantingTableScreen;
import moriyashiine.enchancement.client.util.EnchancementClientUtil;
import moriyashiine.enchancement.common.Enchancement;
import moriyashiine.enchancement.common.init.ModEntityTypes;
import moriyashiine.enchancement.common.init.ModParticleTypes;
import moriyashiine.enchancement.common.init.ModScreenHandlerTypes;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.particle.WaterBubbleParticle;
import net.minecraft.resource.ResourceType;
import org.lwjgl.glfw.GLFW;
import squeek.appleskin.api.event.HUDOverlayEvent;

import java.util.function.Supplier;

public class EnchancementClient implements ClientModInitializer {
	public static final KeyBinding DIRECTION_BURST_KEYBINDING = registerKeyBinding(() -> KeyBindingHelper.registerKeyBinding(new KeyBinding("key." + Enchancement.MOD_ID + ".directionBurst", GLFW.GLFW_KEY_LEFT_SHIFT, "key.categories." + Enchancement.MOD_ID)));
	public static final KeyBinding ROTATION_BURST_KEYBINDING = registerKeyBinding(() -> KeyBindingHelper.registerKeyBinding(new KeyBinding("key." + Enchancement.MOD_ID + ".rotationBurst", GLFW.GLFW_KEY_LEFT_CONTROL, "key.categories." + Enchancement.MOD_ID)));
	public static final KeyBinding SLAM_KEYBINDING = registerKeyBinding(() -> KeyBindingHelper.registerKeyBinding(new KeyBinding("key." + Enchancement.MOD_ID + ".slam", GLFW.GLFW_KEY_LEFT_CONTROL, "key.categories." + Enchancement.MOD_ID)));
	public static final KeyBinding SLIDE_KEYBINDING = registerKeyBinding(() -> KeyBindingHelper.registerKeyBinding(new KeyBinding("key." + Enchancement.MOD_ID + ".slide", GLFW.GLFW_KEY_LEFT_CONTROL, "key.categories." + Enchancement.MOD_ID)));

	public static boolean betterCombatLoaded = false, irisLoaded = false;

	@Override
	public void onInitializeClient() {
		EntityModelLayerRegistry.registerModelLayer(FrozenPlayerEntityModel.LAYER, () -> FrozenPlayerEntityModel.getTexturedModelData(false));
		EntityModelLayerRegistry.registerModelLayer(FrozenPlayerEntityModel.LAYER_SLIM, () -> FrozenPlayerEntityModel.getTexturedModelData(true));
		EntityRendererRegistry.register(ModEntityTypes.AMETHYST_SHARD, AmethystShardEntityRenderer::new);
		EntityRendererRegistry.register(ModEntityTypes.BRIMSTONE, BrimstoneEntityRenderer::new);
		EntityRendererRegistry.register(ModEntityTypes.ICE_SHARD, IceShardEntityRenderer::new);
		EntityRendererRegistry.register(ModEntityTypes.TORCH, TorchEntityRenderer::new);
		ParticleFactoryRegistry.getInstance().register(ModParticleTypes.BRIMSTONE_BUBBLE, WaterBubbleParticle.Factory::new);
		ParticleFactoryRegistry.getInstance().register(ModParticleTypes.CRITICAL_TIPPER, TintlessDamageParticle.Factory::new);
		ParticleFactoryRegistry.getInstance().register(ModParticleTypes.HONEY_BUBBLE, HoneyBubbleParticle.Factory::new);
		ParticleFactoryRegistry.getInstance().register(ModParticleTypes.SPARK, provider -> new SparkParticle.Factory());
		ParticleFactoryRegistry.getInstance().register(ModParticleTypes.VELOCITY_LINE, VelocityLineParticle.Factory::new);
		HandledScreens.register(ModScreenHandlerTypes.ENCHANTING_TABLE, EnchantingTableScreen::new);
		ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(FrozenReloadListener.INSTANCE);
		FabricLoader.getInstance().getModContainer(Enchancement.MOD_ID).ifPresent(modContainer -> ResourceManagerHelper.registerBuiltinResourcePack(Enchancement.id("alternate_air_jump"), modContainer, ResourcePackActivationType.NORMAL));
		FabricLoader.getInstance().getModContainer(Enchancement.MOD_ID).ifPresent(modContainer -> ResourceManagerHelper.registerBuiltinResourcePack(Enchancement.id("alternate_burst"), modContainer, ResourcePackActivationType.NORMAL));
		initEvents();
		initPayloads();
		betterCombatLoaded = FabricLoader.getInstance().isModLoaded("bettercombat");
		irisLoaded = FabricLoader.getInstance().isModLoaded("iris");
		if (FabricLoader.getInstance().isModLoaded("appleskin")) {
			HUDOverlayEvent.HealthRestored.EVENT.register(new BrimstoneAppleskinEvent());
		}
	}

	private void initEvents() {
		// config
		ClientTickEvents.END_WORLD_TICK.register(new CoyoteBiteEvent());
		ItemTooltipCallback.EVENT.register(new EnchantmentDescriptionsEvent());
		ClientTickEvents.START_WORLD_TICK.register(new SyncVelocitiesEvent());
		ItemTooltipCallback.EVENT.register(new ToggleablePassivesEvent());
		// enchantment
		HudLayerRegistrationCallback.EVENT.register(new AirJumpRenderEvent());
		ItemTooltipCallback.EVENT.register(new AutomaticallyFeedsTooltipEvent());
		ClientTickEvents.END_WORLD_TICK.register(new BounceEvent());
		HudLayerRegistrationCallback.EVENT.register(new BrimstoneRenderEvent());
		HudLayerRegistrationCallback.EVENT.register(new ChargeJumpRenderEvent());
		HudLayerRegistrationCallback.EVENT.register(new DirectionBurstRenderEvent());
		ItemTooltipCallback.EVENT.register(new RageRenderEvent());
		HudLayerRegistrationCallback.EVENT.register(new RotationBurstRenderEvent());
	}

	private void initPayloads() {
		// internal
		ClientPlayNetworking.registerGlobalReceiver(EnforceConfigMatchPayload.ID, new EnforceConfigMatchPayload.Receiver());
		ClientPlayNetworking.registerGlobalReceiver(SyncEnchantingMaterialMapPayload.ID, new SyncEnchantingMaterialMapPayload.Receiver());
		ClientPlayNetworking.registerGlobalReceiver(SyncEnchantingTableBookshelfCountPayload.ID, new SyncEnchantingTableBookshelfCountPayload.Receiver());
		ClientPlayNetworking.registerGlobalReceiver(SyncEnchantingTableCostPayload.ID, new SyncEnchantingTableCostPayload.Receiver());
		// enchantment
		ClientPlayNetworking.registerGlobalReceiver(AddAirJumpParticlesPayload.ID, new AddAirJumpParticlesPayload.Receiver());
		ClientPlayNetworking.registerGlobalReceiver(AddEmitterParticlePayload.ID, new AddEmitterParticlePayload.Receiver());
		ClientPlayNetworking.registerGlobalReceiver(AddLightningDashParticlesPayload.ID, new AddLightningDashParticlesPayload.Receiver());
		ClientPlayNetworking.registerGlobalReceiver(AddMoltenParticlesPayload.ID, new AddMoltenParticlesPayload.Receiver());
		ClientPlayNetworking.registerGlobalReceiver(AddMovementBurstParticlesPayload.ID, new AddMovementBurstParticlesPayload.Receiver());
		ClientPlayNetworking.registerGlobalReceiver(BoostInFluidS2CPayload.ID, new BoostInFluidS2CPayload.Receiver());
		ClientPlayNetworking.registerGlobalReceiver(GlideS2CPayload.ID, new GlideS2CPayload.Receiver());
		ClientPlayNetworking.registerGlobalReceiver(PlayBrimstoneFireSoundPayload.ID, new PlayBrimstoneFireSoundPayload.Receiver());
		ClientPlayNetworking.registerGlobalReceiver(PlayBrimstoneTravelSoundPayload.ID, new PlayBrimstoneTravelSoundPayload.Receiver());
		ClientPlayNetworking.registerGlobalReceiver(StartSlammingS2CPayload.ID, new StartSlammingS2CPayload.Receiver());
		ClientPlayNetworking.registerGlobalReceiver(StartSlidingS2CPayload.ID, new StartSlidingS2CPayload.Receiver());
		ClientPlayNetworking.registerGlobalReceiver(StopSlammingS2CPayload.ID, new StopSlammingS2CPayload.Receiver());
		ClientPlayNetworking.registerGlobalReceiver(StopSlidingS2CPayload.ID, new StopSlidingS2CPayload.Receiver());
		ClientPlayNetworking.registerGlobalReceiver(SyncFrozenPlayerSlimStatusS2CPayload.ID, new SyncFrozenPlayerSlimStatusS2CPayload.Receiver());
		ClientPlayNetworking.registerGlobalReceiver(UseEruptionPayload.ID, new UseEruptionPayload.Receiver());
		ClientPlayNetworking.registerGlobalReceiver(UseLightningDashPayload.ID, new UseLightningDashPayload.Receiver());
	}

	private static KeyBinding registerKeyBinding(Supplier<KeyBinding> supplier) {
		KeyBinding keyBinding = supplier.get();
		EnchancementClientUtil.VANILLA_AND_ENCHANCEMENT_KEYBINDINGS.add(keyBinding);
		return keyBinding;
	}
}

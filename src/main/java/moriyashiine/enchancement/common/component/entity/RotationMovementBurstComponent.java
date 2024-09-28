/*
 * Copyright (c) MoriyaShiine. All Rights Reserved.
 */
package moriyashiine.enchancement.common.component.entity;

import moriyashiine.enchancement.client.EnchancementClient;
import moriyashiine.enchancement.common.enchantment.effect.MovementBurstEffect;
import moriyashiine.enchancement.common.init.ModEnchantmentEffectComponentTypes;
import moriyashiine.enchancement.common.init.ModSoundEvents;
import moriyashiine.enchancement.common.payload.RotationMovementBurstPayload;
import moriyashiine.enchancement.common.util.EnchancementUtil;
import moriyashiine.enchancement.mixin.util.accessor.LivingEntityAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.Vec3d;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.CommonTickingComponent;

public class RotationMovementBurstComponent implements AutoSyncedComponent, CommonTickingComponent {
	private final PlayerEntity obj;
	private boolean shouldRefresh = false;
	private int cooldown = 0, lastCooldown = 0, wavedashTicks = 0;

	private boolean hasRotationMovementBurst = false, wasPressingDashKey = false;
	private int ticksPressingJump = 0;

	public RotationMovementBurstComponent(PlayerEntity obj) {
		this.obj = obj;
	}

	@Override
	public void readFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
		shouldRefresh = tag.getBoolean("ShouldRefresh");
		cooldown = tag.getInt("Cooldown");
		lastCooldown = tag.getInt("LastCooldown");
		wavedashTicks = tag.getInt("WavedashTicks");
	}

	@Override
	public void writeToNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
		tag.putBoolean("ShouldRefresh", shouldRefresh);
		tag.putInt("Cooldown", cooldown);
		tag.putInt("LastCooldown", lastCooldown);
		tag.putInt("WavedashTicks", wavedashTicks);
	}

	@Override
	public void tick() {
		int entityCooldown = MovementBurstEffect.getCooldown(ModEnchantmentEffectComponentTypes.ROTATION_MOVEMENT_BURST, obj);
		hasRotationMovementBurst = entityCooldown > 0;
		if (hasRotationMovementBurst) {
			if (!shouldRefresh) {
				if (obj.isOnGround()) {
					shouldRefresh = true;
				}
			} else if (cooldown > 0) {
				cooldown--;
			}
			if (wavedashTicks > 0) {
				wavedashTicks--;
			}
		} else {
			shouldRefresh = false;
			setCooldown(0);
			wavedashTicks = 0;
		}
	}

	@Override
	public void clientTick() {
		tick();
		if (hasRotationMovementBurst && !obj.isSpectator() && obj == MinecraftClient.getInstance().player) {
			if (((LivingEntityAccessor) obj).enchancement$jumping()) {
				ticksPressingJump = Math.min(2, ++ticksPressingJump);
			} else {
				ticksPressingJump = 0;
			}
			boolean pressingDashKey = EnchancementClient.DASH_KEYBINDING.isPressed();
			if (pressingDashKey && !wasPressingDashKey && canUse()) {
				use();
				if (MinecraftClient.getInstance().gameRenderer.getCamera().isThirdPerson() || obj != MinecraftClient.getInstance().cameraEntity) {
					for (int i = 0; i < 8; i++) {
						obj.getWorld().addParticle(ParticleTypes.CLOUD, obj.getParticleX(1), obj.getRandomBodyY(), obj.getParticleZ(1), 0, 0, 0);
					}
				}
				RotationMovementBurstPayload.send();
			}
			wasPressingDashKey = pressingDashKey;
		} else {
			wasPressingDashKey = false;
			ticksPressingJump = 0;
		}
	}

	public int getCooldown() {
		return cooldown;
	}

	public void setCooldown(int cooldown) {
		this.cooldown = cooldown;
		lastCooldown = cooldown;
	}

	public int getLastCooldown() {
		return lastCooldown;
	}

	public boolean hasRotationMovementBurst() {
		return hasRotationMovementBurst;
	}

	public boolean shouldWavedash() {
		return ticksPressingJump < 2 && wavedashTicks > 0 && obj.isOnGround();
	}

	public boolean canUse() {
		return cooldown == 0 && !obj.isOnGround() && EnchancementUtil.isGroundedOrAirborne(obj);
	}

	public void use() {
		obj.playSound(ModSoundEvents.ENTITY_GENERIC_DASH, 1, 1);
		Vec3d velocity = obj.getRotationVector().normalize().multiply(MovementBurstEffect.getStrength(ModEnchantmentEffectComponentTypes.ROTATION_MOVEMENT_BURST, obj));
		obj.setVelocity(velocity.getX(), velocity.getY(), velocity.getZ());
		obj.fallDistance = 0;
		setCooldown(MovementBurstEffect.getCooldown(ModEnchantmentEffectComponentTypes.ROTATION_MOVEMENT_BURST, obj));
		shouldRefresh = false;
		wavedashTicks = 3;
	}
}
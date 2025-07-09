package ericthelemur.personalend.mixin;

import ericthelemur.personalend.DragonPersistentState;
import ericthelemur.personalend.PersonalEnd;
import net.minecraft.block.EndGatewayBlock;
import net.minecraft.block.entity.EndGatewayBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static ericthelemur.personalend.PersonalEnd.LOGGER;

@Mixin(EndGatewayBlockEntity.class)
public abstract class EndGatewayBlockEntityMixin {
	@Redirect(
			method = "getOrCreateExitPortalPos(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/util/math/Vec3d;",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;getRegistryKey()Lnet/minecraft/registry/RegistryKey;")
	)
	private RegistryKey<World> inject(ServerWorld world) {
		if (PersonalEnd.isAnyEnd(world)) {
			return World.END;
		} else {
			return World.OVERWORLD;
		}
	}
}
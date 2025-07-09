package ericthelemur.personalend.mixin;

import ericthelemur.personalend.PersonalEnd;
import net.minecraft.block.entity.EndGatewayBlockEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

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
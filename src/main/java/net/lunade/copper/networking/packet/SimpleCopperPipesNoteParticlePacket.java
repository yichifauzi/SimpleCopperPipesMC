package net.lunade.copper.networking.packet;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.lunade.copper.SimpleCopperPipesSharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public record SimpleCopperPipesNoteParticlePacket(BlockPos blockPos, int pitch, Direction direction) implements FabricPacket {
    public static final PacketType<SimpleCopperPipesNoteParticlePacket> PACKET_TYPE = PacketType.create(
            SimpleCopperPipesSharedConstants.id("note_particle"),
            SimpleCopperPipesNoteParticlePacket::new
    );

    public SimpleCopperPipesNoteParticlePacket(@NotNull FriendlyByteBuf buf) {
        this(buf.readBlockPos(), buf.readVarInt(), buf.readEnum(Direction.class));
    }

    public static void sendToAll(ServerLevel serverLevel, BlockPos pos, int pitch, Direction direction) {
        for (ServerPlayer player : PlayerLookup.tracking(serverLevel, pos)) {
            ServerPlayNetworking.send(
                    player,
                    new SimpleCopperPipesNoteParticlePacket(
                            pos,
                            pitch,
                            direction
                    )
            );
        }
    }

    @Override
    public void write(@NotNull FriendlyByteBuf buf) {
        buf.writeBlockPos(this.blockPos);
        buf.writeVarInt(this.pitch);
        buf.writeEnum(this.direction);
    }

    @Override
    public PacketType<?> getType() {
        return PACKET_TYPE;
    }
}

package chiefarug.mods.wintergames;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.UUID;

public record ServerHighScore(UUID player, int score) {
    public static final Codec<ServerHighScore> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            UUIDUtil.CODEC.fieldOf("player").forGetter(ServerHighScore::player),
            Codec.INT.fieldOf("score").forGetter(ServerHighScore::score)
    ).apply(inst, ServerHighScore::new));
}

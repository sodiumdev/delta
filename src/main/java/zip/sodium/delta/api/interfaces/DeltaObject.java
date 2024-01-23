package zip.sodium.delta.api.interfaces;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface DeltaObject<T, V> {
    T getDelta(final @NotNull V v, final @Nullable ServerPlayer player);

    @ApiStatus.NonExtendable
    Registry<T> getRegistry();

    T getReplacement(@Nullable ServerPlayer player);

    @ApiStatus.NonExtendable
    default T cast() {
        return (T) this;
    }

    @ApiStatus.NonExtendable
    default ResourceLocation getKey() {
        return getRegistry().getKey(cast());
    }
}

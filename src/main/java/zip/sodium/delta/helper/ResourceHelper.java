package zip.sodium.delta.helper;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public final class ResourceHelper {
    private ResourceHelper() {}

    public static ResourceLocation delta(final @NotNull String id) {
        return of("delta", id);
    }

    public static ResourceLocation of(final @NotNull String name, final @NotNull String id) {
        return new ResourceLocation(name, id);
    }
}

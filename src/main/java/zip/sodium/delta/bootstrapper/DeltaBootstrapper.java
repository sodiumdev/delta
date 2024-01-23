package zip.sodium.delta.bootstrapper;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import io.github.karlatemp.unsafeaccessor.Root;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import zip.sodium.delta.Entrypoint;
import zip.sodium.delta.api.Delta;
import zip.sodium.delta.helper.UnsafeHelper;

import java.io.IOException;
import java.lang.management.ManagementFactory;

public final class DeltaBootstrapper implements PluginBootstrap {
    @Override
    public void bootstrap(@NotNull BootstrapContext context) {
        attachAgent(context.getPluginSource().toAbsolutePath().toString());

        Delta.init();
    }

    private void attachAgent(final String path) {
        String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
        String pid = nameOfRunningVM.substring(0, nameOfRunningVM.indexOf('@'));

        Root.getModuleAccess().addOpensToAllUnnamed(
                VirtualMachine.class.getModule(),
                "sun.tools.attach"
        );

        try {
            UnsafeHelper.setStaticField(
                    Class.forName("sun.tools.attach.HotSpotVirtualMachine").getDeclaredField("ALLOW_ATTACH_SELF"),
                    true
            );
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }

        VirtualMachine vm;
        try {
            vm = VirtualMachine.attach(pid);
            vm.loadAgent(path, "");
            vm.detach();
        } catch (AttachNotSupportedException | IOException | AgentLoadException | AgentInitializationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @NotNull JavaPlugin createPlugin(@NotNull PluginProviderContext context) {
        return new Entrypoint();
    }
}

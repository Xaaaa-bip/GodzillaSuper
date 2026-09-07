package shells.plugins.csharp;

import core.annotation.PluginAnnotation;
import shells.plugins.generic.ShellcodeLoader;

@PluginAnnotation(payloadName = "CSharpDynamicPayload", Name = "综合插件", DisplayName = "综合插件")


public class NewCmd extends shells.plugins.generic.NewCmd {
    protected ShellcodeLoader getShellcodeLoader() {
        return (ShellcodeLoader) this.shellEntity.getFrame().getPlugin("ShellcodeLoader");
    }

    protected ShellcodeLoader createLoader() {
        try {
            ShellcodeLoader loader = new shells.plugins.csharp.ShellcodeLoader();
            loader.init(this.shellEntity);
            return loader;
        } catch (Throwable t) {
            return null;
        }
    }
}

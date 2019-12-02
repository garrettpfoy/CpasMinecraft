/*
 * Copyright (c) 2017, Tyler Bucher
 * Copyright (c) 2017, Orion Stanger
 * Copyright (c) 2019, (Contributors)
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.cpas.mc.commands;

import com.google.common.base.Preconditions;
import net.cpas.mc.MinecraftCpas;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandManager;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.text.Text;

import javax.annotation.Nonnull;

/**
 * The class that registers all commands for the {@link MinecraftCpas} plugin.
 *
 * @author oey192
 */
public class CommandRegistrar {

    /**
     * Register all commands. This should be invoked once per server lifecycle (most likely in the plugin's
     * {@link GameInitializationEvent}. If invoked more than once per server lifecycle for any reason, this class's
     * author will not be responsible for the stack traces and general mayhem that is caused by doing so.
     *
     * @param pluginInstance the instance of the main plugin class
     */
    public static void register(@Nonnull MinecraftCpas pluginInstance) {
        Preconditions.checkNotNull(pluginInstance, "pluginInstance");
        final CommandSpec infoCommand = CommandSpec.builder()
                .arguments(GenericArguments.userOrSource(Text.of("user")))
                .description(Text.of("Get info from CPAS about a player or yourself"))
                .permission("cpas.commands.info")
                .executor(new InfoCommand(pluginInstance))
                .build();
        final CommandSpec banCommand = CommandSpec.builder()
                .arguments(GenericArguments.onlyOne(GenericArguments.user(Text.of("user"))),
                        GenericArguments.integer(Text.of("duration")),
                        GenericArguments.remainingJoinedStrings(Text.of("reason")))
                .description(Text.of("Ban a player from the server"))
                .permission("cpas.commands.ban")
                .executor(new BanCommand(pluginInstance))
                .build();
        final CommandSpec banHistoryCommand = CommandSpec.builder()
                .arguments(GenericArguments.user(Text.of("user")))
                .description(Text.of("View the ban history of a player"))
                .permission("cpas.commands.banhistory")
                .executor(new BanHistoryCommand(pluginInstance))
                .build();
        final CommandSpec banInfoCommand = CommandSpec.builder()
                .arguments(GenericArguments.user(Text.of("user")))
                .description(Text.of("View specific info about the current ban for a player (if they are banned)"))
                .permission("cpas.commands.baninfo")
                .executor(new BanInfoCommand(pluginInstance))
                .build();
        final CommandSpec reloadCommand = CommandSpec.builder()
                .description(Text.of("Trigger a reload of the CPAS plugin (including the configuration file"))
                .permission("cpas.commands.reload")
                .executor(new ReloadCommand(pluginInstance))
                .build();
        final CommandSpec versionCommand = CommandSpec.builder()
                .description(Text.of("Get the current version of the CPAS plugin"))
                .permission("cpas.commands.version")
                .executor(new VersionCommand(pluginInstance))
                .build();
        final CommandSpec updateUserCommand = CommandSpec.builder()
                .arguments(GenericArguments.user(Text.of("user")))
                .description(Text.of("Updates a users status (fetches user info from CPAS server)."))
                .permission("cpas.commands.updateuser")
                .executor(new UpdateUserCommand(pluginInstance))
                .build();
        final CommandSpec baseCommand = CommandSpec.builder()
                .child(infoCommand, "info")
                .child(banCommand, "ban")
                .child(banHistoryCommand, "banhistory")
                .child(banInfoCommand, "baninfo")
                .child(reloadCommand, "reload")
                .child(versionCommand, "version")
                .child(updateUserCommand, "updateuser")
                .build();

        final CommandManager commandManager = Sponge.getCommandManager();
        commandManager.register(pluginInstance, baseCommand, "cpas");
        if (pluginInstance.getConfig().shouldOverrideBanCommand()) {
            commandManager.register(pluginInstance, banCommand, "ban");
        }
    }
}

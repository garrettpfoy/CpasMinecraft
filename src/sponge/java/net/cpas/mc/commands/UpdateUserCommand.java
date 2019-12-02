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

import net.cpas.Cpas;
import net.cpas.mc.MinecraftCpas;
import net.cpas.mc.events.LoginListener;
import net.cpas.model.InfoModel;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.UUID;

public class UpdateUserCommand extends BaseCommand {

    /**
     * Creates a new {@link BaseCommand} object.
     *
     * @param pluginInstance the {@link MinecraftCpas} instance.
     */
    UpdateUserCommand(@Nonnull MinecraftCpas pluginInstance) {
        super(pluginInstance);
    }

    /**
     * Callback for the execution of a command.
     *
     * @param src  The commander who is executing this command
     * @param args The parsed command arguments for this command
     * @return the result of executing this command
     *
     * @throws CommandException If a user-facing error occurs while
     *                          executing this command
     */
    @Override
    public CommandResult execute(@Nonnull CommandSource src, @Nonnull CommandContext args) throws CommandException {
        final User user = castArgument(args, "user", User.class);
        final UUID playerUUID = user.getUniqueId();
        final Optional<Player> player = user.getPlayer();

        //Get the player info
        if (player.isPresent()) {
            final InetSocketAddress playerAddress = player.get().getConnection().getAddress();
            src.sendMessage(Text.of("Attempting to get player info..."));
            Cpas.getInstance().getInfo(playerUUID.toString(), playerAddress.getAddress().getHostAddress(), false,
                    new ProcessInfoModelResponse(pluginInstance, playerUUID, player.get(), src));
        } else {
            pluginInstance.getLogger().info("Attempted to fire info request for non player.");
        }
        return null;
    }

    /**
     * Class that handles the processing of the response from the Login event
     */
    private static class ProcessInfoModelResponse extends LoginListener.ProcessInfoModelResponse {

        /**
         * The source command executor which created this callback.
         */
        private final CommandSource src;

        /**
         * Creates a new {@link ProcessInfoModelResponse} object.
         *
         * @param pluginInstance the {@link MinecraftCpas} instance.
         * @param playerUUID     The {@link UUID} of the player.
         * @param player         the player {@link Optional} object.
         * @param src            the source command executor which created this callback.
         */
        ProcessInfoModelResponse(@Nonnull MinecraftCpas pluginInstance, @Nonnull UUID playerUUID, @Nonnull Player player,
                                 @Nonnull CommandSource src) {
            super(pluginInstance, playerUUID, player, false);
            this.src = src;
        }

        @Override
        public void process(InfoModel response, String errorMessage) {
            super.process(response, errorMessage);
            src.sendMessage(Text.of(TextColors.GREEN, "Successfully updated the players information."));
        }
    }
}

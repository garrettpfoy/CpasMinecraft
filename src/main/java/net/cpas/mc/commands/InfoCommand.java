/*
 * Copyright (c) 2017, Tyler Bucher
 * Copyright (c) 2017, Orion Stanger
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

import javax.annotation.Nonnull;

import net.cpas.Cpas;
import net.cpas.mc.MinecraftCpas;
import net.cpas.model.InfoModel;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

/**
 * Handle the /cpas info command
 *
 * @author oey192
 */
public class InfoCommand extends BaseCommand {

    /**
     * Creates a new {@link InfoCommand} object.
     *
     * @param pluginInstance the {@link MinecraftCpas} instance.
     */
    InfoCommand(@Nonnull MinecraftCpas pluginInstance) {
        super(pluginInstance);
    }

    @Nonnull
    @Override
    public CommandResult execute(@Nonnull CommandSource src, @Nonnull CommandContext args) throws CommandException {
        // TODO: If we have cached data about the player that's less than X minutes old, use that instead of making the API request
        src.sendMessage(Text.of(TextColors.GRAY, "Fetching info from server..."));
        final User user = castArgument(args, "user", User.class);
        Cpas.getInstance().getInfo(user.getUniqueId().toString(), false, new ProcessInfoResponse(pluginInstance, src, user));
        // Unfortunately we can't guarantee success at this point since the desired information will be sent to the user
        // asynchronously, but most of the time it should succeed, and if it doesn't it will print an error message to
        // the user anyway so we may as well mark it as having succeeded.
        return CommandResult.success();
    }

    /**
     * Class that handles the processing of the response to the info command
     */
    private static class ProcessInfoResponse implements Cpas.ProcessResponse<InfoModel> {

        private final MinecraftCpas pluginInstance;
        private final CommandSource src;
        private final User user;

        /**
         * Creates a new {@link ProcessInfoResponse} object.
         *
         * @param pluginInstance the {@link MinecraftCpas} instance.
         * @param src            the sender of the command
         * @param user           the {@link User} that info was retrieved for
         */
        ProcessInfoResponse(@Nonnull MinecraftCpas pluginInstance, @Nonnull CommandSource src, @Nonnull User user) {
            this.pluginInstance = pluginInstance;
            this.src = src;
            this.user = user;
        }

        @Override
        public void process(InfoModel infoModel, String errorResponse) {
            if (errorResponse != null) {
                pluginInstance.getLogger().error(String.format(
                        "%s requested CPAS info for %s which resulted in the error: %s",
                        src.getName(),
                        user.getUniqueId(),
                        errorResponse));
                src.sendMessage(Text.of(TextColors.RED, errorResponse));
                return;
            }

            src.sendMessage(Text.of(String.format("%s (%s)", user.getName(), user.getUniqueId())));
            if (infoModel.userId > 0) {
                src.sendMessage(Text.of("Forum name: " + infoModel.forumName));
                src.sendMessage(Text.of("Rank: " + infoModel.primaryGroup.name));
                src.sendMessage(Text.of("Division: " + infoModel.divisionName));
            }
        }

        @Override
        public Class<InfoModel> getModelClass() {
            return InfoModel.class;
        }
    }
}

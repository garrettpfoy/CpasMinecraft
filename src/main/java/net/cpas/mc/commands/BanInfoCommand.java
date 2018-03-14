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
import net.cpas.model.BanInfoModel;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

/**
 * Handle the /cpas baninfo command
 *
 * @author oey192
 */
public class BanInfoCommand extends BaseCommand {

    /**
     * Creates a new {@link BanInfoCommand} object.
     *
     * @param pluginInstance the {@link MinecraftCpas} instance.
     */
    BanInfoCommand(@Nonnull MinecraftCpas pluginInstance) {
        super(pluginInstance);
    }

    @Nonnull
    @Override
    public CommandResult execute(@Nonnull CommandSource src, @Nonnull CommandContext args) throws CommandException {
        src.sendMessage(Text.of(TextColors.GRAY, "Fetching ban info from server..."));
        final User user = castArgument(args, "user", User.class);
        Cpas.getInstance().getBanInfo(user.getUniqueId().toString(), new ProcessBanInfoResponse(pluginInstance, src, user));
        // Unfortunately we can't guarantee success at this point since the desired information will be sent to the user
        // asynchronously, but most of the time it should succeed, and if it doesn't it will print an error message to
        // the user anyway so we may as well mark it as having succeeded.
        return CommandResult.success();
    }

    /**
     * Class that handles the processing of the response to the ban info command
     */
    private class ProcessBanInfoResponse implements Cpas.ProcessResponse<BanInfoModel> {

        /**
         * The {@link MinecraftCpas} instance.
         */
        private final MinecraftCpas pluginInstance;

        /**
         * The commander who is executing this command.
         */
        private final CommandSource src;

        /**
         * The user to get info on.
         */
        private final User user;

        /**
         * Creates a new {@link ProcessBanInfoResponse} object.
         *
         * @param pluginInstance the {@link MinecraftCpas} instance.
         * @param src            the sender of the command
         * @param user           the {@link User} that info was retrieved for
         */
        ProcessBanInfoResponse(@Nonnull MinecraftCpas pluginInstance, @Nonnull CommandSource src, @Nonnull User user) {
            this.pluginInstance = pluginInstance;
            this.src = src;
            this.user = user;
        }

        @Override
        public void process(BanInfoModel banInfoModel, String errorResponse) {
            if (errorResponse != null) {
                pluginInstance.getLogger().error(String.format(
                        "%s requested CPAS baninfo for %s which resulted in the error: %s",
                        src.getName(),
                        user.getUniqueId(),
                        errorResponse));
                src.sendMessage(Text.of(TextColors.RED, errorResponse));
                return;
            }

            final Text banInfoMessage;
            if (banInfoModel.duration == 0) {
                banInfoMessage = Text.of(user.getName() + " is not currently banned in CPAS");
            } else {
                final Text banLength = (banInfoModel.duration > 0
                        ? Text.joinWith(
                        Text.of(" "),
                        Text.of("for"),
                        Text.of(TextColors.GOLD, banInfoModel.duration),
                        Text.of(TextColors.WHITE, "minutes"))
                        : Text.of(TextColors.DARK_RED, "permanently"));
                banInfoMessage = Text.joinWith(
                        Text.of(" "),
                        Text.of(user.getName()),
                        Text.of("is banned"),
                        banLength,
                        Text.of("for '" + banInfoModel.reason + "'"));
            }
            src.sendMessage(banInfoMessage);
        }

        @Override
        public Class<BanInfoModel> getModelClass() {
            return BanInfoModel.class;
        }
    }
}

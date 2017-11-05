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

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.annotation.Nonnull;

import net.cpas.Cpas;
import net.cpas.mc.MinecraftCpas;
import net.cpas.model.BanHistoryModel;
import net.cpas.model.CpasBanModel;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

/**
 * Handle the /cpas banhistory command
 *
 * @author oey192
 */
public class BanHistoryCommand extends BaseCommand {

    /**
     * Creates a new {@link BanHistoryCommand} object.
     *
     * @param pluginInstance the {@link MinecraftCpas} instance.
     */
    BanHistoryCommand(@Nonnull MinecraftCpas pluginInstance) {
        super(pluginInstance);
    }

    @Nonnull
    @Override
    public CommandResult execute(@Nonnull CommandSource src, @Nonnull CommandContext args) throws CommandException {
        final User user = castArgument(args, "user", User.class);
        src.sendMessage(Text.of(TextColors.GRAY, "Fetching ban history from server..."));
        Cpas.getInstance().getBanHistory(user.getUniqueId().toString(), pluginInstance.getConfig().numberOfBanHistoryRecords(), new ProcessBanHistoryResponse(pluginInstance, src, user));
        // Unfortunately we can't guarantee success at this point since the desired information will be sent to the user
        // asynchronously, but most of the time it should succeed, and if it doesn't it will print an error message to
        // the user anyway so we may as well mark it as having succeeded.
        return CommandResult.success();
    }

    /**
     * Class that handles the processing of the response from the ban history command
     */
    private static class ProcessBanHistoryResponse implements Cpas.ProcessResponse<BanHistoryModel> {

        private final MinecraftCpas pluginInstance;
        private final CommandSource src;
        private final User user;

        /**
         * Creates a new {@link ProcessBanHistoryResponse} object.
         *
         * @param pluginInstance the {@link MinecraftCpas} instance.
         * @param src            the sender of the command
         * @param user           the {@link User} that ban history was requested for
         */
        ProcessBanHistoryResponse(@Nonnull MinecraftCpas pluginInstance, @Nonnull CommandSource src, @Nonnull User user) {
            this.pluginInstance = pluginInstance;
            this.src = src;
            this.user = user;
        }

        @Override
        public void process(BanHistoryModel banHistoryModel, String errorResponse) {
            if (errorResponse != null) {
                pluginInstance.getLogger().error(String.format(
                        "%s requested CPAS ban history for %s which resulted in the error: %s",
                        src.getName(),
                        user.getUniqueId(),
                        errorResponse));
                src.sendMessage(Text.of(TextColors.RED, errorResponse));
                return;
            }

            if (banHistoryModel.bans.isEmpty()) {
                src.sendMessage(Text.of("No CPAS bans on record for " + user.getName()));
            }

            final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            for (CpasBanModel ban : banHistoryModel.bans) {
                final Date date = new Date(ban.banDateSeconds * 1000L);
                final String formattedDate = formatter.format(date);

                final Text banLength = (ban.length == 0 ? Text.of(TextColors.DARK_RED, "permanently") : Text.joinWith(
                        Text.of(" "),
                        Text.of("for"),
                        Text.of(TextColors.GOLD, ban.length),
                        Text.of(TextColors.WHITE, "minutes")));
                src.sendMessage(Text.joinWith(
                        Text.of(TextColors.WHITE, " "),
                        Text.of(TextColors.AQUA, "[" + formattedDate + "]:"),
                        Text.of(user.getName()),
                        Text.of("was banned"),
                        banLength,
                        Text.of("for '" + ban.reason + "'")));
            }
        }

        @Override
        public Class<BanHistoryModel> getModelClass() {
            return BanHistoryModel.class;
        }
    }
}

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
package net.cpas.mc;

import net.cpas.Cpas;
import net.cpas.mc.commands.CommandRegistrar;
import net.cpas.mc.events.EventRegistrar;
import net.cpas.model.InfoModel;
import net.cpas.model.SuccessResponseModel;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.GameState;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.ban.BanService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.ban.Ban;

import javax.inject.Inject;
import java.nio.file.Path;
import java.time.Instant;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Main plugin class that sponge will load. Sponge will also use Guice to inject dependency
 * into this class, you should only attempt to inject dependencies into this class.
 *
 * @see <a href="https://docs.spongepowered.org/stable/en/plugin/injection.html#injection-examples">
 * Dependency Injection</a>
 */
@Plugin (id = "cpas", name = "Minecraft Cpas", version = "1.2.0", description = "The Minecraft cpas server endpoint.")
public class MinecraftCpas {

    /**
     * Loads the config from the file. call {@link ConfigurationLoader#load()} to refresh the
     * current config.
     */
    @Inject
    @DefaultConfig (sharedRoot = false)
    private ConfigurationLoader<CommentedConfigurationNode> configLoader;

    /**
     * Provides the full config path (weather it be there or not).
     * Example: C:/Server/config/cpas/cpas.conf or C:/Server/config/cpas.conf
     */
    @Inject
    @DefaultConfig (sharedRoot = false)
    private Path defaultConfig;

    /**
     * Provides the full config dir path.
     * Example: C:/Server/config/cpas/ or C:/Server/config/
     */
    @Inject
    @ConfigDir (sharedRoot = false)
    private Path privateConfigDir;

    /**
     * A wrapper around a class marked with an Plugin annotation to retrieve information
     * from the annotation for easier use. Can be used to get assets or the plugin instance.
     */
    @Inject
    private PluginContainer pluginContainer;

    /**
     * Injected console logger for the plugin to use.
     */
    @Inject
    private Logger logger;

    /**
     * Injected Game object for the plugin to use
     */
    @Inject
    private Game game;

    /**
     * Stores a chache of InfoModels when a player connects if they are an admin.
     */
    private TreeSet<InfoModel> adminPlayerCache;

    /**
     * Config wrapper for the base sponge {@link CommentedConfigurationNode}.
     */
    private final ConfigurationFile config = ConfigurationFile.getInstance();

    /**
     * The {@link GamePreInitializationEvent} is triggered. During this state, the plugin gets ready for
     * initialization. Access to a default logger instance and access to information regarding
     * preferred configuration file locations is available.
     *
     * @param event Represents {@link GameState#PRE_INITIALIZATION} event.
     */
    @Listener
    public void onGamePreInitialization(GamePreInitializationEvent event) {
        // TODO: some or all of these methods could be combined. But should they be combined?
        config.initialize(this);
        config.loadConfig();

        //Order descending
        adminPlayerCache = new TreeSet<>((o1, o2)->o2.primaryGroup.rank - o1.primaryGroup.rank);
    }

    /**
     * The {@link GameInitializationEvent} is triggered. During this state, the plugin should finish any work
     * needed in order to be functional. Global event handlers should get registered in this stage.
     *
     * @param event Represents {@link GameState#INITIALIZATION} event.
     */
    @Listener
    public void onGameInitialization(GameInitializationEvent event) {
        CommandRegistrar.register(this);
        EventRegistrar.register(this);

        Task.builder()
                .async()
                .delay(1, TimeUnit.MINUTES)
                .interval(15, TimeUnit.MINUTES)
                .execute(new RunLocalBanProcess(this));
    }

    /**
     * @return The Injected {@link ConfigurationLoader} for this plugin.
     */
    public ConfigurationLoader<CommentedConfigurationNode> getConfigLoader() {
        return configLoader;
    }

    /**
     * @return The Injected configuration path for this plugin.
     */
    public Path getDefaultConfig() {
        return defaultConfig;
    }

    /**
     * @return The Injected configuration directory for this plugin.
     */
    public Path getPrivateConfigDir() {
        return privateConfigDir;
    }

    /**
     * @return The Injected {@link PluginContainer} for this plugin.
     */
    public PluginContainer getPluginContainer() {
        return pluginContainer;
    }

    /**
     * @return The Injected {@link ConfigurationFile} for this plugin.
     */
    public ConfigurationFile getConfig() {
        return config;
    }

    /**
     * @return The Injected {@link Logger} for this plugin.
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * @return a set of the {@link InfoModel}s for all the current admins on the server
     */
    public TreeSet<InfoModel> getAdminPlayerCache() {
        return adminPlayerCache;
    }

    /**
     * @return The injected {@link Game} for this plugin.
     */
    public Game getGame() {
        return game;
    }

    /**
     * Class that attempts to push local bans to CPAS web.
     */
    private static class RunLocalBanProcess implements Runnable {

        private final MinecraftCpas pluginInstance;

        /**
         * Creates a new {@link RunLocalBanProcess} object.
         *
         * @param pluginInstance the {@link MinecraftCpas} instance.
         */
        RunLocalBanProcess(MinecraftCpas pluginInstance) {
            this.pluginInstance = pluginInstance;
        }

        @Override
        public void run() {
            final BanService banService = pluginInstance.getGame().getServiceManager().provideUnchecked(BanService.class);
            for (Ban.Profile ban : banService.getProfileBans()) {
                // Set the duration
                final long duration;
                if (ban.getExpirationDate().isPresent()) {
                    duration = ban.getExpirationDate().get().getEpochSecond() - Instant.now().getEpochSecond();
                } else {
                    duration = 0;
                }
                // Only push bans that are permanent or still have time left
                if (ban.isIndefinite() || duration > 0) {
                    //Set the banner id
                    final String bannerId;
                    if (ban.getBanCommandSource().isPresent()) {
                        bannerId = (ban.getBanCommandSource().get() instanceof User ? ((User) ban.getBanCommandSource().get()).getUniqueId().toString() : "");
                    } else {
                        bannerId = "";
                    }
                    final GameProfile profile = ban.getProfile();
                    // Push ban to cpas
                    Cpas.getInstance().banUser(
                            profile.getUniqueId().toString(),
                            profile.getName().orElse("INTERNAL ERROR"),
                            bannerId,
                            new String[]{""},
                            (int) duration * 60,
                            ban.getReason().orElse(Text.of("INTERNAL ERROR")).toPlain(),
                            new ProcessBanResponseFromLocal(ban, pluginInstance));
                }
            }
        }
    }

    /**
     * Class that handles the processing of the response from the ban function (from local to cpas)
     */
    private static class ProcessBanResponseFromLocal implements Cpas.ProcessResponse<SuccessResponseModel> {

        private final MinecraftCpas pluginInstance;
        private final Ban ban;

        /**
         * Creates a new {@link ProcessBanResponseFromLocal} object.
         *
         * @param ban            the ban to remove
         * @param pluginInstance the {@link MinecraftCpas} instance.
         */
        ProcessBanResponseFromLocal(Ban ban, MinecraftCpas pluginInstance) {
            this.pluginInstance = pluginInstance;
            this.ban = ban;
        }

        @Override
        public void process(SuccessResponseModel response, String errorMessage) {
            if (errorMessage == null && response.success) {
                final BanService banService = pluginInstance.getGame().getServiceManager().provideUnchecked(BanService.class);
                banService.removeBan(ban);
            }
        }

        @Override
        public Class<SuccessResponseModel> getModelClass() {
            return SuccessResponseModel.class;
        }
    }
}

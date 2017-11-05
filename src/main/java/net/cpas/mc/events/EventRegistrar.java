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
package net.cpas.mc.events;

import net.cpas.mc.MinecraftCpas;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.game.state.GameInitializationEvent;

import javax.annotation.Nonnull;

/**
 * The class that registers all events for the {@link MinecraftCpas} plugin.
 *
 * @author agent6262
 */
public final class EventRegistrar {

    /**
     * Register all events. This should be invoked once per server lifecycle (most likely in the plugin's
     * {@link GameInitializationEvent}. If invoked more than once per server lifecycle for any reason, this class's
     * author will not be responsible for the stack traces and general mayhem that is caused by doing so.
     *
     * @param pluginInstance the instance of the main plugin class
     */
    public static void register(@Nonnull MinecraftCpas pluginInstance) {
        final EventManager eventManager = Sponge.getEventManager();
        eventManager.registerListeners(pluginInstance, new AuthListener(pluginInstance));
        eventManager.registerListeners(pluginInstance, new LoginListener(pluginInstance));
        eventManager.registerListeners(pluginInstance, new JoinListener(pluginInstance));
        eventManager.registerListeners(pluginInstance, new DisconnectListener(pluginInstance));
        eventManager.registerListeners(pluginInstance, new GameReloadListener(pluginInstance));
    }
}

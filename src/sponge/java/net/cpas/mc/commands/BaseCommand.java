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

import javax.annotation.Nonnull;

import net.cpas.mc.MinecraftCpas;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.text.Text;

/**
 * Contains logic shared by all commands
 *
 * @author oey192
 */
abstract class BaseCommand implements CommandExecutor {

    /**
     * The {@link MinecraftCpas} instance.
     */
    final MinecraftCpas pluginInstance;

    /**
     * Creates a new {@link BaseCommand} object.
     *
     * @param pluginInstance the {@link MinecraftCpas} instance.
     */
    BaseCommand(@Nonnull MinecraftCpas pluginInstance) {
        this.pluginInstance = pluginInstance;
    }

    /**
     * Fetch an argument from the provided {@link CommandContext} and cast it to the provided {@link Class}<{@link T}>
     *
     * @param args  The arguments that should contain the argument to cast
     * @param key   They key to use to extract an argument from the {@link CommandContext}
     * @param clazz The class to cast the key to
     * @param <T>   The type of the return value as specified by the {@code clazz} parameter
     * @return The value extracted from the {@link CommandContext} cast to the requested type
     *
     * @throws CommandException if no arguments with the given {@code key} are present in the {@link CommandContext}
     */
    @Nonnull
    <T> T castArgument(@Nonnull CommandContext args, @Nonnull String key, @Nonnull Class<T> clazz) throws CommandException {
        return args.getOne(Text.of(key))
                .map(clazz::cast)
                .orElseThrow(()->new CommandException(Text.of("'" + key + "' did not match any provided arguments")));
    }
}

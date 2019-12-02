# CpasMinecraft [![License](https://img.shields.io/badge/License-BSD%203--Clause-blue.svg)](https://opensource.org/licenses/BSD-3-Clause)

CpasMinecraft is a server endpoint for connecting to the CPAS REST API. This plugin is intended to be compatible with 
[SpongeVanilla](https://github.com/SpongePowered/SpongeVanilla), [SpongeForge](https://github.com/SpongePowered/SpongeForge) 
and [Spigot](https://www.spigotmc.org/).


## Building
**Note:** If you do not have Gradle installed then use ./gradlew for Unix systems or Git Bash and gradlew.bat for Windows 
systems in place of any 'gradle' command.

In order to build CpasMinecraft just run the `gradle build` command. Once that is finished you will find library, sources, and 
javadoc .jars exported into the `./build/libs` folder and the will be labeled like the following.
```
MinecraftCpas-sponge-x.x.x.jar
MinecraftCpas-spigot-x.x.x.jar
```
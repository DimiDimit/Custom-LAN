# Custom LAN
![Screenshot of Custom LAN](docs/open_to_lan_screen.png)
A **Fabric** mod that allows you to:
* Customize more of your integrated server (Online Mode, PvP, Port, Max Players, MOTD)
* Change the settings mid-game (including the port) and stop the server without quitting the world
* Change who can use cheats individually using the `/op` and `/deop` commands and cheat in singleplayer without opening to LAN (replaces the Allow Cheats button)

It also allows you to start the LAN world by pressing Enter, doesn't force the gamemode (anyone who left in Creative mode will still be in Creative when they join even if the world is set to Survival), and extends the `/publish` command, which can now change settings mid-game as well:
```
/publish [<port>] [<onlineMode>] [<pvpEnabled>] [<maxPlayers>] [<motd>]
/publish stop
```

It **only** requires [Fabric Loader](https://fabricmc.net/use/)—[Fabric API](https://modrinth.com/mod/fabric-api) is not required.

It has been backported to all Minecraft versions supported by Fabric (except the snapshots)!
I'm **not** planning on porting it to **Forge** myself *for now* ([this](https://forums.minecraftforge.net/topic/70592-113how-to-use-mixin-for-forge-modding/?tab=comments#comment-341587), [this](https://forums.minecraftforge.net/topic/97430-forgemixinfabric-question/), [that](https://web.archive.org/web/20210118022002/https://gist.github.com/jellysquid3/8b68b81a5e48462f8690284a0a3c89a1) and [that](https://gist.github.com/The-Fireplace/d092f25e892a46902ecdec68dee2b938) is why), but you're more than welcome to send me a pull request.

## Explanation of `/op` and `/deop`
`/op` and `/deop` work like in dedicated servers.
You can also use them on yourself, the owner. Doing this will add/remove the "Cheats" label on the world (as if you've NBT edited the `allowCommands` field), and you can use these commands without opening to LAN as well, replacing the Vanilla method of opening to LAN, cheating, quitting the world and entering it again.

Operators are persisted per-world in an `ops.json` file inside that world's directory. To allow operators to assign and remove other operators, change their `level` in the `ops.json` file to `3` or higher (the default is `2`, while level `4` allows them to use `/publish`). You, as the host, can always assign and remove operators, even if you're not an operator yourself.

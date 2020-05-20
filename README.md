# polychat-client-bukkit
Polychat Client Bukkit Port WIP

This is a bukkit port of the Polychat Forge mod client, https://github.com/ModdedMinecraftClub/polychat-client

Override feature required additional config file (polychat_override.properties). If it's not there then it will create a sample file.

File is checked every command i.e. it can be tweaked live. Seems a little heavyweight to load file each time but (1) it's small and (2) not many commands happen on an hourly basis.

Example command config for !promote function:
  override_command_ranks=manuadd $3 $4

The following on discord:
  !promote im joe member

Results in:
  manuadd joe member

Using the above config.

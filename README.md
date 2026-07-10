# ChestSort

A Paper plugin for automatic chest and inventory sorting. Requires Paper 26.2 (or newer) and Java 25.

## Download & more information

Please see the related topic at spigotmc.org for information regarding the commands, permissions and download links:

https://www.spigotmc.org/resources/1-13-chestsort.59773/

## API

If you want to use ChestSort's advanced sorting features for your own plugin, you can use the ChestSort API. It provides methods to sort any given inventory, following the rules you have specified in your ChestSort's plugin.yml and the corresponding category files.

More information about the API can be found [HERE](https://github.com/JEFF-Media-GbR/Spigot-ChestSort/blob/master/HOW_TO_USE_API.md).

## Building the .jar file

This project uses Gradle. Build with:

```
gradle build
```

The resulting jar will be at `build/libs/ChestSort-<version>.jar`.

## Technical stuff

ChestSort takes an instance of `org.bukkit.inventory.Inventory` and copies the contents. The resulting array is sorted by rules defined in the config.yml. This takes far less than one millisecond for a whole chest, causing no noticeable lag even on servers where many players are using chests at the same time.

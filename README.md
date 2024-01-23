## What?
Delta is a library designed for Paper developers. 
Its primary aim is to make the process of adding items, blocks, and block entities both easy and efficient.

## How?
Well, Delta uses a LOT of `Unsafe` code to achieve this.
It injects its agent before the server has started (thanks to plugin bootstrappers), and modifies the `Material` class to make it somewhat compatible with Bukkit.
It also replaces some classes like `ServerPlayerGameMode`, `PacketEncoder`, and `PacketDecoder`.

Specifically, Delta replaces `ServerPlayerGameMode` to manage server-side mining, `PacketEncoder` to inject a custom ByteBuf with some overridden methods, and `PacketDecoder` with similar overridden methods.

## Why?
That's... certainly a question.
Well, I felt like making items was not as easy as doing them in Fabric or Forge, since they all hooked into the internal registries of Minecraft (hence they represent modern Minecraft), it was really easy. To check if an item is of a type, you just do `item instanceof MyItem`. That basic. I wanted this simplicity on Paper. And, here you go. Might not be as stable, but I'm certainly going to make it as stable as possible. This is just the early stages after all!

## Who?
The developer of this library is `sodium.zip` (yes i also own [sodium.zip](https://sodium.zip)).
You can support me right [here](https://ko-fi.com/sodium_zip).


# CustomVehicles

Plugin for **Paper 1.21.4 or later** that lets you create configurable vehicles in Minecraft with custom models, basic physics, seats, and an automatically generated resource pack.

The plugin loads each vehicle from its own folder, generates a resource pack with its models and textures, and uses `ItemDisplay` entities to show vehicles in the world.

> Project status: in development. A two-seat car with driver, passengers, keyboard movement, and a custom model has been tested so far.

## Features

- Vehicles defined through YAML files.
- Separate models and textures for each vehicle.
- Automatic ZIP resource pack generation.
- Automatic SHA-1 hash generation for the resource pack.
- Visual models through `ItemDisplay`.
- Forward and backward movement.
- Turning via player controls.
- Configurable acceleration, friction braking, and maximum speed.
- Driver seat.
- Multiple passenger seats with configurable offsets.
- Categories prepared for cars, ~~motorcycles, boats, planes, and helicopters~~ (coming soon).
- Reload definitions and regenerate the resource pack with a command.

## Requirements

- Java 21.
- Paper 1.21.4 or later.
- ProtocolLib compatible with the server version.
- A resource pack loaded by the client.
- Gradle or Maven to build the project.

The plugin **generates** the resource pack, but does not host it or send it to players automatically. To distribute it, you can use another plugin, a web server, or the server's usual setup.

## Installation

1. Install Paper and ProtocolLib.
2. Copy the CustomVehicles JAR into `plugins/`.
3. Start the server once.
4. The plugin will create its data folder and copy the included example vehicles.
5. Configure the vehicles inside `plugins/CustomVehicles/`.
6. Restart the server or use `/vehicle reload`.
7. Host and distribute the generated resource pack.

## Vehicle configuration

Example for `Cars/Sedan/config.yml`:

```yaml
id: "sedan"
display-name: "Sedan"

enabled: true

physics:
  max-forward-speed: 0.50
  max-reverse-speed: 0.20
  acceleration: 0.03
  reverse-acceleration: 0.02
  friction: 0.98
  handling: 4.0
  minimum-turn-speed: 0.01

dimensions:
  width: 1.8
  height: 1.4
  length: 3.8

model:
  file: "model.json"
  scale: 1.0

  offset:
    x: 0.0
    y: 0.0
    z: 0.0

  rotation:
    yaw-offset: 0.0
    pitch-offset: 0.0
    roll-offset: 0.0

  textures:
    "0": "texture.png"

seats:
  driver:
    role: "driver"
    offset:
      x: 0.0
      y: 0.0
      z: 0.0

  passengers:
    - id: "front_passenger"
      offset:
        x: 0.45
        y: 0.0
        z: 0.25

    - id: "rear_left"
      offset:
        x: -0.45
        y: 0.0
        z: -0.75

    - id: "rear_right"
      offset:
        x: 0.45
        y: 0.0
        z: -0.75
```

## Physics parameters

### `max-forward-speed`

Maximum forward speed.

### `max-reverse-speed`

Maximum reverse speed. It is configured as a positive value, although internally it is used as a negative speed.

### `acceleration`

Speed increase when accelerating forward.

### `reverse-acceleration`

Speed increase when accelerating in reverse.

### `friction`

Factor applied to speed on each update. It must be between `0` and `1`.

A value close to `1` keeps momentum for longer.

### `handling`

Number of degrees the heading changes when turning.

### `minimum-turn-speed`

Minimum speed required to turn.

## Models and textures

The model is exported from Blockbench as a model compatible with Java Block/Item.

Example of a texture reference inside `model.json`:

```json
{
  "textures": {
    "0": "texture"
  },
  "elements": [
    {
      "from": [0, 0, 0],
      "to": [16, 16, 16],
      "faces": {
        "north": {
          "uv": [0, 0, 16, 16],
          "texture": "#0"
        }
      }
    }
  ]
}
```

The YAML key must match the key used by the model faces:

```yaml
textures:
  "0": "texture.png"
```

```json
"texture": "#0"
```

During generation, the plugin rewrites the texture reference and creates a structure like this:

## Seats

The main Slime currently works as:

- Physical body of the vehicle.
- Reference point.
- Driver seat.

Passengers use independent Slimes.

### Current driver limitation

The configured driver offset is not applied yet, because the driver is mounted directly on the main body.

It is temporarily recommended to use:

```yaml
seats:
  driver:
    offset:
      x: 0.0
      y: 0.0
      z: 0.0
```

Passengers do use their configured offsets.

## Commands

### Spawn a vehicle

```text
/vehicle spawn <id>
```

Example:

```text
/vehicle spawn astral
```

### List vehicles

```text
/vehicle list
```

### Reload definitions

```text
/vehicle reload
```

This command:

- Reloads the global `config.yml`.
- Reloads vehicle definitions.
- Regenerates the resource pack if it is integrated into the reload method.

## Permissions

```text
customvehicles.admin.reload
```

Allows use of:

```text
/vehicle reload
```

By default, it is assigned to operators.

## Add a new vehicle

1. Create a folder in the corresponding category.
2. Add `config.yml`.
3. Add the JSON model.
4. Add all PNG textures.
5. Check that the texture keys match the model references.
6. Run `/vehicle reload`.
7. Check the generated ZIP.
8. Update the hosted resource pack.
9. Reload the pack on the client.
10. Spawn the vehicle with `/vehicle spawn <id>`.

Example:

```text
Cars/
└── SportsCar/
    ├── config.yml
    ├── model.json
    └── texture.png
```

## Troubleshooting

### The model appears black and purple

Minecraft cannot find a texture or cannot load the model.

Check:

- That the PNG exists.
- That the PNG is valid.
- That the YAML key matches `#key` in the faces.
- That the ZIP contains the texture in the expected path.
- That the client has downloaded the latest version of the pack.
- That the SHA-1 has been updated.

### The model is invisible

Check:

- That the `ItemDisplay` is created.
- That `setItemModel()` uses the same namespace and ID as the pack.
- That the base item is `DIAMOND_BLOCK`.
- That the scale is greater than zero.
- That the offset does not leave the model underground.
- That the JSON does not contain invalid rotations.
- That the client does not log errors while loading the model.

### The model is lagging behind

Check that the model uses the expected position based on the body's velocity.

### Seats shift position

Do not replace vertical correction with the seat's own Y velocity. Seats must calculate the full difference between their current position and target position.

### The vehicle can be walked through

`ItemDisplay` does not provide physical collision. Enable collision on the main body or add invisible collision entities.

### The model only shows part of itself

Check:

- The `from` and `to` bounds of all elements.
- The number and validity of rotations.
- That the model does not use geometry incompatible with Java Block/Item.
- That the exported file is a valid Java model.

### 💬 Need Help or Support?
📖 Wiki: https://rexi666-plugins.gitbook.io/rexi666/customchat

Join my Discord server (Spanish/English):
<p align="center">
  <a href="https://discord.com/invite/a3zkKtrjTr">
    <img src="https://discordapp.com/api/guilds/1025688556779360266/widget.png?style=banner3" alt="Discord Invite"/>
  </a>
</p>

---

## 🙋‍♂️ Author

Made with ❤️ by **Rexi666**

If you enjoy this plugin, consider [donating](https://paypal.me/rexigamer666)!

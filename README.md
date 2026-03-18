
---

# Anti-Air System Mod (Minecraft 1.20.1)

## Overview

**Anti-Air System** is a lightweight, server-side focused Minecraft Forge mod for **1.20.1** that introduces simple but immersive anti-aircraft mechanics using mostly vanilla systems.

The mod enhances crossbows and adds a deployable **Flak 38 anti-air gun**, allowing players to defend against aircraft (especially when used with mods like *Immersive Aircraft*).

This project was designed with a **vanilla-friendly philosophy**, avoiding unnecessary complexity while still delivering dynamic anti-air gameplay.

---

## Features

### 1. Smart Anti-Air Crossbow System

* Crossbow-fired arrows automatically become **anti-air projectiles** when aircraft are nearby.
* Detection is handled dynamically within a configurable radius.
* Modified arrows gain:

  * Increased speed
  * Basic **homing behavior** (only targets aircraft)
* On impact with aircraft:

  * Explosion effect is triggered (visual + feedback)

---

### 2. Projectile Enhancements

* Anti-air arrows:

  * Track nearby aircraft mid-flight
  * Produce particle effects during flight
* Impact system:

  * Detects entity collisions
  * Triggers explosion-like feedback when hitting aircraft targets

---

### 3. Flak 38 Anti-Air Gun

* Deployable anti-aircraft weapon inspired by historical WW2 designs
* Features:

  * Custom model and renderer
  * Manual aiming and firing system
  * Dedicated firing network packets
  * Sound effects for shooting and reloading
* Includes:

  * Ammo system (20mm rounds)
  * Repair kit item

---

### 4. Aircraft Detection System

* Central system that:

  * Scans nearby entities
  * Identifies aircraft targets
  * Used by both:

    * Crossbow system
    * Projectile tracking logic

---

### 5. Fully Integrated Mod Structure

* Custom:

  * Entities (Flak gun)
  * Items (ammo, repair kit)
  * Sounds
  * Creative tab
* Clean registry and modular handler system:

  * `CrossbowHandler`
  * `ProjectileHandler`
  * `AircraftDetector`

---

## Design Philosophy

This mod was built with the idea of:

* Keeping mechanics **simple and effective**
* Extending **vanilla combat systems**
* Remaining **compatible with aircraft mods**
* Avoiding heavy dependencies or bloated systems

---

## Historical Inspiration

The **Flak 38** included in this mod is inspired by real-world German anti-aircraft guns used during **World War II**.

While not intended as a simulation, the mod loosely captures:

* Rapid-fire anti-air defense
* Ground-based aircraft interception
* Visual style of early AA systems

---

## Compatibility

* Minecraft Forge **1.20.1**
* Designed to work well with:

  * Aircraft-based mods (e.g. Immersive Aircraft)
* Server-side friendly (core mechanics do not require client modifications)

---

## License

This project is licensed under the **GPL-3.0 License**.

You are free to:

* Use
* Modify
* Distribute

**As long as:**

* You disclose source code
* Keep the same license

---

## Future Plans (Optional)

* Improved targeting logic
* Config system for detection radius & speed
* More anti-air weapon variants
* Multiplayer balancing

---

## Author

Created as a custom anti-air solution for modded Minecraft gameplay.

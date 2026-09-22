# Free ISS UI

![GPLv3](gplv3-logo.png)     

This project is a free software.

A client side HUD rework for **Iron's Spells 'n Spellbooks** (Minecraft 1.20.1 / Forge).

## What it does

* The flat spell bar is replaced by a **vertical spell deck** hugging the left edge of the screen.
  The deck stays compact while you play, unfolds into the full list while you scroll through your
  spells, and folds itself back together a moment after you stop.
* Changing spells is animated: a short slide for single steps and a longer *swoop* when a spell is
  picked from the spell wheel. Both are accompanied by a soft dial click.
* The focused card shows the spell icon, a selection halo, its remaining cooldown and a gentle
  rotation while you are casting.
* Hold **Alt** to print the complete spell sheet next to the deck: level, rarity, mana, cooldown,
  cast time, spell power, recasts and whatever extra lines the spell itself provides.
* The **inscription table** pages through long spell books. Three rows stay visible, the mouse
  wheel moves through the rest, and a slim scrollbar shows where you are. The slot of the spell
  you selected is always kept on screen.

The mod only changes presentation - no gameplay value is touched.

## Building

```
gradlew build
```

The dependencies (Iron's Spells 'n Spellbooks, Curios, GeckoLib, Iron's Lib, Player Animator) are
bundled in `libs/`, so the build resolves them straight from disk.

## Licence

GPL-3.0. Author: FromtheArakiel.

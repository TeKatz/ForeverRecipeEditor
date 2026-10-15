# Forever Recipe Editor 1.4.3-skyblock.1

Visual in-game editor for shaped and shapeless crafting recipes on Minecraft 1.21.1.

Version 1.4 introduces a complete Forever-themed interface with a restrained gold-and-violet palette, custom buttons, cards, input fields, status banners and slot hover effects. Version 1.4.1 improves slot highlighting, optical alignment and reliable navigation back to the recipe selection after completed actions.

This is the separate `skyblock` branch for Forever Skyblock. Version 1.4.3-skyblock.1 updates the build and required JEI version to 19.64.0.454. The JEI dependency in the generated mod metadata is derived from the same Gradle property used to compile the integration. The regular Forever build remains on `main`.

This branch produces development builds. In-game validation of JEI hover, editor opening, ghost drag, save/reload, rename and delete is still required before a release.

## Requirements

- NeoForge 21.1.249 or newer in the 21.1 line
- JEI **19.64.0.454** (exact version)

## Usage

1. Open any inventory or container where the JEI item list is visible.
2. Hold the configured editor key (Left Alt by default) and left-click an item in JEI.
3. Select an existing crafting recipe or create a new recipe.
4. Drag ghost items from JEI, or click a real inventory stack and use it to fill the crafting grid like in AE2's pattern terminal.
5. Click **Save & Reload**.

Existing recipes can be disabled with **Delete Recipe…**. Deletion is implemented as a clean NeoForge false-condition override, so recipes from Minecraft and other mods can be removed without modifying their original files.

Existing recipes can be moved to a new recipe ID with **Rename Recipe…**. Saving, deleting and renaming return to a freshly loaded recipe selection screen after the server confirms that the data-pack reload completed.

Generated recipes are stored in `config/forever_recipe_editor/generated_pack`.
Existing generated files are backed up below `_backups` before replacement.

Right-click clears an ingredient. Shift + right-click cycles between an exact item and its available item tags. Use the mouse wheel over the result slot to change its output count.

Inventory clicks use Minecraft's real container handling: the stack is actually picked up, can be placed back into any displayed inventory slot, and is only copied into the recipe grid as a pattern ingredient.

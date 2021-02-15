# FortniteAutoExporter
A tool made in Java/Python to automatically import Fortnite Skins into Blender

## Installation
* Download the [latest release](https://github.com/halfuwu/FortniteAutoExporter/releases) of the AutoExporter
* Open Blender and install the **AutoExporterPlugin.py** as an addon (Edit > Preferences > Add-ons)
  * If you get a psk/psa error, make sure to install the latest [Blender PSK/PSA Import Plguin](https://github.com/Befzz/blender3d_import_psk_psa)
* Press **N** in the view port and find the AutoExporter tab
* Select the AutoExporter folder where all of your files are located

## Settings
  * **Game Path** Location to your Fortnite Installation's Pak Files
  * **UE4 Version** The UE4 Version of your Fortnite Installation (GAME_UE4_LATEST works best)
  * **Export Assets** Exports all the meshes and textures (Turn off if you dont need to rexport everything)
  * **Dump Materials** Dumps Materials into json files for reading

  * **Reorient Bones** Toggles Reoriented Bones for Skeletal Meshes
  * **Convert to Quads** Converts all imported meshes' geometry to quads
  * **Texture Imported Messhes** Toggles Texturing for Imported Meshes



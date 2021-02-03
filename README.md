# FortniteAutoExporter
A tool made in Java/Python to automatically import Fortnite Skins into Blender

## Usage

### Exporter
* Download the [latest release](https://github.com/halfuwu/FortniteAutoExporter/releases) of the AutoExporter
* Extract the zip file onto your computer
* Edit the [config.json](#Config) to fit your needs
* Run the RunExporter.bat file

### Importer
* Open Blender and install the **AutoExporterPlugin.py** as an addon (Edit > Preferences > Add-ons)
* Press **N** in the view port and find the AutoExporter tab
* Select the AutoExporter folder where all of your files are located

* Optional Settngs
  * **Reorient Bones** Toggles Reoriented Bones for Skeletal Meshes
  * **Texture Imported Messhes** Toggles Texturing for Imported Meshes


## Config
* **PaksDirectory** : Path to Fortnite's pak folder
* **UEVersion** : Unreal Engine Version
* **mainKey** : The Main AES key to load the paks with (Automatically filled out by the RunExporter.bat)
* **dynamicKeys**: List of AES keys to use for loading the dynamic paks (Automatically filled out by the RunExporter.bat)
* **exportAssets** : Set this to false if you've already exported the models/textures of a skin
* **dumpMaterials** : Dumps material.jsons into a folder for extra reading if needed



## Prerequisites
* [JDK 11](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html) or higher
* The Latest [Blender PSK/PSA Import Plguin](https://github.com/Befzz/blender3d_import_psk_psa)



# FortniteAutoExporter
A tool made in Java/Python to automatically import Fortnite Skins into blender

## Usage

### Exporter
* Download the [latest release](https://github.com/halfuwu/FortniteAutoExporter/releases) of the AutoExporter
* Extract the zip file onto your computer
* Edit the [config.json](#Config) to fit your needs
* Run the RunExporter.bat file

### Importer
* Open Blender and import the python script **autoexporter.py** into the scripting tab
* Press the run script button

* Optional Settngs
  * **bReorientBones** Toggles reoriented bones
  * **textureCharacter** Toggles automatic character texturing


## Config
* **PaksDirectory** : Path to Fortnite's pak folder
* **UEVersion** : Unreal Engine Version
* **EncryptionKey** : AES Key to load the paks with
* **exportAssets** : Set this to false if you've already exported the models/textures of a skin
* **dumpMaterials** : Dumps material.jsons into a folder for extra reading if needed



## Prerequisites
* The Latest [Java Runtime Environment](https://www.oracle.com/java/technologies/javase-server-jre8-downloads.html) and [JDK 11](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html) or higher
* The Latest [Blender PSK/PSA Import Plguin](https://github.com/Befzz/blender3d_import_psk_psa)

## Limitations
* Skin Styles aren't currently supported



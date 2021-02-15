bl_info = {
    "name": "Fortnite Auto Exporter",
    "author": "Half",
    "version": (0, 5, 1),
    "blender": (2, 80, 0),
    "location": "View3D > Sidebar > AutoExporter",
    "description": "Blender Addon for FortniteAutoExporter",
    "category": "AutoExporter",
}

import bpy
import json
import os
import requests
import subprocess
import sys
from io_import_scene_unreal_psa_psk_280 import pskimport, psaimport

def GetConfig(self, context):
    os.chdir(bpy.context.scene.workingDirectory)
    config = json.loads(open("config.json", "r").read())
    context.scene.UEVersion = config.get("UEVersion")
    context.scene.bExportAssets = config.get("exportAssets")
    context.scene.bDumpMaterials = config.get("dumpMaterials")
    context.scene.gamePath = config.get("PaksDirectory")
    
    
def SetConfig(self, context):
    os.chdir(bpy.context.scene.workingDirectory)
    config = json.load(open("config.json", "r+"))
    config["UEVersion"] = context.scene.UEVersion
    config["exportAssets"] = context.scene.bExportAssets
    config["dumpMaterials"] = context.scene.bDumpMaterials
    config["PaksDirectory"] = context.scene.gamePath
    json.dump(config, open("config.json", "w"), indent = 4)
    f.write(bpy.context.scene.workingDirectory)    

class AutoExporterPanel(bpy.types.Panel):
    bl_label = "Fortnite Auto Exporter"
    bl_idname = 'AUTOEXPORTER_PT_Main'
    bl_space_type = 'VIEW_3D'
    bl_region_type = 'UI'
    bl_category = 'AutoExporter'

    def draw(self, context):
        layout = self.layout

        row = layout.row()
        row.label(text="Config Settings:")
        
        row = layout.row()
        row.prop(context.scene, "workingDirectory")
        
        if context.scene.workingDirectory != "":
            row = layout.row()
            row.prop(context.scene, "gamePath")
        
            row = layout.row()
            row.prop(context.scene, "UEVersion")
            row = layout.row()
            row.prop(context.scene, "bExportAssets")
            row = layout.row()
            row.prop(context.scene, "bDumpMaterials")
            
            row = layout.row()
            row.label(text="Import Settings:")

            row = layout.row()
            row.prop(context.scene, "bReorientBones")
            row = layout.row()
            row.prop(context.scene, "bConvertToQuads")
            row = layout.row()
            row.prop(context.scene, "bTextureObject")
            
            row = layout.row()
            row.label(text="Export Settings:")

            row = layout.row()
            row.prop(context.scene, "ExportTypes")

            if context.scene.ExportTypes == "Character":
                row = layout.row()
                row.label(text="Enter Character Name or CID:")
            elif context.scene.ExportTypes == "Backpack":
                row = layout.row()
                row.label(text="Enter Backpack Name or BID:")
            elif context.scene.ExportTypes == "Glider":
                row = layout.row()
                row.label(text="Enter Glider Name or Glider ID:")
            elif context.scene.ExportTypes == "Pickaxe":
                row = layout.row()
                row.label(text="Enter Pickaxe Name or Pickaxe ID:")
            elif context.scene.ExportTypes == "Weapon":
                row = layout.row()
                row.label(text="Enter Weapon WID Path:")
            elif context.scene.ExportTypes == "Mesh":
                row = layout.row()
                row.label(text="Enter Mesh Path:")
                
            row = layout.row()
            row.prop(context.scene, "Selection")
            
            row = layout.row()
            row.operator("autoexporter.export", icon='EXPORT')

class AutoExporterMain():

    def MakeCollection(CollectionName):
        if not CollectionName in bpy.data.collections:
            bpy.ops.object.select_all(action='DESELECT')
            bpy.ops.collection.create(name=CollectionName)
            bpy.context.scene.collection.children.link(bpy.data.collections[CollectionName])
            bpy.context.view_layer.active_layer_collection = bpy.context.view_layer.layer_collection.children[
                CollectionName]

    def FormatMeshPath(path: str):
        path = path[1:] if path.startswith("/") else path
        return os.path.join(bpy.context.scene.workingDirectory, "UmodelExport", path) + ".psk"

    def FormatImagePath(MaterialLocation):
        path = MaterialLocation[1:] if MaterialLocation.startswith("/") else MaterialLocation
        return os.path.join(bpy.context.scene.workingDirectory, "UmodelExport", path + ".tga")

    def ImportMeshes(PartSet, ImportedCharacterParts, processed):
        if PartSet in processed and PartSet != None:
            for Part in processed.get(PartSet):
                FormattedMeshPath = AutoExporterMain.FormatMeshPath(
                    Part.get("Mesh").replace(Part.get("Mesh").split("/")[-1], Part.get("Mesh").split(".")[1]))

                if not "Empty" in Part.get("Mesh") and FormattedMeshPath != None:
                    if "CharacterPartType" in Part:
                        if Part.get("CharacterPartType") not in ImportedCharacterParts:
                            pskimport(FormattedMeshPath, bpy.context, bReorientBones=bpy.context.scene.bReorientBones)
                            ImportedCharacterParts.append(Part.get("CharacterPartType"))
                    else:
                        pskimport(FormattedMeshPath, bpy.context, bReorientBones=bpy.context.scene.bReorientBones)

    def SetupTextureMeshes(PartSet, obj, Material_name, processed):
        if PartSet in processed:
            for Part in processed.get(PartSet):
                for Material in Part.get("Materials"):
                    if Part["Mesh"].split(".")[1] == obj.name.replace(".mo", ""):
                        if "OverrideMaterialIndex" in Material:
                            AutoExporterMain.TextureMesh(Material["Material"].split(".")[1], Material.get("OverrideMaterialIndex"),
                             PartSet, True, obj, processed)
                    for slot in obj.material_slots:
                        if Material["Material"].split(".")[1] == slot.name:
                            if "OverrideMaterialIndex" in Material:
                                AutoExporterMain.TextureMesh(Material["Material"].split(".")[1],
                                                             Material.get("OverrideMaterialIndex"),
                                                             PartSet, True, obj, processed)
                            else:
                                if Material.get("Material").split(".")[1].lower() == Material_name.lower():
                                    AutoExporterMain.TextureMesh(Material["Material"].split(".")[1], Material_name,
                                                                 PartSet, False, obj,
                                                                 processed)

    def TextureMesh(TargetMaterial, DefaultMaterial, PartType, bUseIndex, obj, processed):
        bOctaneNodes = True if bpy.context.scene.render.engine.lower() == "octane" else False

        if bUseIndex == True:
            mat = bpy.data.materials[obj.material_slots[:][DefaultMaterial].name]
        else:
            if DefaultMaterial in bpy.data.materials.keys():
                mat = bpy.data.materials[DefaultMaterial]
            else:
                return

        mat.use_nodes = True
        nodes = mat.node_tree.nodes
        links = mat.node_tree.links
        nodes.clear()

        mat.name = TargetMaterial

        if bOctaneNodes == True:
            ShaderDirectory = bpy.context.scene.workingDirectory + "\\Shaders\\octaneshader.blend\\NodeTree\\"
            MainShaderName = "OctaneShader"
            bpy.ops.wm.append(directory=ShaderDirectory, filename=MainShaderName)

            OutputNode = nodes.new(type="ShaderNodeOutputMaterial")
            OutputNode.location = 200, 0

            MainShaderGroup = nodes.new("ShaderNodeGroup")
            MainShaderGroup.node_tree = bpy.data.node_groups[MainShaderName]

        else:

            ShaderDirectory = bpy.context.scene.workingDirectory + "\\Shaders\\shader.blend\\NodeTree\\"
            MainShaderName = "Fortnite Auto Exporter Shader"
            bpy.ops.wm.append(directory=ShaderDirectory, filename=MainShaderName)

            OutputNode = nodes.new(type="ShaderNodeOutputMaterial")
            OutputNode.location = 200, 0

            MainShaderGroup = nodes.new("ShaderNodeGroup")
            MainShaderGroup.node_tree = bpy.data.node_groups[MainShaderName]
            MainShaderGroup.inputs[3].default_value = 1

        if PartType == "VariantMaterials":
            for TestMaterial in processed[PartType]:
                if TestMaterial["VariantMaterial"].split(".")[1] == TargetMaterial:
                    Material = TestMaterial

        else:
            for CharacterPart in processed[PartType]:
                for TestMaterial in CharacterPart["Materials"]:
                    if TestMaterial["Material"].split(".")[1] == TargetMaterial:
                        Material = TestMaterial

        Param = ""
        if "VariantParams" in processed:
            for Param in processed["VariantParams"]:
                if TargetMaterial.lower() == Param["MaterialToAlter"].split(".")[1].lower():
                    Param = Param
        if "Diffuse" in Material:
            if bOctaneNodes == True:
                Diffuse = nodes.new(type="ShaderNodeOctImageTex")
                Diffuse.image = bpy.data.images.load(
                    AutoExporterMain.FormatImagePath(
                        (Material if "Diffuse" not in Param else Param)["Diffuse"].split(".")[0]))
                Diffuse.location = -400, -75
                Diffuse.hide = True
                Diffuse.select = True
                links.new(Diffuse.outputs[0], MainShaderGroup.inputs[0])
            else:
                Diffuse = nodes.new(type="ShaderNodeTexImage")
                Diffuse.image = bpy.data.images.load(
                    AutoExporterMain.FormatImagePath(
                        (Material if "Diffuse" not in Param else Param)["Diffuse"].split(".")[0]))
                Diffuse.location = -400, -75
                Diffuse.hide = True
                Diffuse.select = True
                links.new(Diffuse.outputs[0], MainShaderGroup.inputs[0])

        if "M" in Material:
            if bOctaneNodes == True:
                M = nodes.new(type="ShaderNodeOctImageTex")
                M.image = bpy.data.images.load(
                    AutoExporterMain.FormatImagePath((Material if "M" not in Param else Param)["M"].split(".")[0]))
                M.location = -400, -100
                M.hide = True
                links.new(M.outputs[0], MainShaderGroup.inputs[5])
            else:
                M = nodes.new(type="ShaderNodeTexImage")
                M.image = bpy.data.images.load(
                    AutoExporterMain.FormatImagePath((Material if "M" not in Param else Param)["M"].split(".")[0]))
                M.location = -400, -100
                M.hide = True
                links.new(M.outputs[0], MainShaderGroup.inputs[2])

        if "Normals" in Material:
            if bOctaneNodes == True:
                Normals = nodes.new(type="ShaderNodeOctImageTex")
                Normals.image = bpy.data.images.load(
                    AutoExporterMain.FormatImagePath(
                        (Material if "Normals" not in Param else Param)["Normals"].split(".")[0]))
                Normals.location = -400, -125
                Normals.hide = True
                Normals.inputs[1].default_value = 1.000
                links.new(Normals.outputs[0], MainShaderGroup.inputs[2])
            else:
                Normals = nodes.new(type="ShaderNodeTexImage")
                Normals.image = bpy.data.images.load(
                    AutoExporterMain.FormatImagePath(
                        (Material if "Normals" not in Param else Param)["Normals"].split(".")[0]))
                Normals.image.colorspace_settings.name = "Linear"
                Normals.location = -400, -125
                Normals.hide = True
                links.new(Normals.outputs[0], MainShaderGroup.inputs[2])

        if "SpecularMasks" in Material:
            if bOctaneNodes == True:
                SpecularMasks = nodes.new(type="ShaderNodeOctImageTex")
                SpecularMasks.image = bpy.data.images.load(AutoExporterMain.FormatImagePath(
                    (Material if "SpecularMasks" not in Param else Param)["SpecularMasks"].split(".")[0]))
                SpecularMasks.image.colorspace_settings.name = "Linear"
                SpecularMasks.location = -400, -175
                SpecularMasks.hide = True
                SpecularMasks.inputs[1].default_value = 1.000
                links.new(SpecularMasks.outputs[0], MainShaderGroup.inputs[1])
            else:
                SpecularMasks = nodes.new(type="ShaderNodeTexImage")
                SpecularMasks.image = bpy.data.images.load(AutoExporterMain.FormatImagePath(
                    (Material if "SpecularMasks" not in Param else Param)["SpecularMasks"].split(".")[0]))
                SpecularMasks.image.colorspace_settings.name = "Linear"
                SpecularMasks.location = -400, -175
                SpecularMasks.hide = True
                links.new(SpecularMasks.outputs[0], MainShaderGroup.inputs[4])

        if "Emissive" in Material and "FX" not in Material["Emissive"]:
            if bOctaneNodes == True:
                Emissive = nodes.new(type="ShaderNodeOctImageTex")
                Emissive.image = bpy.data.images.load(
                    AutoExporterMain.FormatImagePath(
                        (Material if "Emissive" not in Param else Param)["Emissive"].split(".")[0]))
                Emissive.location = -400, -325
                Emissive.hide = True
                links.new(Emissive.outputs[0], MainShaderGroup.inputs[3])
            else:
                Emissive = nodes.new(type="ShaderNodeTexImage")
                Emissive.image = bpy.data.images.load(
                    AutoExporterMain.FormatImagePath(
                        (Material if "Emissive" not in Param else Param)["Emissive"].split(".")[0]))
                Emissive.location = -400, -325
                Emissive.hide = True
                links.new(Emissive.outputs[0], MainShaderGroup.inputs[11])

        if "SkinFX_Mask" in Material:
            if bOctaneNodes == True:
                SkinFX_Mask = nodes.new(type="ShaderNodeOctImageTex")
                SkinFX_Mask.image = bpy.data.images.load(
                    AutoExporterMain.FormatImagePath(
                        (Material if "SkinFX_Mask" not in Param else Param)["SkinFX_Mask"].split(".")[0]))
                SkinFX_Mask.location = -800, -100
                SkinFX_Mask.hide = True
            else:
                SkinFX_Mask = nodes.new(type="ShaderNodeTexImage")
                SkinFX_Mask.image = bpy.data.images.load(
                    AutoExporterMain.FormatImagePath(
                        (Material if "SkinFX_Mask" not in Param else Param)["SkinFX_Mask"].split(".")[0]))
                SkinFX_Mask.location = -800, -100
                SkinFX_Mask.hide = True
            SkinFX_MaskFrame = nodes.new(type="NodeFrame")
            SkinFX_MaskFrame.location = -800, -100
            SkinFX_Mask.parent = SkinFX_MaskFrame
            SkinFX_MaskFrame.label = "SkinFX_Mask"

        if "emissive mult" in Material:
            if bOctaneNodes == True:
                bpy.data.node_groups["OctaneShader"].inputs[4].default_value = Material["emissive mult"]
            else:
                bpy.data.node_groups["Fortnite Auto Exporter Shader"].inputs[12].default_value = Material[
                    "emissive mult"]

        if "Skin Boost Color And Exponent" in Material:
            vector = Material["Skin Boost Color And Exponent"].split(",")
            if bOctaneNodes == True:
                bpy.data.materials[str(mat.name)].node_tree.nodes["Group"].inputs[6].default_value = float(
                    vector[0]), float(vector[1]), float(vector[2]), float(vector[3])
            else:
                bpy.data.materials[str(mat.name)].node_tree.nodes["Group"].inputs[17].default_value = float(
                    vector[0]), float(vector[1]), float(vector[2]), float(vector[3])

        links.new(MainShaderGroup.outputs[0], OutputNode.inputs[0])

    def Import(self):
        os.chdir(bpy.context.scene.workingDirectory)
        processed = json.loads(open("processed.json", "r").read())

        ObjectName = processed.get("ObjectName")
        AutoExporterMain.MakeCollection(ObjectName)

        ImportedCharacterParts = []
        AutoExporterMain.ImportMeshes("VariantCharacterParts", ImportedCharacterParts, processed)
        AutoExporterMain.ImportMeshes("CharacterParts", ImportedCharacterParts, processed)

        bpy.ops.object.select_all(action="DESELECT")
        for obj in bpy.data.collections[ObjectName].all_objects:
            if obj.type == 'MESH':
                bpy.context.view_layer.objects.active = obj
                obj.select_set(True)
                bpy.ops.object.shade_smooth()
                obj.data.use_auto_smooth = 0
                if bpy.context.scene.bConvertToQuads == True:
                    bpy.ops.object.editmode_toggle()
                    bpy.ops.mesh.tris_convert_to_quads(uvs=True)
                    bpy.ops.object.editmode_toggle()

        
        if bpy.context.scene.bTextureObject == True:
            for obj in bpy.data.collections[ObjectName].all_objects:
                if ".mo" in obj.name:
                    bpy.context.view_layer.objects.active = obj
                    for material_slot in obj.material_slots:
                        material_name = material_slot.material.name
                        if "VariantMaterials" in processed:
                            for VariantMaterial in processed.get("VariantMaterials"):
                                OriginalMaterial = VariantMaterial.get("OriginalMaterial").split(".")[1]
                                OverrideMaterial = VariantMaterial.get("VariantMaterial").split(".")[1]

                                if material_name.lower() == OriginalMaterial.lower():
                                    print(f"Texturing {OriginalMaterial} with {OverrideMaterial}")
                                    AutoExporterMain.TextureMesh(OverrideMaterial, OriginalMaterial,
                                                                 "VariantMaterials", False, obj,
                                                                 processed)

                        AutoExporterMain.SetupTextureMeshes("VariantCharacterParts", obj, material_name, processed)
                        AutoExporterMain.SetupTextureMeshes("CharacterParts", obj, material_name, processed)

class AutoExporterExport(bpy.types.Operator):
    bl_idname = "autoexporter.export"
    bl_label = "Launch Exporter"

    def execute(self, context):
        os.chdir(bpy.context.scene.workingDirectory)
        
        objectResponse = requests.get(f"https://benbotfn.tk/api/v1/cosmetics/br/search/all?lang=en&searchLang=en&matchMethod=full&name={bpy.context.scene.Selection}").text.replace(" ", "%20")
        if objectResponse == "[]" and context.scene.ExportTypes != "Mesh" and context.scene.ExportTypes != "Weapon" :
            raise Exception(f'Cosmetic "{context.scene.Selection}" Doesn\'t Exist!')
            
        resultcode = os.system(f'START /WAIT "{context.scene.workingDirectory}" java -jar -Xmx4G FortniteAutoExporter.jar -{bpy.context.scene.ExportTypes} -"{bpy.context.scene.Selection}')
        if resultcode != 0:
            raise Exception("Something went wrong while exporting")
        else:
            AutoExporterMain.Import(self)
        return {'FINISHED'}

class AutoExporterTypes():

    ExportTypes = (
        ("Backpack", "Backpack", ""),
        ("Character", "Character", ""),
        ("Glider", "Glider", ""),
        ("Pickaxe", "Pickaxe", ""),
        ("Weapon", "Weapon", ""),
        ("Mesh", "Mesh", ""),
    )

    UE4Versions = (
        ("GAME_UE4_0", "GAME_UE4_0", ""),
        ("GAME_UE4_1", "GAME_UE4_1", ""),
        ("GAME_UE4_2", "GAME_UE4_2", ""),
        ("GAME_UE4_3", "GAME_UE4_3", ""),
        ("GAME_UE4_4", "GAME_UE4_4", ""),
        ("GAME_UE4_5", "GAME_UE4_5", ""),
        ("GAME_UE4_6", "GAME_UE4_6", ""),
        ("GAME_UE4_7", "GAME_UE4_7", ""),
        ("GAME_UE4_8", "GAME_UE4_8", ""),
        ("GAME_UE4_9", "GAME_UE4_9", ""),
        ("GAME_UE4_10", "GAME_UE4_10", ""),
        ("GAME_UE4_11", "GAME_UE4_11", ""),
        ("GAME_UE4_12", "GAME_UE4_12", ""),
        ("GAME_UE4_13", "GAME_UE4_13", ""),
        ("GAME_UE4_14", "GAME_UE4_14", ""),
        ("GAME_UE4_15", "GAME_UE4_15", ""),
        ("GAME_UE4_16", "GAME_UE4_16", ""),
        ("GAME_UE4_17", "GAME_UE4_17", ""),
        ("GAME_UE4_18", "GAME_UE4_18", ""),
        ("GAME_UE4_19", "GAME_UE4_19", ""),
        ("GAME_UE4_20", "GAME_UE4_20", ""),
        ("GAME_UE4_21", "GAME_UE4_21", ""),
        ("GAME_UE4_22", "GAME_UE4_22", ""),
        ("GAME_UE4_23", "GAME_UE4_23", ""),
        ("GAME_UE4_24", "GAME_UE4_24", ""),
        ("GAME_UE4_25", "GAME_UE4_25", ""),
        ("GAME_UE4_26", "GAME_UE4_26", ""),
        ("GAME_VALORANT", "GAME_VALORANT", ""),
        ("GAME_UE4_LATEST", "GAME_UE4_LATEST", ""),
    )

class AutoExporterSettings(bpy.types.PropertyGroup):
    scene = bpy.types.Scene

    scene.gamePath = bpy.props.StringProperty(name="Game Path", subtype='DIR_PATH', update=SetConfig)
    scene.workingDirectory = bpy.props.StringProperty(name="Exporter Path", subtype='DIR_PATH', update=GetConfig)
    scene.UEVersion = bpy.props.EnumProperty(name="UE4 Version", items=AutoExporterTypes.UE4Versions, update=SetConfig)
    scene.bExportAssets = bpy.props.BoolProperty(name="Export Assets", default=True, update=SetConfig)
    scene.bDumpMaterials = bpy.props.BoolProperty(name="Dump Materials", default=False, update=SetConfig)

    scene.bReorientBones = bpy.props.BoolProperty(name="Reorient Bones", default=False)
    scene.bConvertToQuads = bpy.props.BoolProperty(name="Convert to Quads", default=False)
    scene.bTextureObject = bpy.props.BoolProperty(name="Texture Imported Meshes", default=True)

    scene.ExportTypes = bpy.props.EnumProperty(name="Export Type", items=AutoExporterTypes.ExportTypes, default="Character")
    scene.Selection = bpy.props.StringProperty(name="")


classes = [AutoExporterPanel, AutoExporterExport, AutoExporterSettings]

def register():
    
    subprocess.check_call([sys.executable, '-m', 'pip', 'install', 'requests'])
    
    for cls in classes:
        bpy.utils.register_class(cls)
    bpy.types.Scene.AutoExporterSettings = bpy.props.PointerProperty(type=AutoExporterSettings)

def unregister():
    for cls in classes:
        bpy.utils.unregister_class(cls)

if __name__ == "__main__":
    register()

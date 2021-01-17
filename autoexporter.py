"""
Fortnite Auto Exporter by Half
"""

# SETTINGS

workingDirectory = r"D:\Blender Files\Fortnite Auto Exporter"

bReorientBones = True
textureCharacter = True

# CODE -------------------------------------------- DO NOT EDIT UNLESS NECESSARY -------------------------------- CODE

import bpy
import json
import os
from io_import_scene_unreal_psa_psk_280 import pskimport


def objectCollection():
    if not objectName in bpy.data.collections:
        new_collection = bpy.ops.collection.create(name=objectName)
        bpy.context.scene.collection.children.link(bpy.data.collections[objectName])
    for collection in bpy.context.view_layer.layer_collection.children:
        if collection.name == objectName:
            bpy.context.view_layer.active_layer_collection = collection


def formatImagePath(indexPassthrough, textureType):
    path = processed["Materials"][indexPassthrough][textureType] + ".tga"
    imagePath = path[1:] if path.startswith("/") else path
    return os.path.join(
        workingDirectory,
        "UmodelExport",
        imagePath,
    )


def formatMeshPath(path: str):
    path = path[1:] if path.startswith("/") else path
    return os.path.join(workingDirectory, "UmodelExport", path) + ".psk"

def textureSkin(NameOrIndex, Index, useMaterialSlotIndex):
    if useMaterialSlotIndex == True:
        mat = bpy.data.materials[obj.material_slots[:][NameOrIndex].name]
    if useMaterialSlotIndex == False:
        mat = bpy.data.materials[NameOrIndex]
    mat.use_nodes = True
    mat.name = processed["Materials"][Index]["MaterialName"]

    nodes = mat.node_tree.nodes

    nodes.clear()

    shaderfile = workingDirectory + "\\shader.blend\\NodeTree\\"
    shaderobject = "Fortnite Auto Exporter Shader"

    bpy.ops.wm.append(directory=shaderfile, filename=shaderobject)

    links = mat.node_tree.links

    group = nodes.new("ShaderNodeGroup")
    group.node_tree = bpy.data.node_groups[shaderobject]
    group.inputs[3].default_value = 1

    node_output = nodes.new(type="ShaderNodeOutputMaterial")
    node_output.location = 200, 0

    # TEXTURE CHECKING

    if "Diffuse" in processed["Materials"][Index]:
        node = nodes.new(type="ShaderNodeTexImage")
        node.image = bpy.data.images.load(formatImagePath(Index, "Diffuse"))
        node.location = -400, -75
        node.hide = True
        link = links.new(node.outputs[0], group.inputs[0])
        node.select = True

    if "M" in processed["Materials"][Index]:
        node = nodes.new(type="ShaderNodeTexImage")
        node.image = bpy.data.images.load(formatImagePath(Index, "M"))
        node.location = -400, -100
        node.hide = True
        link = links.new(node.outputs[0], group.inputs[1])

    if "Normals" in processed["Materials"][Index]:
        node = nodes.new(type="ShaderNodeTexImage")
        node.image = bpy.data.images.load(formatImagePath(Index, "Normals"))
        node.image.colorspace_settings.name = "Linear"
        node.location = -400, -125
        node.hide = True
        link = links.new(node.outputs[0], group.inputs[2])

    if "SpecularMasks" in processed["Materials"][Index]:
        node = nodes.new(type="ShaderNodeTexImage")
        node.image = bpy.data.images.load(formatImagePath(Index, "SpecularMasks"))
        node.image.colorspace_settings.name = "Linear"
        node.location = -400, -175
        node.hide = True
        link = links.new(node.outputs[0], group.inputs[4])

    if "Emissive" in processed["Materials"][Index] and "FX" not in processed["Materials"][Index]["Emissive"]:
        emissive_node = nodes.new(type="ShaderNodeTexImage")
        emissive_node.image = bpy.data.images.load(formatImagePath(Index, "Emissive"))
        emissive_node.location = -400, -325
        emissive_node.hide = True
        link = links.new(emissive_node.outputs[0], group.inputs[11])

    if "SkinFX_Mask" in processed["Materials"][Index]:
        node = nodes.new(type="ShaderNodeTexImage")
        node.image = bpy.data.images.load(formatImagePath(Index, "SkinFX_Mask"))
        node.location = -800, -100
        node.hide = True
        frame = nodes.new(type="NodeFrame")
        frame.location = -800, -100
        node.parent = frame
        frame.label = "SkinFX_Mask"
        
    


    # PARAMETER CHECKING

    if "emissive mult" in processed["Materials"][Index]:
        bpy.data.node_groups["Fortnite Auto Exporter Shader"].inputs[12].default_value = processed["Materials"][Index]["emissive mult"]
        
    if "Skin Boost Color And Exponent" in processed["Materials"][Index]:
        vector = processed["Materials"][Index]["Skin Boost Color And Exponent"].split(",")
        bpy.data.materials[str(mat.name)].node_tree.nodes["Group"].inputs[17].default_value = float(vector[0]), float(vector[1]),float(vector[2]), float(vector[3])
        
    # FINAL ADJUSTMENTS
    
    link = links.new(group.outputs[0], node_output.inputs[0])

    ob = bpy.context.view_layer.objects.active
    ob.select_set(True)

    bpy.ops.object.select_all(action="DESELECT")


os.chdir(workingDirectory)
processed = json.loads(open("processed.json", "r").read())

objectName = processed.get("objectName")
objectCollection()

bpy.context.scene.collection.children[0]

for mesh in processed["Meshes"]:
    formatFilePath = formatMeshPath(mesh)    
    if not os.path.exists(formatFilePath):
        print(f"WARNING: {mesh} not found.")
        continue

    pskimport(formatFilePath, bpy.context, bReorientBones=bReorientBones)


for obj in bpy.context.scene.objects:
    obj.select_set(obj.type == "MESH")
    bpy.ops.object.shade_smooth()
    bpy.ops.object.select_all(action="DESELECT")

for collection in bpy.data.collections:
    if objectName in collection.name:
        for obj in collection.all_objects:
            if ".mo" in obj.name:
                bpy.context.view_layer.objects.active = obj

                for material_slot in obj.material_slots:
                    Material_name = material_slot.material.name
                    print("MATERIAL", Material_name)

                    for processedIndex in range(len(processed["Materials"])):
                        
                        if processed["Materials"][processedIndex]["useOverride"] == "false":             
                            if processed["Materials"][processedIndex]["MaterialName"].lower() == Material_name.lower():
                                print(
                                    "PROCESSED INDEX",
                                    processedIndex,
                                    processed["Materials"][processedIndex]["MaterialName"],
                                )
                                textureSkin(Material_name, processedIndex, False)
                                
                        if processed["Materials"][processedIndex]["useOverride"] == "true":
                            if processed["Materials"][processedIndex]["TargetMesh"] == obj.name.replace(".mo", ""):
                                textureSkin(processed["Materials"][processedIndex]["OverrideIndex"], processedIndex, True)                       
                            

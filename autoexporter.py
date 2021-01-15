'''
Fortnite Auto Exporter by Half
'''

import bpy
import json
import os
from io_import_scene_unreal_psa_psk_280 import pskimport, psaimport

# SETTINGS

workingDirectory = "D:\\Blender Files\\Fortnite Auto Exporter\\"

bReorientBones = True
textureCharacter = True

# CODE

def objectCollection():
    if not objectName in bpy.data.collections:
        new_collection = bpy.ops.collection.create(name  = objectName)
        bpy.context.scene.collection.children.link(bpy.data.collections[objectName])
    for collection in bpy.context.view_layer.layer_collection.children:
        if collection.name == objectName:
            bpy.context.view_layer.active_layer_collection = collection 
            
def formatImagePath(indexPassthrough, textureType):
    return workingDirectory + "UmodelExport" + str(processed["Materials"][indexPassthrough][textureType]).replace("/", "\\") + ".tga"
    
def textureSkin(MaterialName, Index):
    mat = bpy.data.materials[MaterialName]
    mat.use_nodes = True

    nodes = mat.node_tree.nodes

    nodes.clear()

    shaderfile = workingDirectory + "\\shader.blend\\NodeTree\\"
    shadersection = "\\NodeTree\\"
    shaderobject = "Fortnite Auto Exporter Shader"

    bpy.ops.wm.append(directory = shaderfile, filename = shaderobject)

    links = mat.node_tree.links

    group = nodes.new('ShaderNodeGroup')
    group.node_tree = bpy.data.node_groups[shaderobject]
    group.inputs[3].default_value = 1

    node_output = nodes.new(type="ShaderNodeOutputMaterial")
    node_output.location = 200, 0
    
    # TEXTURE CHECKING
    
    if "Diffuse" in processed["Materials"][Index]:
        node = nodes.new(type="ShaderNodeTexImage")
        node.image = bpy.data.images.load(formatImagePath(Index, "Diffuse"))
        node.location = -400,-75
        node.hide = True
        link = links.new(node.outputs[0], group.inputs[0])
        node.select = True
        
    if "M" in processed["Materials"][Index]:
        node = nodes.new(type="ShaderNodeTexImage")
        node.image = bpy.data.images.load(formatImagePath(Index, "M"))
        node.location = -400,-100
        node.hide = True
        link = links.new(node.outputs[0], group.inputs[1])

    if "Normals" in processed["Materials"][Index]:
        node = nodes.new(type="ShaderNodeTexImage")
        node.image = bpy.data.images.load(formatImagePath(Index, "Normals"))
        node.image.colorspace_settings.name = 'Linear'
        node.location = -400,-125
        node.hide = True
        link = links.new(node.outputs[0], group.inputs[2])
        
    if "SpecularMasks" in processed["Materials"][Index]:
        node = nodes.new(type="ShaderNodeTexImage")
        node.image = bpy.data.images.load(formatImagePath(Index, "SpecularMasks"))
        node.image.colorspace_settings.name = 'Linear'
        node.location = -400,-175
        node.hide = True
        link = links.new(node.outputs[0], group.inputs[4])
        
    if "Emissive" in processed["Materials"][Index]:
        node = nodes.new(type="ShaderNodeTexImage")
        node.image = bpy.data.images.load(formatImagePath(Index, "Emissive"))
        node.location = -400,-325
        node.hide = True
        link = links.new(node.outputs[0], group.inputs[11])

        
    if "SkinFX_Mask" in processed["Materials"][Index]:
        node = nodes.new(type="ShaderNodeTexImage")
        node.image = bpy.data.images.load(formatImagePath(Index, "SkinFX_Mask"))
        node.location = -800,-100
        node.hide = True
        frame = nodes.new(type="NodeFrame")
        frame.location = -800,-100
        node.parent = frame
        frame.label = "SkinFX_Mask"
        
    # PARAMETER CHECKING
    
    if "emissive mult" in processed["Materials"][Index]:
        bpy.data.node_groups["Fortnite Auto Exporter Shader"].inputs[12].default_value = processed["Materials"][Index]["emissive mult"]
        
        
    link = links.new(group.outputs[0], node_output.inputs[0])

    ob = bpy.context.view_layer.objects.active
    ob.select_set(True)


    bpy.ops.object.select_all(action='DESELECT')
    
    
os.chdir(workingDirectory)
processed = json.loads(open("processed.json", "r").read())

objectName = processed.get("objectName")   
objectCollection()   
        
for i in range(len(processed["Meshes"])):
    formatFilePath = workingDirectory.replace("\\","/") + "/UmodelExport" + processed["Meshes"][i] + ".psk"
    pskimport(formatFilePath, bpy.context, bReorientBones = bReorientBones)
    
    if not os.path.exists(formatFilePath):
        splitPath = processed["Meshes"][i].split("\\")
        print(f"WARNING: {splitPath[len(splitPath)-1]} not found.")
    
for obj in bpy.context.scene.objects:
    obj.select_set(obj.type == "MESH")
    bpy.ops.object.shade_smooth()
    bpy.ops.object.select_all(action='DESELECT')
    
    

for collection in bpy.data.collections:
    if objectName in collection.name:
        for obj in collection.all_objects:
          if ".mo" in obj.name:
            bpy.context.view_layer.objects.active = obj
        
            for slotIndex in range(len(obj.material_slots[:])):
                #print("MATERIAL INDEX", slotIndex, str(obj.material_slots[:][slotIndex].material).replace("<bpy_struct, Material(\"", "").replace("\")>", ""))
                for processedIndex in range(len(processed["Materials"])):
                    if str((processed["Materials"][processedIndex]["MaterialName"])).lower() in str(obj.material_slots[:][slotIndex].material).lower():
                        #print("PROCESSED INDEX", processedIndex, processed["Materials"][processedIndex]["MaterialName"])
                        textureSkin(str(obj.material_slots[:][slotIndex].material).replace("<bpy_struct, Material(\"", "").replace("\")>", ""), processedIndex)
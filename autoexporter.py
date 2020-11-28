'''
Fortnite Auto Exporter by Half
'''

import bpy
import json
import os
from io_import_scene_unreal_psa_psk_280 import pskimport

# SETTINGS

workingDirectory = "D:\\Blender Files\\Fortnite Auto Exporter\\"

bReorientBones = True
textureCharacter = False

# Methods

class Logger():
    def INFO(content):
        print("[{Program}] {Type}: {Content}".format(Program = "FortniteAutoExporter", Type = "INFO", Content = content))
    def ERROR(content):
        print("[{Program}] {Type}: {Content}".format(Program = "FortniteAutoExporter", Type = "ERROR", Content = content))
    def WARN(content):
        print("[{Program}] {Type}: {Content}".format(Program = "FortniteAutoExporter", Type = "WARN", Content = content))
        
def mergeCharacter():
    objects = bpy.context.scene.objects
    
    for obj in bpy.data.collections[characterName].all_objects:
        obj.select_set(obj.type == "ARMATURE")
        
    processedsplitao = processedFile["Meshes"][1].split("\\")
    meshnameao = processedsplitao[len(processedsplitao) -1].replace(".psk", ".ao")

    bpy.context.view_layer.objects.active = bpy.data.objects[meshnameao]
    bpy.ops.object.join()

    bpy.ops.object.editmode_toggle()

    bpy.ops.armature.select_all(action='DESELECT')
    bpy.ops.object.select_pattern(pattern="*.001")
    bpy.ops.object.select_pattern(pattern="*.002")
    bpy.ops.object.select_pattern(pattern="*.003")
    bpy.ops.object.select_pattern(pattern="*.004")
    bpy.ops.object.select_pattern(pattern="*.005")
    bpy.ops.armature.delete()
    bpy.ops.object.editmode_toggle()

    for obj in bpy.data.collections[characterName].all_objects:
        obj.select_set(obj.type == "MESH")
        
    processedsplit = processedFile["Meshes"][1].split("\\")
    meshname = processedsplit[len(processedsplit) -1].replace(".psk", ".mo")

    bpy.context.view_layer.objects.active = bpy.data.objects[meshname]
    bpy.ops.object.editmode_toggle()
    bpy.ops.mesh.select_all(action='SELECT')
    bpy.ops.mesh.tris_convert_to_quads(uvs=True)
    bpy.ops.object.editmode_toggle()
    bpy.ops.object.shade_smooth()
    bpy.ops.object.join()

def textureSkin(MaterialName, Index, MatIndex):
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
    group.inputs[3].default_value = 5

    node_output = nodes.new(type="ShaderNodeOutputMaterial")
    node_output.location = 200, 0

    if "Diffuse" in processedFile["Materials"][MatIndex]:
        node_diffuse = nodes.new(type="ShaderNodeTexImage")
        node_diffuse.image = bpy.data.images.load(processedFile["Materials"][MatIndex]["Diffuse"])
        node_diffuse.image.alpha_mode = 'CHANNEL_PACKED'
        node_diffuse.location = -400,-75
        node_diffuse.hide = True
        link = links.new(node_diffuse.outputs[0], group.inputs[0])

    if "SpecularMasks" in processedFile["Materials"][MatIndex]:
         node_specular = nodes.new(type="ShaderNodeTexImage")
         node_specular.image = bpy.data.images.load(processedFile["Materials"][MatIndex]["SpecularMasks"])
         node_specular.image.alpha_mode = 'CHANNEL_PACKED'
         node_specular.image.colorspace_settings.name = 'Linear'
         node_specular.location = -400,-200
         node_specular.hide = True
         link = links.new(node_specular.outputs[0], group.inputs[6])
    if "Normals" in processedFile["Materials"][MatIndex]:
        node_normal = nodes.new(type="ShaderNodeTexImage")
        node_normal.image = bpy.data.images.load(processedFile["Materials"][MatIndex]["Normals"])
        node_normal.image.alpha_mode = 'CHANNEL_PACKED'
        node_normal.image.colorspace_settings.name = 'Linear'
        node_normal.location = -400,-150
        node_normal.hide = True
        link = links.new(node_normal.outputs[0], group.inputs[4])

    if "M" in processedFile["Materials"][MatIndex]:
        node_M = nodes.new(type="ShaderNodeTexImage")
        node_M.image = bpy.data.images.load(processedFile["Materials"][MatIndex]["M"])
        node_M.image.alpha_mode = 'CHANNEL_PACKED'
        node_M.location = -400,-110
        node_M.hide = True
        link = links.new(node_M.outputs[0], group.inputs[1])

    if "Emissive" in processedFile["Materials"][MatIndex]:
        node_emissive = nodes.new(type="ShaderNodeTexImage")
        node_emissive.image = bpy.data.images.load(processedFile["Materials"][MatIndex]["Emissive"])
        node_emissive.image.alpha_mode = 'CHANNEL_PACKED'
        node_emissive.location = -400,-300
        node_emissive.hide = True
        link = links.new(node_emissive.outputs[0], group.inputs[2])
        
    if "SkinFX_Mask" in processedFile["Materials"][MatIndex]:
        node_skinfx = nodes.new(type="ShaderNodeTexImage")
        node_skinfx.image = bpy.data.images.load(processedFile["Materials"][MatIndex]["SkinFX_Mask"])
        node_skinfx.image.alpha_mode = 'CHANNEL_PACKED'
        node_skinfx.location = -400,-400



    link = links.new(group.outputs[0], node_output.inputs[0])

    ob = bpy.context.view_layer.objects.active
    ob.select_set(True)

    if ob.data.materials:
        ob.data.materials[Index] = mat
    else:
        ob.data.materials.append(mat)

    bpy.ops.object.select_all(action='DESELECT')
    
# Main Code
        
os.chdir(workingDirectory)

f = open("processed.json", "r")
processedJson = f.read()
processedFile = json.loads(processedJson)
Logger.INFO("Reading processed.json")

characterName = processedFile["characterName"]
Logger.INFO(f"Importing Character: {characterName}")

new_collection = ""

new_collection = bpy.data.collections.new(characterName)
bpy.context.scene.collection.children.link(new_collection)

# Create Collection & Import Mesh
    
for collection in bpy.context.view_layer.layer_collection.children:
    if collection.name == characterName:
        bpy.context.view_layer.active_layer_collection = collection

for i in processedFile["Meshes"]:
    pskimport(i, bpy.context, bReorientBones = bReorientBones)
    
mergeCharacter();

ob = bpy.context.view_layer.objects.active

if textureCharacter:
    for index, mat in enumerate(ob.data.materials):
        for e in range(len(processedFile["Materials"])):
            if processedFile["Materials"][e]["MaterialName"] in mat.name:
                print(e, mat.name)
                textureSkin(mat.name, index, e)
        
    
bl_info = {
    "name": "Fortnite Auto Exporter",
    "author": "Half",
    "version": (0, 5, 0),
    "blender": (2, 80, 0),
    "location": "View3D > Sidebar > AutoExporter",
    "description": "Blender Addon for FortniteAutoExporter",
    "warning": "",
    "doc_url": "",
    "category": "AutoExporter",
}


import bpy
import json
import os
from io_import_scene_unreal_psa_psk_280 import pskimport, psaimport

def saveWorkingDirectory(self, context):
    os.chdir(bpy.utils.user_resource('SCRIPTS', "addons"))
    print(bpy.utils.user_resource('SCRIPTS', "addons"))
    f = open("FortniteAutoExporterSettings.txt", "w+")
    f.write(bpy.data.scenes["Scene"].workingDirectory)

class FAESettings(bpy.types.PropertyGroup):
    scene = bpy.types.Scene
    
    os.chdir(bpy.utils.user_resource('SCRIPTS', "addons"))
    f = open("FortniteAutoExporterSettings.txt", "r+")
    
    scene.bReorientBones = bpy.props.BoolProperty(
        name="Reorient Bones",
        default = False
        )
        
    scene.bTextureObject = bpy.props.BoolProperty(
        name="Texture Imported Meshes",
        default = True
    )
    
    scene.workingDirectory = bpy.props.StringProperty(
        name="",
        default = f.read(),
        subtype = 'DIR_PATH',
        update = saveWorkingDirectory
    )
    

class FAE_PT_Main(bpy.types.Panel):
    bl_label = "Fortnite Auto Exporter"
    bl_idname = "FAE_PT_Main"
    bl_space_type = "VIEW_3D"
    bl_region_type = "UI"
    bl_category = "AutoExporter"
    

    def draw(self, context):
        layout = self.layout
        
        row = layout.row()
        row.label(text="AutoExporter Folder:")
        row = layout.row()
        row.prop(context.scene, "workingDirectory")
        
        row = layout.row()
        row.prop(context.scene, "bReorientBones")
        row = layout.row()
        row.prop(context.scene, "bTextureObject")
        
        row = layout.row()
        row.operator("fae.runscript")
        
class FAE_OT_RunScript(bpy.types.Operator):
    bl_idname = "fae.runscript"
    bl_label = "Import"
    
    
    def execute(self, context):
        
        workingDirectory = context.scene.workingDirectory

        def objectCollection():
            if not objectName in bpy.data.collections:
                new_collection = bpy.ops.collection.create(name=objectName)
                bpy.context.scene.collection.children.link(bpy.data.collections[objectName])
            for collection in bpy.context.view_layer.layer_collection.children:
                if collection.name == objectName:
                    bpy.context.view_layer.active_layer_collection = collection


        def formatImagePath(indexPassthrough, textureType, MaterialType):
            path = processed[MaterialType][indexPassthrough][textureType] + ".tga"
            imagePath = path[1:] if path.startswith("/") else path
            return os.path.join(
                workingDirectory,
                "UmodelExport",
                imagePath,
            )


        def formatMeshPath(path: str):
            path = path[1:] if path.startswith("/") else path
            if os.path.exists(os.path.join(workingDirectory, "UmodelExport", path) + ".pskx"):
                print("INFO: Mesh is StaticMesh Type")
                return os.path.join(workingDirectory, "UmodelExport", path) + ".pskx"
            elif os.path.exists(os.path.join(workingDirectory, "UmodelExport", path) + ".psk"):
                print("INFO: Mesh is SkeletalMesh Type")
                return os.path.join(workingDirectory, "UmodelExport", path) + ".psk"
            else:
                if not os.path.exists(os.path.join(workingDirectory, "UmodelExport", path)):
                    print(f"WARNING: {mesh} not found.")
                    return os.path.join(workingDirectory, "UmodelExport", path) + ".NONE"   

        def textureSkin(NameOrIndex, Index, useMaterialSlotIndex, MaterialType):
            if useMaterialSlotIndex == True:
                mat = bpy.data.materials[obj.material_slots[:][NameOrIndex].name]
            if useMaterialSlotIndex == False:
                if NameOrIndex in bpy.data.materials.keys():            
                    mat = bpy.data.materials[NameOrIndex]
                else:
                    return
            mat.use_nodes = True
            
            if MaterialType == "VariantMaterials":
                mat.name = processed["VariantMaterials"][processedIndexVariant]["OverrideMaterial"].split("/")[-1]
            else:
                mat.name = processed[MaterialType][Index]["MaterialName"]

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

            if "Diffuse" in processed[MaterialType][Index]:
                node = nodes.new(type="ShaderNodeTexImage")
                node.image = bpy.data.images.load(formatImagePath(Index, "Diffuse", MaterialType))
                node.location = -400, -75
                node.hide = True
                link = links.new(node.outputs[0], group.inputs[0])
                node.select = True

            if "M" in processed[MaterialType][Index]:
                node = nodes.new(type="ShaderNodeTexImage")
                node.image = bpy.data.images.load(formatImagePath(Index, "M", MaterialType))
                node.location = -400, -100
                node.hide = True
                link = links.new(node.outputs[0], group.inputs[1])

            if "Normals" in processed[MaterialType][Index]:
                node = nodes.new(type="ShaderNodeTexImage")
                node.image = bpy.data.images.load(formatImagePath(Index, "Normals", MaterialType))
                node.image.colorspace_settings.name = "Linear"
                node.location = -400, -125
                node.hide = True
                link = links.new(node.outputs[0], group.inputs[2])

            if "SpecularMasks" in processed[MaterialType][Index]:
                node = nodes.new(type="ShaderNodeTexImage")
                node.image = bpy.data.images.load(formatImagePath(Index, "SpecularMasks", MaterialType))
                node.image.colorspace_settings.name = "Linear"
                node.location = -400, -175
                node.hide = True
                link = links.new(node.outputs[0], group.inputs[4])

            if "Emissive" in processed[MaterialType][Index] and "FX" not in processed[MaterialType][Index]["Emissive"]:
                emissive_node = nodes.new(type="ShaderNodeTexImage")
                emissive_node.image = bpy.data.images.load(formatImagePath(Index, "Emissive", MaterialType))
                emissive_node.location = -400, -325
                emissive_node.hide = True
                link = links.new(emissive_node.outputs[0], group.inputs[11])

            if "SkinFX_Mask" in processed[MaterialType][Index]:
                node = nodes.new(type="ShaderNodeTexImage")
                node.image = bpy.data.images.load(formatImagePath(Index, "SkinFX_Mask", MaterialType))
                node.location = -800, -100
                node.hide = True
                frame = nodes.new(type="NodeFrame")
                frame.location = -800, -100
                node.parent = frame
                frame.label = "SkinFX_Mask"

            # PARAMETER CHECKING

            if "emissive mult" in processed[MaterialType][Index]:
                bpy.data.node_groups["Fortnite Auto Exporter Shader"].inputs[12].default_value = processed[MaterialType][Index]["emissive mult"]
                
            if "Skin Boost Color And Exponent" in processed[MaterialType][Index]:
                vector = processed[MaterialType][Index]["Skin Boost Color And Exponent"].split(",")
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
        
        
        ImportedCharacterParts = []
        i = 0
        for mesh in processed["Meshes"]:
            print(processed["CharacterPartTypes"][i])
            if processed["CharacterPartTypes"][i] in ImportedCharacterParts:
                break
            else:
                ImportedCharacterParts.append(processed["CharacterPartTypes"][i])
            formatFilePath = formatMeshPath(mesh)
            
            #umodel export name fix
            meshPath = formatFilePath.replace(formatFilePath.split("/")[len(formatFilePath.split("/")) - 1], "")
            for j in os.listdir(meshPath):
                if formatFilePath.split("/")[len(formatFilePath.split("/")) - 1].replace(".NONE", "") in j and ".psk" in j and formatFilePath.split("/")[len(formatFilePath.split("/")) - 1].replace(".NONE", "") != j:
                    pskimport(meshPath + j, bpy.context, bReorientBones=context.scene.bReorientBones)

            if not "Empty" in mesh and not formatFilePath == None:
                pskimport(formatFilePath, bpy.context, bReorientBones=context.scene.bReorientBones)
            i += 1


        for obj in bpy.context.scene.objects:
            obj.select_set(obj.type == "MESH")
            bpy.ops.object.shade_smooth()
            bpy.ops.object.select_all(action="DESELECT")
            

        if context.scene.bTextureObject == True:
            for collection in bpy.data.collections:
                if objectName in collection.name:
                    for obj in collection.all_objects:
                        if ".mo" in obj.name:
                            bpy.context.view_layer.objects.active = obj
                            for material_slot in obj.material_slots:
                                Material_name = material_slot.material.name
                                print("MATERIAL", Material_name)
                                
                                if "VariantMaterials" in processed:
                                    for processedIndexVariant in range(len(processed["VariantMaterials"])):
                                        OriginalMaterial = processed["VariantMaterials"][processedIndexVariant]["OriginalMaterial"].split("/")[-1]
                                        OverrideMaterial = processed["VariantMaterials"][processedIndexVariant]["OverrideMaterial"].split("/")[-1]           
                                        
                                        if Material_name.lower() == OriginalMaterial.lower():
                                            print(
                                                "PROCESSED INDEX",
                                                processedIndexVariant,
                                                OverrideMaterial,
                                            )
                                            textureSkin(processed["VariantMaterials"][processedIndexVariant]["OverrideIndex"], processedIndexVariant, True, "VariantMaterials")
                                
                                for processedIndex in range(len(processed["Materials"])):                                           
                                    
                                    if processed["Materials"][processedIndex]["OverrideIndex"] == -1:             
                                        if processed["Materials"][processedIndex]["MaterialName"].lower() == Material_name.lower():
                                            print(
                                                "PROCESSED INDEX",
                                                processedIndex,
                                                processed["Materials"][processedIndex]["MaterialName"],
                                            )
                                            textureSkin(Material_name, processedIndex, False, "Materials")
                                            
                                    if processed["Materials"][processedIndex]["OverrideIndex"] != -1:
                                        if processed["Materials"][processedIndex]["TargetMesh"] == obj.name.replace(".mo", ""):
                                            textureSkin(processed["Materials"][processedIndex]["OverrideIndex"], processedIndex, True, "Materials")    
                                            
                                    if "Glass" in Material_name and not "Diffuse" in processed["Materials"][processedIndex]:
                                        mat = bpy.data.materials[Material_name]
                                        mat.use_nodes = True
                                        mat.blend_method = 'BLEND'
                                        mat.show_transparent_back = False

                                        mat.node_tree.nodes.clear()

                                        glass = mat.node_tree.nodes.new("ShaderNodeBsdfGlass")
                                        glass.location = -200, 100
                                        mix = mat.node_tree.nodes.new("ShaderNodeMixShader")
                                        transparent = mat.node_tree.nodes.new("ShaderNodeBsdfTransparent")
                                        transparent.location = -200, -100
                                        node_output = mat.node_tree.nodes.new(type="ShaderNodeOutputMaterial")
                                        node_output.location = 200, 0
                                        
                                        links = mat.node_tree.links
                                        links.new(glass.outputs[0], mix.inputs[1])
                                        links.new(transparent.outputs[0], mix.inputs[2]) 
                                        links.new(mix.outputs[0], node_output.inputs[0])  
                                    
                                

                                              
                                    

        return {'FINISHED'}
    
classes = [FAE_PT_Main, FAE_OT_RunScript, FAESettings]
        
def register():
    for cls in classes:
        bpy.utils.register_class(cls)
    bpy.types.Scene.FAESettings = bpy.props.PointerProperty(type=FAESettings)    
    
def unregister():
    for cls in classes:
        bpy.utils.unregister_class(cls)
    
if __name__ == "__main__":
    register()

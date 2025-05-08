# Real-time Object Dectection In OSRS

AIM: Real-time detection of various game objects and NPCs inside [Old School RuneScape](https://www.oldschool.runescape.com/). 



## Data Collection
The detection data is collected using a DreamBot script to: 
    - walk in a given path 
    - random camera view
    - get annotations (BBoxes, names, UIDs) for all entities in view 
    - save game client image and annotations

## Training 
Fine-tunning a MS-COCO pretrained YOLO11n model from Ultralytics 

## TODO:    
 - Clean dataset: occlusion  
 - export ONNX model and run inference using onnxruntime




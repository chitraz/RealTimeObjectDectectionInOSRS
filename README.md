# Real-time Object Dectection In OSRS

The goal is the real-time detection of various game objects and NPCs inside [Old School RuneScape](https://www.oldschool.runescape.com/). <br> The dectection categories consists of:
  - 3 types of cow NPCs ('Cow', 'Cow_calf', 'Dairy_cow')
  - 5 types of trees ('Tree', 'Evergreen_tree', 'Oak_tree', 'Willow_tree', 'Yew_tree')
  - 3 game objects from the scenery ('Plant', 'Daisies', 'Thistle')
  - 9 types of mine rocks ('Rocks', 'Copper_rocks', 'Tin_rocks', 'Silver_rocks', 'Iron_rocks', 'Coal_rocks', 'Gold_rocks', 'Mithril_rocks', 'Adamantite_rocks'). 

This repo contains:
  - Dreambot script written to collect detection data from the game client
  - Jupyter Notebooks to prepare the dataset and fine-tune a MS-COCO pretrained YOLO11m model
  - Python script to select a screen region and run detection on it using trained model. 

## Training
[yolo11](https://github.com/ultralytics/ultralytics/blob/main/docs/en/models/yolo11.md)

## Running


```
pip install -r requirements.txt

```

```
python scripts/Detection.py
```


## To Do:    

### Clean dataset:
The main concern for the osrs data is occlusion which can occur for grounditems in a loot pile, 


  - detect occlusion during data collection by the bot
    or 
  - post-processing:
       - feedforward entire dataset through trained model and inspect quality of annoations with low confidence.  
         or 
       - Using segmenation info, get pixels coresponding to each objects and get a image descriptor (GCH?). Compute a mean image descriptor for each class and if a object's descriptor is too far (threshold?) from class mean then it is likely that the object is occluded.

### Effcient inference on the CPU using onnxruntime

### Instance segemnataion:


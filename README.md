# Real-time Object Dectection In OSRS

The goal is the real-time detection of various game objects and NPCs inside [Old School RuneScape](https://www.oldschool.runescape.com/). <br> The dectection categories consists of:
  - 3 types of cow NPCs ('Cow', 'Cow_calf', 'Dairy_cow')
  - 5 types of trees ('Tree', 'Evergreen_tree', 'Oak_tree', 'Willow_tree', 'Yew_tree')
  - 3 game objects from the scenery ('Plant', 'Daisies', 'Thistle')
  - 9 types of mine rocks ('Rocks', 'Copper_rocks', 'Tin_rocks', 'Silver_rocks', 'Iron_rocks', 'Coal_rocks', 'Gold_rocks', 'Mithril_rocks', 'Adamantite_rocks'). 

This repo contains:
  - Dreambot script written to collect detection data from the game
  - Jupyter Notebooks to prepare the dataset, fine-tune a MS-COCO pretrained YOLO11m model and export it as an onnx model
  - Python script to select a screen region and run detection on it using onnxruntime


## Running


```
pip install -r requirements.txt

```

```
python scripts/Detection_onnx.py
```


## TODO:    
 - Clean dataset: occlusion 




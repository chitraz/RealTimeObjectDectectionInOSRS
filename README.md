# Real-time Object Dectection In OSRS

The goal is the real-time detection of various game objects and NPCs inside [Old School RuneScape](https://www.oldschool.runescape.com/). The dectection categories consists of:
  - 3 types of cow NPCs (***Cow, Cow_calf, Dairy_cow***)
  - 5 types of trees (***Tree, Evergreen_tree, Oak_tree, Willow_tree, Yew_tree***)
  - 3 game objects from the scenery (***Plant, Daisies, Thistle***)
  - 10 types of mine rocks (***Rocks, Copper_rocks, Tin_rocks, Silver_rocks, Iron_rocks, Coal_rocks, Gold_rocks, Mithril_rocks, Adamantite_rocks, Clay_rocks***)
<br>

https://github.com/user-attachments/assets/6ff7f330-9faf-402e-bb35-497be3410ffa

<br>

This repo contains:
  - Dreambot script written to collect detection data from the game client ([DataBot.java](scripts/DataBot.java))
  - Jupyter Notebooks to prepare the dataset ([OSRS DATASET.ipynb](notebooks/OSRS_DATASET.ipynb)) and fine-tune a MS-COCO pretrained YOLO11n model ([Yolo11_finetune.ipynb](notebooks/Yolo11_finetune.ipynb))
  - Python script to select a screen region and run detection on it using the trained model ([Detection.py](scripts/Detection.py))

## Data Collection and Training
A [DreamBot](https://dreambot.org/guides/scripter-guide/starting/#4-keep-learning) script is created to automate the data collection and annotation process. The bot walks in a given path, sets a random camera angle/zoom and saves a 520x340 screenshot image from the game. Annotations (UID + name + box + segmentaion shapes) are computed and saved for all game enities (NPCs, GameObjects, GroundItems and Players) in view. *(note that it only checks if an entity is in the 520x340 viewport. Checking if an entity is actually visible, and not fully occulled, is hard!)* 
<br> 

https://github.com/user-attachments/assets/86fab115-05d8-4676-a4bc-37c928759584

<br>

A subset of 21 classes is taken from the collected data and a 80/20 train/valid split is done. On which a YOLO11n model, from [Ultralytics](https://github.com/ultralytics/ultralytics/blob/main/docs/en/models/yolo11.md), is trained, achieving about **84%** **mAP@50** on the validation set. See [results](notebooks/runs/detect/train3) for more detail on training. The training set has 9368 images and validation set has 2296 iamges with the class instance counts as such:
<br> <br>
<img src="https://github.com/user-attachments/assets/8a4adc28-d42e-458a-b652-50028676138a" width="380" />
<img src="https://github.com/user-attachments/assets/e22068b9-64c6-40e6-a0e3-d97c62db8a21" width="372.5" />
<br>

## Running

```
pip install -r requirements.txt

```

```
python scripts/Detection.py
```

## To Do:    

### 1) Clean dataset

The main concern for the collected data is occlusion, see example:<br>
![Screenshot from 2025-05-22 13-15-36](https://github.com/user-attachments/assets/ad6cf8dc-9562-4e1c-8f73-7c8a80b33f3d)
![Screenshot from 2025-05-22 13-16-17](https://github.com/user-attachments/assets/ac1c1ab0-9a09-4f03-aa31-4d680ead1706)

  - detect occlusion during data collection
    or 
  - post-processing:
       - feedforward entire dataset through trained model and inspect quality of annoations with low confidence.  
         or 
       - Using segmenation info, get pixels coresponding to each objects and get a image descriptor (GCH?). Compute a mean image descriptor for each class and if a object's descriptor is too far (threshold?) from class mean then it is likely that the object is occluded.

### 2) Efficient inference on the CPU using onnxruntime

### 3) Instance segmentation



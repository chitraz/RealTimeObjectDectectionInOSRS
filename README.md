# Real-time Object Dectection In OSRS

AIM: Real-time detection of (scope?  ) inside [Old School RuneScape](https://www.oldschool.runescape.com/) (video game). 



## Requirements 

The game runs natively at 50 FPS, so the we have a window of 20ms between each fame to make the predction. 

Catagories to detect?
### Background research
scope? 
NPCs, game items, UI -> inventory, map, chat box -> text data


## TODO:

### Data collection and anotation
- Use botting techniques to read the object info directly from the game to automate the labeling process!
    - java injection? openGL hook? 

### Training 

What detection model to use? 
  - Faster-RCNN
  - Single Shot Detector (SSD)
  - YOLOV5, YOLOV8

  - fine-tuning from pre-train (on MSCOCO?) weights
    - Natural images -> virtual images   




# Real-time Object Dectection In OSRS

AIM: Real-time detection of various NPCs inside [Old School RuneScape](https://www.oldschool.runescape.com/) (video game). 

## Requirements 

The game runs natively at 50 FPS, so the we have a window of 20ms between each fame to make the predction. 

Catagories to detect?


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




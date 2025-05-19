# Real-time Object Dectection In OSRS

The goal is the real-time detection of various game objects and NPCs inside [Old School RuneScape](https://www.oldschool.runescape.com/).

This repo contains:
  - Dreambot script written to collect detection data from the game
  - Jupyter Notebooks to prepare the dataset, fine-tune a MS-COCO pretrained YOLO11m model and export it as an onnx model
  - Python script to select a screen region and run detection on it using onnxruntime

## Running

```
python scripts/Detection_onnx.py
```


## TODO:    
 - Clean dataset: occlusion 




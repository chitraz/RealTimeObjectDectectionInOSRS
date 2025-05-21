#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
@author: chitraz
    - GUI Tool to select a 520x340 screen region from game client 
    - Run dectection on the selected region by inferencing a trained model. Overlays predicted BBs and class labels. 
"""

import os
import time
import numpy as np 
import cv2
from PIL import Image, ImageTk
import pyautogui as pg
import pyscreenshot as ImageGrab
import tkinter as tk

import torch as t
import torchvision as tv
from torchvision import transforms
from torchvision.utils import draw_bounding_boxes
from copy import deepcopy
from ultralytics import YOLO

import onnx
import onnxruntime 

class DetectionWindowTool():
    def __init__(self, root):
        # Detection window size, 520x340 
        self.Window_height = 340
        self.Window_width = 520

        # detection window's location on the screen
        self.DetectionWindow_TL = None
        self.DetectionWindow_BR = None
        
        # save main window root
        self.root = root
        
		# set gui window size
        self.root.geometry(str(self.Window_width + 65)+'x'+str(self.Window_height + 165))
        
        # frame widget for layout control
        frame = tk.Frame(self.root, padx=5, pady=10)
        frame.grid(row=5, column=5)
        
        # Canvas widget with transparent bg and inventory BB. Used for 
        self.canvas = tk.Canvas(frame, bg='white', bd=10, width=self.Window_width + 30, height=self.Window_height + 30)
        self.canvas.pack()
        self.draw_guidelines()
        
        # Button to run calibration to get user selected detection window's loation on screen
        self.calib_button = tk.Button(frame, text = 'Calibrate Detection Window', font = 'Helvetica 12 bold', command = self.start_calibration)
        self.calib_button.pack()
        
        # Button to take a screenshot at selected region and preview it
        self.collect_button = tk.Button(frame, text = 'Take Screenshot', font = 'Helvetica 12 bold', command = self.take_screenshot)
        self.collect_button.pack()
        
        # Button to run Detection 
        self.detection_button = tk.Button(frame, text = 'Run Detection!', font = 'Helvetica 12 bold', command = self.run_detection)
        self.detection_button.pack()
        
    # function to draw guidelines
    def draw_guidelines(self):
        # clear canvas
        self.canvas.delete('all')
        # create a 320x320 box that user can align to region of intrest 
        self.canvas.create_rectangle(0+30,0+30,self.Window_width+30,self.Window_height+30, fill = '')
        self.canvas.create_oval(20, 20, 40, 40, outline = 'blue')
        self.canvas.create_text(160, 20, text="2) Left-Click top-left corner", fill="blue", font=('Helvetica 12 bold'))
        self.canvas.create_text(180, 180, text="1) Align Me! ", fill="black", font=('Helvetica 12 bold'))
        self.canvas.bind("<Button-1>", self.finish_calibration)
        
    # Start Calibration 
    def start_calibration(self):
        # draw guidelines 
        self.draw_guidelines()
                
        # make window transparent 
        self.root.wm_attributes("-alpha", 0.4)
        
    # Finish Calibration
    def finish_calibration(self, event=None):
        # compute location of detection window using left mouse click location
        DetectionWindow_L, DetectionWindow_T = pg.position() 
        self.DetectionWindow_TL = (DetectionWindow_T, DetectionWindow_L)
        self.DetectionWindow_BR = (DetectionWindow_T + self.Window_height, DetectionWindow_L + self.Window_width)    

        # reset window transparency
        self.root.wm_attributes("-alpha", 1)

    def take_screenshot(self):
        # clear canvas
        self.canvas.delete('all')
            
        # get input image
        self.img = get_screenshot(self.DetectionWindow_TL[1], self.DetectionWindow_TL[0], self.DetectionWindow_BR[1], self.DetectionWindow_BR[0])

        # process and put into canvas 
        image_width = self.DetectionWindow_BR[1] - self.DetectionWindow_TL[1]
        image_height = self.DetectionWindow_BR[0] - self.DetectionWindow_TL[0]
        
        # put screenshot on canvas for review
        self.Canvas_Photo = ImageTk.PhotoImage(image = Image.fromarray(self.img))
        self.canvas.create_image(30, 30, image = self.Canvas_Photo,anchor=tk.NW)

        return self.img
        
    
    def run_detection(self):
        # update global var with select screen region values before closing gui
        global Selected_Region 
        Selected_Region = (self.DetectionWindow_TL[1], self.DetectionWindow_TL[0], self.DetectionWindow_BR[1], self.DetectionWindow_BR[0])

        # close gui, no longer needed
        self.root.destroy()
    
    
def get_screenshot(L,T,R,B): #(BB of detection window position on the screen)
    windowRegion = (L,T,R,B)
    img = ImageGrab.grab(bbox = windowRegion, backend="mss", childprocess=False)
    img = np.array(img) 
    return img

if __name__ == '__main__':
    # --------------------------------------------------------------------
    # Get detection window using GUI tool
    # --------------------------------------------------------------------
    
    Selected_Region = None # global var
    rootWindow = tk.Tk()
    GUI_Tool = DetectionWindowTool(rootWindow)
    rootWindow.title('GUI Tool to select detection window on game client')
    rootWindow.mainloop() 
    
    # -------------------------------------------------------------------
    # run Detection 
    # --------------------------------------------------------------------

    # load our trained yolo11n model
    model = YOLO("saves/yolo11m_osrs_21c.pt")

    loop_time = time.time()
    while(True):
        # get input image from selection screen region
        input_img = get_screenshot(Selected_Region[0], Selected_Region[1], Selected_Region[2], Selected_Region[3])

        # pre-process  input image
        input_img = np.transpose(input_img,(2,0,1)) # (HxWxC) -> (CxHxW) 
        input_tensor = t.from_numpy(input_img)

        # create input batch 
        input_batch = transforms.Resize(size =(320,320))(input_tensor)/255
        input_batch = t.unsqueeze(input_batch, 0) # fake minibatch dim

        # run inference 
        results = model(input_batch,verbose=False) #, conf=0.4, iou=0.7)

        # extract predctions (object's box and class name)
        pred_boxes =  deepcopy(results[0].boxes.xyxy)
        x_sf = 520/320
        y_sf = 340/320  
        pred_boxes[:,0] *= x_sf # rescale boxes to original size
        pred_boxes[:,1] *= y_sf
        pred_boxes[:,2] *= x_sf
        pred_boxes[:,3] *= y_sf
        pred_class_ids = deepcopy(results[0].boxes.cls)
        pred_class_names = [results[0].names[int(class_idx)] for class_idx in pred_class_ids]
        pred_conf = t.round(results[0].boxes.conf.cpu()*100, decimals=1).numpy()
        pred_labels = [str(conf) + '% : ' + cls_name for (conf,cls_name) in zip(pred_conf, pred_class_names)]

        # Annotate with predicted BBs and classifications
        annot_img = draw_bounding_boxes(input_tensor, pred_boxes, pred_labels, colors="purple", label_colors="blue", width=2)
        
        # convert to plotable iamge
        annot_img = annot_img.numpy()
        annot_img = np.transpose(annot_img,(1,2,0)) # (CxHxW) -> (HxWxC)


        # determine and display framerate
        FPS = 1 / (time.time() - loop_time)
        loop_time = time.time()
        font = cv2.FONT_HERSHEY_COMPLEX_SMALL
        annot_img = cv2.putText(annot_img, str(round(FPS)), (480,20), font , 1, (256,0,0),1, cv2.LINE_AA)
        
        # BGR -> RGB 
        annot_img = annot_img[:, :, [2, 1, 0]]


        cv2.imshow('Real-time Object Detection in OSRS', annot_img)

        # press 'q' to exit
        if cv2.waitKey(1) == ord('q'):
            cv2.destroyAllWindows()
            break

        
        
    print('Done.')
    

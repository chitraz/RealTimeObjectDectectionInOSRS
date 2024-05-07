#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Tue Dec 27 15:37:56 2022

@author: chitraz

+ GUI Tool to: 
    - select a portion of the screen as the detection window 
    - Run the data collection process (i.e take and save screenshots)
    
+ Runs inference on trained detection model using screenshot images from selected screen region
    - Overlays predicted BBs and classes onto input image 

"""

import os
import time
import numpy as np 
import cv2
from PIL import Image, ImageTk
import pyautogui as pg
import pyscreenshot as ImageGrab
import tkinter as tk

import torch 
from ultralytics import YOLO

class DetectionWindowTool():
    def __init__(self, root):
        # detection window's location on the screen
        self.DetectionWindow_TL = None
        self.DetectionWindow_BR = None
        self.Window_height = 320
        self.Window_width = 320
        
        # save main window root
        self.root = root
        
		# set main window size
        self.root.geometry('385x510')
        
        # frame widget for layout control
        frame = tk.Frame(self.root, padx=5, pady=10)
        frame.grid(row=5, column=5)
        
        # Canvas widget with transparent bg and inventory BB. Used for 
        self.canvas = tk.Canvas(frame, bg='white', bd=10, width=350, height=350)
        self.canvas.pack()
        self.draw_guidelines()
        
        # Button to run calibration to get user selected detection window's loation on screen
        self.calib_button = tk.Button(frame, text = 'Calibrate Detection Window', font = 'Helvetica 12 bold', command = self.start_calibration)
        self.calib_button.pack()
        
        # Button to take a screenshot at selected region and preview it
        self.collect_button = tk.Button(frame, text = 'Take Screenshot', font = 'Helvetica 12 bold', command = self.take_screenshot)
        self.collect_button.pack()
        
        # Button save the screenshot 
        self.collect_button = tk.Button(frame, text = 'Save Screenshot', font = 'Helvetica 12 bold', command = self.save_screenshot)
        self.collect_button.pack()
        
        # Button to run Detection 
        # note: this is used to destory the gui so that we can continue on with detection (see main)
        self.detection_button = tk.Button(frame, text = 'Run Detection!', font = 'Helvetica 12 bold', command = self.run_detection)
        self.detection_button.pack()
        
    # function to draw guidelines
    def draw_guidelines(self):
        # clear canvas
        self.canvas.delete('all')
        # create a 320x320 box that user can align to region of intrest 
        self.canvas.create_rectangle(0+30,0+30,320+30,320+30, fill = '')
        self.canvas.create_oval(28, 28, 32, 32, fill = 'blue')
        self.canvas.create_text(150, 20, text="2) Left-Click top-left corner", fill="blue", font=('Helvetica 12 bold'))
        self.canvas.create_text(180, 180, text="1) Align Me! ", fill="black", font=('Helvetica 12 bold'))
        self.canvas.bind("<Button-1>", self.finish_calibration)
        
    # Start Calibration 
    def start_calibration(self):
        # draw guidelines 
        self.draw_guidelines()
                
        # make window transparent 
        self.root.wm_attributes("-alpha", 0.4)
        self.root.wait_visibility(self.root)
        
    # Finish Calibration
    def finish_calibration(self, event=None):
        # compute location of detection window using left mouse click location
        DetectionWindow_L, DetectionWindow_T = pg.position() 
        self.DetectionWindow_TL = (DetectionWindow_T, DetectionWindow_L)
        self.DetectionWindow_BR = (DetectionWindow_T + self.Window_height, DetectionWindow_L + self.Window_width)    

        # reset window transparency
        self.root.wm_attributes("-alpha", 1)
        self.root.wait_visibility(self.root)

    def take_screenshot(self):
        # clear canvas
        self.canvas.delete('all')
            
        # get input image
        self.img = get_screenshot(self.DetectionWindow_TL[1], self.DetectionWindow_TL[0], self.DetectionWindow_BR[1], self.DetectionWindow_BR[0])
        
        # BGR -> RGB
        #self.img = self.img[:, :, [2, 1, 0]]
        
        # process and put into canvas 
        image_width = self.DetectionWindow_BR[1] - self.DetectionWindow_TL[1]
        image_height = self.DetectionWindow_BR[0] - self.DetectionWindow_TL[0]
        
        # put screenshot on canvas for review
        self.Canvas_Photo = ImageTk.PhotoImage(image = Image.fromarray(self.img))
        self.canvas.create_image((image_width+30)/2,(image_height+30)/2, image = self.Canvas_Photo,anchor=tk.CENTER)
    
    def save_screenshot(self):
        # save the screenshot image
        save_img = self.img[:, :, [2, 1, 0]] # BGR -> RGB
        img_name = 'test.jpg'
        save_path = os.path.join('RealTimeODinOSRS/dataset/raw', img_name) 
        status = cv2.imwrite(save_path, save_img )
        print(status)
        
    
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
    
    # GUI tool for selecting screen region of intrest, taking and saving screenshots
    Selected_Region = None # global var
    rootWindow = tk.Tk()
    GUI_Tool = DetectionWindowTool(rootWindow)
    rootWindow.title('Real-time Object Detection in OSRS')
    rootWindow.mainloop() # GUI is destroyed if 'Run Detection!' is clicked
    
    # run Detection 
    loop_time = time.time()
    
    #
    # prepare model here
    #
    
    while(True):
        # get input image from selection screen region
        input_img = get_screenshot(Selected_Region[0], Selected_Region[1], Selected_Region[2], Selected_Region[3])
        # BGR -> RGB 
        input_img = input_img[:, :, [2, 1, 0]]
        
        cv2.imshow('Game overlay', input_img)
        
        #
        # run inference here
        # 
        
        # determine and display framerate
        FPS = 1 / (time.time() - loop_time)
        #cv2.putText(f"{FPS:.2f}", (5,5),cv2.FONT_HERSHEY_SIMPLEX,1, (255, 0, 0) , 2, cv2.LINE_AA)
        loop_time = time.time()
        
        # press 'q' to exit
        if cv2.waitKey(1) == ord('q'):
            cv2.destroyAllWindows()
            break
        
    print('Done.')
    

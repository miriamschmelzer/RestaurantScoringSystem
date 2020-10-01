# -*- coding: utf-8 -*-
"""
Created on Wed Nov  2 15:44:40 2016

@author: agedemo
"""

from UnitServer import UnitServer
##from GrabberThread import GrabberThread
import GrabUnit
from DetectionThread import DetectionThread
from RecognitionThread import RecognitionThread

import threading
import time
import cv2
import os
import numpy as np
import requests
import json
from PIL import Image, ImageDraw

class ControllerThread(threading.Thread):
    """ Responsible for starting and shutting down all threads and
        services. """

    def __init__(self, params):

        threading.Thread.__init__(self)

        self.image = '/home/nvidia/pizza/images/JPEG_20181208_050854_-1776544470.jpg'  #'/home/miriam/workspace/AI-server-food-scoring/test.jpg' 
        self.filepath = '/home/nvidia/pizza/images/' #'/home/miriam/workspace/AI-server-food-scoring/images/'

        self.emotion = 0
        self.newImage = False
        self.emotionRecognized = False
        self.imageName = ""

        self.imageSaveTime = time.time()
        os.makedirs('saves', exist_ok=True)

        self.terminated = False
        ##self.caption = params.get("window", "caption")        

        self.minDetections = int(params.get("recognition", "mindetections"))

        ##self.displaysize = params.get("window", "displaysize")
        ##self.displaysize = self.displaysize.upper().split("X")
        ##self.displaysize = tuple([int(s) for s in self.displaysize])

        # Get current resolution
        ##self.resolution = subprocess.Popen('xrandr | grep "\*" | cut -d" " -f4',
        ##                                   shell=True, stdout=subprocess.PIPE).communicate()[0].decode("utf-8").rstrip().split('x')
        ##self.resolution = [int(s) for s in self.resolution]
        ##print(self.resolution)

        # Start frame storage
        # queueLength = params.getint("server", "num_frames")
        queueLength = 8
        self.unitServer = UnitServer(queueLength)

        # Start Grabber thread
        ##self.grabberThread = GrabberThread(self, params)
        ##self.grabberThread.start()
        self.flipHor = params.getint("camera", "flip_horizontal")

        # Start Detection thread
        self.faces = []
        self.detectionThread = DetectionThread(self, params)
        self.detectionThread.start()

        # Start Recognition Thread
        self.recognitionThread = RecognitionThread(self, params)
        self.recognitionThread.start()

        ##unused_width = self.resolution[0] - self.displaysize[0]
        ##cv2.moveWindow(self.caption, unused_width//2, 0)  # Will move window when everything is running. Better way TODO

    def run(self):
        continue_reading = True
        oldImageName = "name"
        url = "https://aiotlab.111mb.de/AndroidUploadImage/downloadImage.php"
        while not self.terminated:
            while continue_reading:
                response = requests.request("GET", url)
                # parse response:
                y = json.loads(response.text)
                image_url =  y[0]["url"]
                self.imageName =  y[0]["name"]
                filename= self.filepath+self.imageName
                if self.imageName != oldImageName :
                    if oldImageName != "name":

                        self.image = filename

                        r = requests.get(image_url, allow_redirects=True)
                        open(filename, 'wb').write(r.content)
                        print(filename)
                        print("new image saved")

                        self.alignImage(self.image)
                        self.newImage = True

                        oldImageName = self.imageName
                        
                        self.loadImage()

                    else:
                        oldImageName = self.imageName
                else:
                    print('No new image')
                    
                print(self.emotionRecognized)

                score = 0
                if self.emotionRecognized == True: 
                    print('Emotion recognized')
                    validFaces = [f for f in self.faces if len(f['bboxes']) > self.minDetections]
                    ##for face in validFaces:
                    face = validFaces[0]
                    
                    if "expression" in list(face.keys()):
                        expression = face["expression"]

                    if expression == "Neutral":
                        score = 2
                    elif expression == "Happy":
                        score = 1
                    elif expression == "Sad":
                        score = 3
                    elif expression == "Surprise":
                        score = 1
                    elif expression == "Fear":
                        score = 3
                    elif expression == "Disgust":
                        score = 3
                    elif expression == "Anger":
                        score = 3

                    up_url = "https://aiotlab.111mb.de/AndroidUploadImage/upload.php"
                    up_payload = {'name':self.imageName , 'ai_rate':score}
                    response = requests.request("POST", up_url, data=up_payload)
                    self.newImage = False
                    self.emotionRecognized = False
                    print('score')
                        
        #while not self.terminated:
            #time.sleep(0.5)  

    def loadImage(self):
        frame = cv2.imread(self.image)
        if frame is not None and not self.isTerminated():
                if self.flipHor:
                    frame = frame[:, ::-1, ...] 

                unit = GrabUnit.GrabUnit(frame)
                self.putUnit(unit)     

    ##def getImage(self):
        ##return self.image

    def alignImage(self, image):        # Bild richtig drehen und schneiden
        image = Image.open(image)

        if hasattr(image, '_getexif'):
            orientation = 0x0112
            exif = image._getexif()
            if exif is not None:
                orientation = exif[orientation]
                rotations = {
                    3: Image.ROTATE_180,
                    6: Image.ROTATE_270,
                    8: Image.ROTATE_90
                }
                if orientation in rotations:
                    image = image.transpose(rotations[orientation])

        w, h = image.size
        alignedImage = image.crop((-467,0,w+467,h))

        draw = ImageDraw.Draw(alignedImage)
        draw.rectangle((0,0,467,h),fill="white")
        draw.rectangle((w+467,0,w+(2*467),h), fill="white")
        del draw

        alignedImage.save(self.image)

    def putUnit(self, unit):

        # Show the newest frame immediately.
        ##self.showVideo(unit)

        # Send to further processing
        if not self.terminated:
            self.unitServer.putUnit(unit)

    def getUnit(self, caller, timestamp = None):

        return self.unitServer.getUnit(caller, timestamp)

    def terminate(self):

        self.terminated = True

##    def drawBoundingBox(self, img, bbox):
##
##        x,y,w,h = [int(c) for c in bbox]
##
##        m = 0.2
##
##        # Upper left corner
##        pt1 = (x,y)
##        pt2 = (int(x + m*w), y)
##        cv2.line(img, pt1, pt2, color = [255,255,0], thickness = 2)
##
##        pt1 = (x,y)
##        pt2 = (x, int(y + m*h))
##        cv2.line(img, pt1, pt2, color = [255,255,0], thickness = 2)
##
##        # Upper right corner
##        pt1 = (x + w, y)
##        pt2 = (x + w, int(y + m*h))
##        cv2.line(img, pt1, pt2, color = [255,255,0], thickness = 2)
##
##        pt1 = (x + w, y)
##        pt2 = (int(x + w - m * w), y)
##        cv2.line(img, pt1, pt2, color = [255,255,0], thickness = 2)
##
##        # Lower left corner
##        pt1 = (x, y + h)
##        pt2 = (x, int(y + h - m*h))
##        cv2.line(img, pt1, pt2, color = [255,255,0], thickness = 2)
##
##        pt1 = (x, y + h)
##        pt2 = (int(x + m * w), y + h)
##        cv2.line(img, pt1, pt2, color = [255,255,0], thickness = 2)
##
##        # Lower right corner
##        pt1 = (x + w, y + h)
##        pt2 = (x + w, int(y + h - m*h))
##        cv2.line(img, pt1, pt2, color = [255,255,0], thickness = 2)
##
##        pt1 = (x + w, y + h)
##        pt2 = (int(x + w - m * w), y + h)
##        cv2.line(img, pt1, pt2, color = [255,255,0], thickness = 2)

##    def drawFace(self, face, img):
##
##        bbox = np.mean(face['bboxes'], axis = 0)
##
##        self.drawBoundingBox(img, bbox)
##        x,y,w,h = [int(c) for c in bbox]
##
##        # 1. AGE
##
##        if "age" in list(face.keys()):
##
##            age = face['age']
##            annotation = "Age: %.0f" % (age)
##            txtLoc = (x, y + h + 30)
##
##            cv2.putText(img,
##                        text = annotation,
##                        org = txtLoc,
##                        fontFace = cv2.FONT_HERSHEY_SIMPLEX,
##                        fontScale = 1,
##                        color = [255,255,0],
##                        thickness = 2)
##
##        # 2. GENDER
##
##        if "gender" in list(face.keys()):
##
##            gender = "MALE" if face['gender'] > 0.5 else "FEMALE"
##            genderProb = max(face["gender"], 1-face["gender"])
##            annotation = "%s %.0f %%" % (gender, 100.0 * genderProb)
##            txtLoc = (x, y + h + 60)
##
##            cv2.putText(img,
##                        text = annotation,
##                        org = txtLoc,
##                        fontFace = cv2.FONT_HERSHEY_SIMPLEX,
##                        fontScale = 1,
##                        color = [255,255,0],
##                        thickness = 2)
##
##        # 3. EXPRESSION
##
##        if "expression" in list(face.keys()):
##
##            expression = face["expression"]
##            annotation = "%s" % (expression)
##            txtLoc = (x, y + h + 90)
##
##            cv2.putText(img,
##                        text = annotation,
##                        org = txtLoc,
##                        fontFace = cv2.FONT_HERSHEY_SIMPLEX,
##                        fontScale = 1,
##                        color = [255,255,0],
##                        thickness = 2)
##            score = 0
##            if self.emotionRecognized: 
##
##                if expression == "Neutral":
##                    score = 2
##                elif expression == "Happy":
##                    score = 1
##                elif expression == "Sad":
##                    score = 3
##                elif expression == "Surprise":
##                    score = 1
##                elif expression == "Fear":
##                    score = 3
##                elif expression == "Disgust":
##                    score = 3
##                elif expression == "Anger":
##                    score = 3
##
##                up_url = "https://aiotlab.111mb.de/AndroidUploadImage/upload.php"
##                up_payload = {'name':self.imageName , 'ai_rate': score}
##                response = requests.request("POST", up_url, data=up_payload)
##                self.newImage = False
##                self.emotionRecognized = False

    def getNewImage(self):
        return self.newImage

    def setEmotionRecognized(self, recognized):
        self.emotionRecognized = recognized

##    def showVideo(self, unit):
##
##        unit.acquire()
##        frame = copy.deepcopy(unit.getFrame())
##        unit.release()
##
##        # Annotate
##
##        validFaces = [f for f in self.faces if len(f['bboxes']) > self.minDetections]
##
##        # save image
##        #if validFaces and 'expression' in validFaces[-1] and time.time() - self.imageSaveTime > 5:
##        #    self.imageSaveTime = time.time()
##
##        #    filename = datetime.datetime.now().strftime('%Y-%m-%d-%H-%m-%S')
##
##        #    try:
##        #        for face in validFaces:
##        #            filename += "-age{:.0f}_{:.2f}_{}".format(face['age'], face['gender'][0], face['expression'])
##        #        cv2.imwrite(os.path.join('saves', filename + ".jpg"), frame)
##        #    except:
##        #        pass
##
##        for face in validFaces:
##            self.drawFace(face, frame)
##
##        frame = cv2.resize(frame, self.displaysize)
##        cv2.imshow(self.caption, frame)
##        key = cv2.waitKey(10)
##
##        if key == 27:
##            self.terminate()

    def findNearestFace(self, bbox):

        distances = []

        x,y,w,h = bbox
        bboxCenter = [x + w/2, y + h/2]

        for face in self.faces:

            x,y,w,h = np.mean(face['bboxes'], axis = 0)
            faceCenter = [x + w/2, y + h/2]

            distance = np.hypot(faceCenter[0] - bboxCenter[0], 
                                faceCenter[1] - bboxCenter[1])

            distances.append(distance)

        if len(distances) == 0:
            minIdx = None
            minDistance = None
        else:            
            minDistance = np.min(distances)
            minIdx = np.argmin(distances)

        return minIdx, minDistance        

    def setDetections(self, detections, timestamps):

        # Find the location among all recent face locations where this would belong

        for bbox, timestamp in zip(detections, timestamps):

            idx, dist = self.findNearestFace(bbox)

            if dist is not None and dist < 100:

                self.faces[idx]['bboxes'].append(bbox)
                self.faces[idx]['timestamps'].append(timestamp)

                if len(self.faces[idx]['bboxes']) > 7:
                    self.faces[idx]['bboxes'].pop(0)
                    self.faces[idx]['timestamps'].pop(0)

            else:
                # This is a new face not in the scene before
                self.faces.append({'timestamps': [timestamp], 'bboxes': [bbox]})

        # Clean old detections:

        now = time.time()
        facesToRemove = []

        for i, face in enumerate(self.faces):
            if now - face['timestamps'][-1] > 0.5:
                facesToRemove.append(i)                

        for i in facesToRemove:
            try:
                self.faces.pop(i)
            except:
                # Face was deleted by other thread. 
                pass

    def getFaces(self):

        if len(self.faces) == 0:
            return None
        else:
            return self.faces

    def isTerminated(self):

        return self.terminated


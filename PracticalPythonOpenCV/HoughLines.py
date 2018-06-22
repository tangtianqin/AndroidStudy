#from __future__ import print_funtion
import argparse
import cv2
import numpy as np

ap = argparse.ArgumentParser()
ap.add_argument("-i", "--image", required=True, help="Path to the image")
args = vars(ap.parse_args())
image = cv2.imread(args["image"])
print("({},{})".format(image.shape[0], image.shape[1]))
resized = cv2.resize(image, None, fx=0.25, fy=0.25)
#cv2.imshow("Resized", resized)
gray = cv2.cvtColor(resized, cv2.COLOR_BGR2GRAY)
edges = cv2.Canny(gray, 50, 150, apertureSize=3)
cv2.imshow("Edges", edges)
minLineLength = 100
maxLineGap = 10
lines = cv2.HoughLinesP(edges, 100, np.pi/180, 100)
for x1,y1,x2,y2 in lines[0]:
    cv2.line(resized, (x1, y1),(x2,y2),(0,255,0),2)

cv2.imshow("Image", resized)
cv2.waitKey(0)

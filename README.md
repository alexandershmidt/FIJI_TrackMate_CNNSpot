# FIJI_TrackMate_CNNSpot
A neural network based algorithm for spot detection
tested on Windows 10, jdk 8 with pycharm IDE

all dependencies are listed in pom.xml

Known bugs:
1. Tensorflow class not found in FIJI.
2. IOU Threshold needs to be reimplemented

how to run.
1. open pycharm
2. run src/test/java/trackmate/main.java
3. Warning will appear, Press ok
3. File/Open: select image (16 bit grayscale)
4. Plugins/Tracking/TrackMate
5. Next
6. select a detector: CNNSpot detector
7. provide path to pretrained model (path ends with \model)
8. preview or next

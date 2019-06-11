package org.taylor_lab.ij.trackmate.cnnspot;

import fiji.plugin.trackmate.detection.DetectorKeys;

public class CNNSpotDetectorKeys extends DetectorKeys {
    public static final double DEFAULT_NN_RADIUS = 0.45D;
    public static final String KEY_SOFTMAX_THRESHOLD = "SOFTMAX_THRESHOLD";
    public static final double DEFAULT_SOFTMAX_THRESHOLD = 0.5D;
    public static final String KEY_IOU_THRESHOLD = "IOU_THRESHOLD";
    public static final double DEFAULT_IOU_THRESHOLD = 3D;
    public static final String KEY_PATH_TO_PB = "PATH_TO_PB";
    public static final String DEFAULT_PATH_TO_PB = "";
    public CNNSpotDetectorKeys() {
    }
}


package org.taylor_lab.ij.trackmate.cnnspot;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.detection.SpotDetector;
import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import org.tensorflow.*;

import java.util.ArrayList;
import java.util.List;

public class CNNSpotDetector< T extends RealType< T > & NativeType< T >> implements SpotDetector< T >, MultiThreaded
{
	public final static String BASE_ERROR_MESSAGE = "CNNSpotDetector: ";

	protected RandomAccessible< T > img;

	protected double radius;

	protected double softmax_threshold;

	protected double iou_threshold;

	protected boolean doSubPixelLocalization;

	protected String baseErrorMessage;

	protected String errorMessage;

	protected List< Spot > spots = new ArrayList<>();

	protected long processingTime;

	protected int numThreads;

	protected final Interval interval;

	protected final double[] calibration;
	RandomAccess<T> sourceRa;

	final String path_to_pb;

	public CNNSpotDetector(final RandomAccessible< T > img, final Interval interval, final double[] calibration, final double radius, final double softmax_threshold, final double iou_threshold, final String path_to_pb, final boolean doSubPixelLocalization){

		this.img = img;
		this.interval = DetectionUtils.squeeze( interval );
		this.calibration = calibration;
		this.radius = radius;
		this.softmax_threshold = softmax_threshold;
		this.iou_threshold = iou_threshold;
		this.doSubPixelLocalization = doSubPixelLocalization;
		this.path_to_pb =path_to_pb;
		this.baseErrorMessage = BASE_ERROR_MESSAGE;
		setNumThreads();
	}

	@Override
	public boolean process(){
		int kernel_size = 7;
		final ImgFactory<FloatType> factory = Util.getArrayOrCellImgFactory(
				interval, new FloatType());
		final Img<FloatType> source = DetectionUtils.copyToFloatImg(img,
				interval, factory);
		sourceRa = img.randomAccess();

		int width = (int) source.dimension(0);
		int height = (int) source.dimension(1);
		if (source.numDimensions() > 1) {
			int depth = (int) source.dimension(2);
		} else {
			int depth = 0;
		}

		int [] positions_x = new int [(width- kernel_size)*(height- kernel_size)];
		int [] positions_y = new int [(width- kernel_size)*(height- kernel_size)];
        boolean [] checked_status = new boolean [(width- kernel_size)*(height- kernel_size)];
		boolean new_spot_status;
		float[][] list = new float [49][(width- kernel_size)*(height- kernel_size)];

		for(int i = 0; i <width - kernel_size; i++) {
			for (int j = 0; j <height - kernel_size; j++) {
				positions_x[i*(width-kernel_size) + j] = j+kernel_size/2;
				positions_y[i*(width-kernel_size) + j] = i+kernel_size/2;
				for(int kernel_y = 0; kernel_y < kernel_size; kernel_y++) {
					for (int kernel_x = 0; kernel_x < kernel_size; kernel_x++) {
						sourceRa.setPosition(new Point((long) kernel_x+j, (long) kernel_y+i, (long) 0));
						list[7 * kernel_y + kernel_x][i*(width-kernel_size) + j] = (float) sourceRa.get().getRealDouble();
						}
					}
				}
			}
        Tensor t = Tensor.create(list);

		SavedModelBundle savedModelBundle = SavedModelBundle.load(path_to_pb, "serve");
		Tensor result = savedModelBundle.session().runner()
				.feed("myInput:0", t )
				.fetch("Add_2:0")
				.run().get(0);
		float[][] resultValues = (float[][]) result.copyTo(new float[2][(width- kernel_size)*(height- kernel_size)]);
		double [][] doubleresultValues = new double [2][(width- kernel_size)*(height- kernel_size)];
		for(int i = 0; i <(width- kernel_size)*(height- kernel_size); i++) {
			doubleresultValues[0][i] = softmax(resultValues[0][i], resultValues[1][i]);
			doubleresultValues[1][i] = softmax(resultValues[1][i], resultValues[0][i]);
		}
		for(int i = 0; i <(width- kernel_size)*(height- kernel_size); i++) {
			doubleresultValues[0][i] = doubleresultValues[0][i] -  softmax_threshold;
		}
		for(int i = 0; i <(width- kernel_size)*(height- kernel_size); i++) {
			if(resultValues[0][i]>resultValues[1][i]){
				new_spot_status = true;
				for(int j = 0; j <(width- kernel_size)*(height- kernel_size); j++) {
					if (resultValues[0][i] >= resultValues[0][j]) {
						if ((Math.pow((positions_x[i] - positions_x[j]), 2) + Math.pow((positions_y[i] - positions_y[j]), 2)) < Math.pow(iou_threshold, 2)) {
						    if (!checked_status[j]){
                                checked_status[j] = true;
                                new_spot_status = true;
                            }
						    else{
                                new_spot_status = false;
                                break;
                            }
						}
					}
				}
				if (new_spot_status){
					final double x = positions_x[i] * calibration[0];
					final double y = positions_y[i] * calibration[1];
					final double z = 0 * calibration[2];
					final double quality = 5;
					final Spot spot = new Spot(x, y, z, radius, quality);
					spots.add(spot);
				}

			}
		}
		return true;
	}

	public double softmax(double x1, double x2){
		double softmax_value = (Math.exp(x1)/ (Math.exp(x1)+ Math.exp(x2)));
		return softmax_value;
	}

	@Override
	public boolean checkInput()
	{
		if ( null == img )
		{
			errorMessage = baseErrorMessage + "Image is null.";
			return false;
		}
		if ( img.numDimensions() > 3 )
		{
			errorMessage = baseErrorMessage + "Image must be 1D, 2D or 3D, got " + img.numDimensions() + "D.";
			return false;
		}
		return true;
	}

	@Override
	public List< Spot > getResult()
	{
		return spots;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

	@Override
	public void setNumThreads() { this.numThreads = Runtime.getRuntime().availableProcessors(); }

	@Override
	public void setNumThreads( final int numThreads ) {this.numThreads = numThreads;}

	@Override
	public int getNumThreads()
	{
		return numThreads;
	}
}
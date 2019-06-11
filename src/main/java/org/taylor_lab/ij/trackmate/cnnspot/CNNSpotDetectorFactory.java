package org.taylor_lab.ij.trackmate.cnnspot;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.detection.SpotDetector;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.gui.ConfigurationPanel;
import fiji.plugin.trackmate.util.TMUtils;
import net.imagej.ImgPlus;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import org.jdom2.Element;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fiji.plugin.trackmate.detection.DetectorKeys.*;
import static fiji.plugin.trackmate.io.IOUtils.*;
import static fiji.plugin.trackmate.util.TMUtils.checkMapKeys;
import static fiji.plugin.trackmate.util.TMUtils.checkParameter;
import static org.taylor_lab.ij.trackmate.cnnspot.CNNSpotDetectorKeys.*;

@Plugin( type = SpotDetectorFactory.class )

public class CNNSpotDetectorFactory< T extends RealType< T > & NativeType< T >> implements SpotDetectorFactory< T >
{
	public static final String DETECTOR_KEY = "CNNSPOT_DETECTOR";

	public static final String NAME = "CNNSpot detector";

	public static final String INFO_TEXT = "<html>" + "<br/> This is a neural network based detector.<br/>" +
			"Provide path to the pretrained model(.pb) to enable detection routine.<br/>" + "</html>";

	protected String errorMessage;

	protected ImgPlus< T > imp;

	protected Map< String, Object > settings;

	public ImageIcon getIcon()
	{
		return null;
	}

	public String getInfoText()
	{
		return INFO_TEXT;
	}

	public String getKey()
	{
		return DETECTOR_KEY;
	}

	public String getName() { return NAME; }

	public String getErrorMessage() { return errorMessage; }

	public boolean setTarget(final ImgPlus< T > imp, final Map< String, Object > settings )
	{
		this.imp = imp;
		this.settings = settings;
		return checkSettings( settings );
	}

	public boolean marshall(final Map< String, Object > settings, final Element element)
	{
		final StringBuilder errorHolder = new StringBuilder();
		final boolean ok =
				writeRadius( settings, element, errorHolder )
						&& writeTargetChannel( settings, element, errorHolder );
		if ( !ok )
		{
			errorMessage = errorHolder.toString();
		}
		return ok;
	}

	public boolean unmarshall(final Element element, final Map< String, Object > settings)
	{
		settings.clear();
		final StringBuilder errorHolder = new StringBuilder();
		boolean ok = true;
		ok = ok & readDoubleAttribute( element, settings, KEY_RADIUS, errorHolder );
		ok = ok & readIntegerAttribute( element, settings, KEY_TARGET_CHANNEL, errorHolder );
		if ( !ok )
		{
			errorMessage = errorHolder.toString();
			return false;
		}
		return checkSettings( settings );
	}

	public SpotDetector< T > getDetector(final Interval interval, final int frame )
	{
		final double radius = ( Double ) settings.get( KEY_RADIUS );
		final double softmax_threshold = ( Double ) settings.get( KEY_SOFTMAX_THRESHOLD );
		final double iou_threshold = ( Double ) settings.get( KEY_IOU_THRESHOLD );
		final boolean doSubpixel = ( Boolean ) settings.get( KEY_DO_SUBPIXEL_LOCALIZATION );
		final String path_to_pb = (String) settings.get( KEY_PATH_TO_PB );
		RandomAccessible< T > imFrame;
		final double[] calibration = TMUtils.getSpatialCalibration( imp );
		final int cDim = TMUtils.findCAxisIndex( imp );
		if ( cDim < 0 )
		{
			imFrame = imp;
		}
		else
		{
			// In ImgLib2, dimensions are 0-based.
			final int channel = ( Integer ) settings.get( KEY_TARGET_CHANNEL ) - 1;
			imFrame = Views.hyperSlice( imp, cDim, channel );
		}

		int timeDim = TMUtils.findTAxisIndex( imp );
		if ( timeDim >= 0 )
		{
			if ( cDim >= 0 && timeDim > cDim )
			{
				timeDim--;
			}
			imFrame = Views.hyperSlice( imFrame, timeDim, frame );
		}

		// In case we have a 1D image.
		if ( imp.dimension( 0 ) < 2 )
		{ // Single column image, will be rotated internally.
			calibration[ 0 ] = calibration[ 1 ]; // It gets NaN otherwise
			calibration[ 1 ] = 1;
			imFrame = Views.hyperSlice( imFrame, 0, 0 );
		}
		if ( imp.dimension( 1 ) < 2 )
		{ // Single line image
			imFrame = Views.hyperSlice( imFrame, 1, 0 );
		}
		final CNNSpotDetector< T > detector = new CNNSpotDetector<>( imFrame, interval, calibration, radius, softmax_threshold, iou_threshold, path_to_pb,doSubpixel);
		detector.setNumThreads( 1 );
		return detector;
	}

	public ConfigurationPanel getDetectorConfigurationPanel(final Settings settings, final Model model )
	{
		return new CNNSpotConfigurationPanel(settings, model, INFO_TEXT, NAME);
	}

	public boolean checkSettings(Map<String, Object> settings)
	{
		boolean ok = true;
		final StringBuilder errorHolder = new StringBuilder();
		ok = ok & checkParameter( settings, KEY_TARGET_CHANNEL, Integer.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_RADIUS, Double.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_SOFTMAX_THRESHOLD, Double.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_IOU_THRESHOLD, Double.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_DO_SUBPIXEL_LOCALIZATION, Boolean.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_PATH_TO_PB, String.class, errorHolder );
		final List< String > mandatoryKeys = new ArrayList<>();
		mandatoryKeys.add( KEY_TARGET_CHANNEL );
		mandatoryKeys.add( KEY_RADIUS );
		mandatoryKeys.add( KEY_SOFTMAX_THRESHOLD );
		mandatoryKeys.add( KEY_IOU_THRESHOLD );
		mandatoryKeys.add( KEY_DO_SUBPIXEL_LOCALIZATION );
		mandatoryKeys.add( KEY_PATH_TO_PB );
		ok = ok & checkMapKeys( settings, mandatoryKeys, null, errorHolder );
		if ( !ok )
		{
			errorMessage = errorHolder.toString();
		}
		return ok;
	}

	public Map< String, Object > getDefaultSettings()
	{
		final Map< String, Object > settings = new HashMap<>();
		settings.put( KEY_TARGET_CHANNEL, DEFAULT_TARGET_CHANNEL );
		settings.put( KEY_RADIUS, DEFAULT_NN_RADIUS );
		settings.put( KEY_SOFTMAX_THRESHOLD, DEFAULT_SOFTMAX_THRESHOLD );
		settings.put( KEY_IOU_THRESHOLD, DEFAULT_SOFTMAX_THRESHOLD );
		settings.put( KEY_DO_MEDIAN_FILTERING, DEFAULT_DO_MEDIAN_FILTERING );
		settings.put( KEY_DO_SUBPIXEL_LOCALIZATION, DEFAULT_DO_SUBPIXEL_LOCALIZATION );
		settings.put( KEY_PATH_TO_PB, DEFAULT_PATH_TO_PB );
		return settings;
	}

}
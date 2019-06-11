package org.taylor_lab.ij.trackmate.cnnspot;

import fiji.plugin.trackmate.*;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.gui.ConfigurationPanel;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.panels.components.JNumericTextField;
import fiji.plugin.trackmate.util.JLabelLogger;
import fiji.util.NumberParser;
import ij.ImagePlus;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static fiji.plugin.trackmate.detection.DetectorKeys.*;
import static fiji.plugin.trackmate.gui.TrackMateWizard.*;
import static org.taylor_lab.ij.trackmate.cnnspot.CNNSpotDetectorKeys.*;

public class CNNSpotConfigurationPanel extends ConfigurationPanel
{

	private static final String TOOLTIP_REFRESH = "<html>" + "Preview the current settings on the current frame." + "<p>" + "Advice: change the settings until you get at least <br>" + "<b>all</b> the spots you want, and do not mind the <br>" + "spurious spots too much. You will get a chance to <br>" + "get rid of them later." + "</html>";

	private static final ImageIcon ICON_REFRESH = new ImageIcon( TrackMateGUIController.class.getResource( "images/arrow_refresh_small.png" ) );

	//Constructor
	protected final Settings settings;

	protected final ImagePlus imp;

	protected final String infoText;

	protected final String detectorName;

	protected final Model model;

	protected final String spaceUnits;

	//GUI
	protected SpringLayout layout;

	private JLabel jLabel1;

	protected JLabel jLabelSegmenterName;

	private JLabel jLabel2;

	protected JButton jButtonRefresh;

	protected JLabel lblSegmentInChannel;

	protected JSlider sliderChannel;

	protected JLabel labelChannel;

	protected JTextField jTextFieldBlobDiameter;

    protected JLabel jLabelBlobDiameterUnit;

    protected JCheckBox jCheckBoxMedianFilter;

    protected JLabel jLabelHelpText;

    protected JTextField jTextFieldSoftmaxThreshold;

	protected JTextField jTextFieldIOUThreshold;

    protected JLabel jLabelThreshold;

	protected JCheckBox jCheckSubPixel;

	protected JButton btnPreview;

	private JLabel jLabel3;

	private static final ImageIcon ICON_PREVIEW = new ImageIcon( TrackMateGUIController.class.getResource( "images/flag_checked.png" ) );

	protected JButton btnPretrainedModel;

	public String path_to_pb;

	//preview
	private Logger localLogger;

	public CNNSpotConfigurationPanel(final Settings settings, final Model model, final String infoText, final String detectorName )
	{
		this.settings = settings;
		this.imp = settings.imp;
		this.infoText = infoText;
		this.detectorName = detectorName;
		this.model = model;
		this.spaceUnits = model.getSpaceUnits();
		initGUI();
	}

    protected SpotDetectorFactory< ? > getDetectorFactory()
    {
        return new CNNSpotDetectorFactory<>();
    }

    protected void preview()
    {
        btnPreview.setEnabled( false );
        new Thread( "TrackMate preview detection thread" )
        {
            @Override
            public void run()
            {
                final Settings lSettings = new Settings();
                lSettings.setFrom( imp );
                final int frame = imp.getFrame() - 1;
                lSettings.tstart = frame;
                lSettings.tend = frame;

                lSettings.detectorFactory = getDetectorFactory();
                lSettings.detectorSettings = getSettings();

                final TrackMate trackmate = new TrackMate( lSettings );
                trackmate.getModel().setLogger( localLogger );

                final boolean detectionOk = trackmate.execDetection();
                if ( !detectionOk )
                {
                    localLogger.error( trackmate.getErrorMessage() );
                    return;
                }
                localLogger.log( "Found " + trackmate.getModel().getSpots().getNSpots( false ) + " spots." );

                // Wrap new spots in a list.
                final SpotCollection newspots = trackmate.getModel().getSpots();
                final Iterator<Spot> it = newspots.iterator( frame, false );
                final ArrayList< Spot > spotsToCopy = new ArrayList< >( newspots.getNSpots( frame, false ) );
                while ( it.hasNext() )
                {
                    spotsToCopy.add( it.next() );
                }
                // Pass new spot list to model.
                model.getSpots().put( frame, spotsToCopy );
                // Make them visible
                for ( final Spot spot : spotsToCopy )
                {
                    spot.putFeature( SpotCollection.VISIBLITY, SpotCollection.ONE );
                }
                // Generate event for listener to reflect changes.
                model.setSpots( model.getSpots(), true );

                btnPreview.setEnabled( true );

            }
        }.start();
    }

	public String load_pretrained_model(){
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new java.io.File("."));
		chooser.setDialogTitle("Choose pretrained model");
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setAcceptAllFileFilterUsed(false);
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			path_to_pb = ""+ chooser.getSelectedFile()+"";
		}
		return path_to_pb;
	}
	private void refresh()
	{
		if ( null == imp ) {
			return;
		}
		double threshold = imp.getProcessor().getMinThreshold();
		if ( threshold < 0 )
		{
			threshold = 0;
		}
		jTextFieldSoftmaxThreshold.setText( String.format( "%.0f", threshold ) );
		sliderChannel.setValue( imp.getC() );
	}
	protected void initGUI()
	{
		this.setPreferredSize( new java.awt.Dimension( 300, 461 ) );
		layout = new SpringLayout();
		setLayout( layout );
		{
			jLabel1 = new JLabel();
			layout.putConstraint( SpringLayout.NORTH, jLabel1, 10, SpringLayout.NORTH, this );
			layout.putConstraint( SpringLayout.WEST, jLabel1, 5, SpringLayout.WEST, this );
			this.add( jLabel1 );
			jLabel1.setText( "Settings for detector:" );
			jLabel1.setFont( FONT );
		}
		{
			jLabelSegmenterName = new JLabel();
			layout.putConstraint( SpringLayout.NORTH, jLabelSegmenterName, 33, SpringLayout.NORTH, this );
			layout.putConstraint( SpringLayout.WEST, jLabelSegmenterName, 11, SpringLayout.WEST, this );
			this.add( jLabelSegmenterName );
			jLabelSegmenterName.setFont( BIG_FONT );
			jLabelSegmenterName.setText( detectorName );
		}
		{
			jLabel2 = new JLabel();
			layout.putConstraint( SpringLayout.NORTH, jLabel2, 247, SpringLayout.NORTH, this );
			layout.putConstraint( SpringLayout.WEST, jLabel2, 16, SpringLayout.WEST, this );
			this.add( jLabel2 );
			jLabel2.setText( "Estimated blob diameter:" );
			jLabel2.setFont( FONT );

		}
		{
			jTextFieldBlobDiameter = new JNumericTextField();
			layout.putConstraint( SpringLayout.NORTH, jTextFieldBlobDiameter, 247, SpringLayout.NORTH, this );
			layout.putConstraint( SpringLayout.WEST, jTextFieldBlobDiameter, 168, SpringLayout.WEST, this );
			layout.putConstraint( SpringLayout.SOUTH, jTextFieldBlobDiameter, 263, SpringLayout.NORTH, this );
			layout.putConstraint( SpringLayout.EAST, jTextFieldBlobDiameter, 208, SpringLayout.WEST, this );
			jTextFieldBlobDiameter.setColumns( 5 );
			jTextFieldBlobDiameter.setText( Double.toString(DEFAULT_NN_RADIUS) );
			this.add( jTextFieldBlobDiameter );
			jTextFieldBlobDiameter.setFont( FONT );
		}
        {
            jLabelBlobDiameterUnit = new JLabel();
			layout.putConstraint( SpringLayout.NORTH, jLabelBlobDiameterUnit, 245, SpringLayout.NORTH, this );
			layout.putConstraint( SpringLayout.WEST, jLabelBlobDiameterUnit, 228, SpringLayout.WEST, this );
			layout.putConstraint( SpringLayout.SOUTH, jLabelBlobDiameterUnit, 262, SpringLayout.NORTH, this );
            layout.putConstraint( SpringLayout.EAST, jLabelBlobDiameterUnit, 268, SpringLayout.EAST, this );
            this.add( jLabelBlobDiameterUnit );
            jLabelBlobDiameterUnit.setFont( FONT );
            jLabelBlobDiameterUnit.setText( spaceUnits );
        }
        {
            jLabelHelpText = new JLabel();
            layout.putConstraint( SpringLayout.NORTH, jLabelHelpText, 60, SpringLayout.NORTH, this );
            layout.putConstraint( SpringLayout.WEST, jLabelHelpText, 10, SpringLayout.WEST, this );
            layout.putConstraint( SpringLayout.SOUTH, jLabelHelpText, 164, SpringLayout.NORTH, this );
            layout.putConstraint( SpringLayout.EAST, jLabelHelpText, -10, SpringLayout.EAST, this );
            this.add( jLabelHelpText );
            jLabelHelpText.setFont( FONT.deriveFont( Font.ITALIC ) );
            jLabelHelpText.setText( infoText.replace( "<br>", "" ).replace( "<p>", "<p align=\"justify\">" ).replace( "<html>", "<html><p align=\"justify\">" ) );
        }
        {
            jLabelThreshold = new JLabel();
            layout.putConstraint( SpringLayout.NORTH, jLabelThreshold, 268, SpringLayout.NORTH, this );
            layout.putConstraint( SpringLayout.WEST, jLabelThreshold, 16, SpringLayout.WEST, this );
            layout.putConstraint( SpringLayout.EAST, jLabelThreshold, -16, SpringLayout.EAST, this );
            this.add( jLabelThreshold );
            jLabelThreshold.setText( "Softmax Threshold:" );
            jLabelThreshold.setFont( FONT );
        }
        {
			jTextFieldSoftmaxThreshold = new JNumericTextField();
            layout.putConstraint( SpringLayout.NORTH, jTextFieldSoftmaxThreshold, 268, SpringLayout.NORTH, this );
            layout.putConstraint( SpringLayout.WEST, jTextFieldSoftmaxThreshold, 168, SpringLayout.WEST, this );
            layout.putConstraint( SpringLayout.SOUTH, jTextFieldSoftmaxThreshold, 284, SpringLayout.NORTH, this );
            layout.putConstraint( SpringLayout.EAST, jTextFieldSoftmaxThreshold, 208, SpringLayout.WEST, this );
			jTextFieldSoftmaxThreshold.setText( Double.toString(DEFAULT_SOFTMAX_THRESHOLD) );
            this.add( jTextFieldSoftmaxThreshold );
			jTextFieldSoftmaxThreshold.setFont( FONT );
        }
		{
			jCheckSubPixel = new JCheckBox();
			layout.putConstraint(SpringLayout.NORTH, jCheckSubPixel, 336, SpringLayout.NORTH, this);
			layout.putConstraint(SpringLayout.WEST, jCheckSubPixel, 11, SpringLayout.WEST, this);
			layout.putConstraint(SpringLayout.SOUTH, jCheckSubPixel, 357, SpringLayout.NORTH, this);
			layout.putConstraint(SpringLayout.EAST, jCheckSubPixel, 242, SpringLayout.WEST, this);
			this.add(jCheckSubPixel);
			jCheckSubPixel.setText("Do sub-pixel localization ");
			jCheckSubPixel.setFont(FONT);
		}
		{
			lblSegmentInChannel = new JLabel( "Segment in channel:" );
			layout.putConstraint( SpringLayout.NORTH, lblSegmentInChannel, 219, SpringLayout.NORTH, this );
			layout.putConstraint( SpringLayout.WEST, lblSegmentInChannel, 16, SpringLayout.WEST, this );
			layout.putConstraint( SpringLayout.EAST, lblSegmentInChannel, 116, SpringLayout.WEST, this );
			lblSegmentInChannel.setFont( SMALL_FONT );
			add( lblSegmentInChannel );

			sliderChannel = new JSlider();
			layout.putConstraint( SpringLayout.NORTH, sliderChannel, 213, SpringLayout.NORTH, this );
			layout.putConstraint( SpringLayout.WEST, sliderChannel, 126, SpringLayout.WEST, this );
			layout.putConstraint( SpringLayout.SOUTH, sliderChannel, 236, SpringLayout.NORTH, this );
			layout.putConstraint( SpringLayout.EAST, sliderChannel, 217, SpringLayout.WEST, this );
			sliderChannel.addChangeListener( new ChangeListener()
			{
				@Override
				public void stateChanged( final ChangeEvent e )
				{
					labelChannel.setText( "" + sliderChannel.getValue() );
				}
			} );
			add( sliderChannel );

			labelChannel = new JLabel( "1" );
			layout.putConstraint( SpringLayout.NORTH, labelChannel, 216, SpringLayout.NORTH, this );
			layout.putConstraint( SpringLayout.WEST, labelChannel, 228, SpringLayout.WEST, this );
			layout.putConstraint( SpringLayout.SOUTH, labelChannel, 234, SpringLayout.NORTH, this );
			layout.putConstraint( SpringLayout.EAST, labelChannel, 249, SpringLayout.WEST, this );
			labelChannel.setHorizontalAlignment( SwingConstants.CENTER );
			labelChannel.setFont( SMALL_FONT );
			add( labelChannel );
		}
		{
			jButtonRefresh = new JButton( "Refresh treshold", ICON_REFRESH );
			layout.putConstraint( SpringLayout.NORTH, jButtonRefresh, 370, SpringLayout.NORTH, this );
			layout.putConstraint( SpringLayout.WEST, jButtonRefresh, 11, SpringLayout.WEST, this );
			layout.putConstraint( SpringLayout.SOUTH, jButtonRefresh, 395, SpringLayout.NORTH, this );
			layout.putConstraint( SpringLayout.EAST, jButtonRefresh, 131, SpringLayout.WEST, this );
			// this.add( jButtonRefresh );
			jButtonRefresh.setToolTipText( TOOLTIP_REFRESH );
			jButtonRefresh.setFont( SMALL_FONT );
			jButtonRefresh.addActionListener( new ActionListener()
			{
				@Override
				public void actionPerformed( final ActionEvent e )
				{
					refresh();
				}
			} );
		}

		{
			btnPreview = new JButton( "Preview", ICON_PREVIEW );
			layout.putConstraint( SpringLayout.NORTH, btnPreview, 370, SpringLayout.NORTH, this );
			layout.putConstraint( SpringLayout.WEST, btnPreview, -141, SpringLayout.EAST, this );
			layout.putConstraint( SpringLayout.SOUTH, btnPreview, 395, SpringLayout.NORTH, this );
			layout.putConstraint( SpringLayout.EAST, btnPreview, -10, SpringLayout.EAST, this );
			this.add( btnPreview );
			btnPreview.setFont( SMALL_FONT );
			btnPreview.addActionListener( new ActionListener()
			{
				public void actionPerformed ( final ActionEvent e ){ preview(); }
			});
		}

		{

			// Deal with channels: the slider and channel labels are only
			// visible if we find more than one channel.
			final int n_channels = imp.getNChannels();
			sliderChannel.setMaximum( n_channels );
			sliderChannel.setMinimum( 1 );
			sliderChannel.setValue( imp.getChannel() );

			if ( n_channels <= 1 )
			{
				labelChannel.setVisible( false );
				lblSegmentInChannel.setVisible( false );
				sliderChannel.setVisible( false );
			}
			else
			{
				labelChannel.setVisible( true );
				lblSegmentInChannel.setVisible( true );
				sliderChannel.setVisible( true );
			}
		}

		{
			final JLabelLogger labelLogger = new JLabelLogger();
			layout.putConstraint( SpringLayout.NORTH, labelLogger, 407, SpringLayout.NORTH, this );
			layout.putConstraint( SpringLayout.WEST, labelLogger, 10, SpringLayout.WEST, this );
			layout.putConstraint( SpringLayout.SOUTH, labelLogger, 431, SpringLayout.NORTH, this );
			layout.putConstraint( SpringLayout.EAST, labelLogger, -10, SpringLayout.EAST, this );
			add( labelLogger );
			localLogger = labelLogger.getLogger();
		}


		//Label and Field for Non-max suppression
		{
			jLabel3 = new JLabel();
			layout.putConstraint( SpringLayout.NORTH, jLabel3, 289, SpringLayout.NORTH, this );
			layout.putConstraint( SpringLayout.WEST, jLabel3, 16, SpringLayout.WEST, this );
			this.add( jLabel3 );
			jLabel3.setText( "IOU Threshold:" );
			jLabel3.setFont( FONT );

		}
		{
			jTextFieldIOUThreshold = new JNumericTextField();
			layout.putConstraint( SpringLayout.NORTH, jTextFieldIOUThreshold, 289, SpringLayout.NORTH, this );
			layout.putConstraint( SpringLayout.WEST, jTextFieldIOUThreshold, 168, SpringLayout.WEST, this );
			layout.putConstraint( SpringLayout.SOUTH, jTextFieldIOUThreshold, 305, SpringLayout.NORTH, this );
			layout.putConstraint( SpringLayout.EAST, jTextFieldIOUThreshold, 208, SpringLayout.WEST, this );
			jTextFieldIOUThreshold.setText( Double.toString(DEFAULT_IOU_THRESHOLD) );
			jTextFieldIOUThreshold.setColumns( 5 );
			this.add( jTextFieldIOUThreshold );
			jTextFieldIOUThreshold.setFont( FONT );
		}

		//load pretrained model
		{
			btnPretrainedModel = new JButton( "Pretrained model" );
			layout.putConstraint( SpringLayout.NORTH, btnPretrainedModel, 370, SpringLayout.NORTH, this );
			layout.putConstraint( SpringLayout.WEST, btnPretrainedModel, 11, SpringLayout.WEST, this );
			layout.putConstraint( SpringLayout.SOUTH, btnPretrainedModel, 395, SpringLayout.NORTH, this );
			layout.putConstraint( SpringLayout.EAST, btnPretrainedModel, 140, SpringLayout.WEST, this );
			this.add( btnPretrainedModel );
			btnPretrainedModel.setFont( SMALL_FONT );
			btnPretrainedModel.addActionListener( new ActionListener()
			{
				public void actionPerformed ( final ActionEvent e ){ load_pretrained_model(); }
			});
		}
	}
	public void setSettings(Map<String, Object> settings){}

    public Map< String, Object > getSettings()
    {
        final HashMap< String, Object > settings = new HashMap< >( 5 );
        final int targetChannel = sliderChannel.getValue();
        final double expectedRadius = NumberParser.parseDouble( jTextFieldBlobDiameter.getText() ) / 2;
        final double softmax_threshold = NumberParser.parseDouble( jTextFieldSoftmaxThreshold.getText() );
		final double iou_threshold = NumberParser.parseDouble( jTextFieldIOUThreshold.getText() );
        final boolean doSubPixelLocalization = jCheckSubPixel.isSelected();
        settings.put( KEY_TARGET_CHANNEL, targetChannel );
		settings.put( KEY_PATH_TO_PB, path_to_pb);
        settings.put( KEY_RADIUS, expectedRadius );
        settings.put( KEY_SOFTMAX_THRESHOLD, softmax_threshold );
		settings.put( KEY_IOU_THRESHOLD, iou_threshold );
        settings.put( KEY_DO_SUBPIXEL_LOCALIZATION, doSubPixelLocalization );

        return settings;
    }

	public void clean(){}
}

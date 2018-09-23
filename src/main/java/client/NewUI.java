package client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import util.MapPoint;

public class NewUI extends JFrame {

  private JPanel mBasePanel;
  private JButton mGoButton;
  private JSlider mThreshold;
  private JPanel mMapContainer;
  private JTextField mSourceLat;
  private JTextField mSourceLong;
  private JTextField mDestLat;
  private JTextField mDestLong;
  private JTextArea mStatus;

  private OnMapRequest mRequestHandler;
  private ProcessedData mData;

  public NewUI(int width, int height, String mapFilePath, String title) {
    setSize(width, height);
    setContentPane(mBasePanel);
    setLocationRelativeTo(null);
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

    mGoButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent actionEvent) {
        try {
          double sourceLat = Double.parseDouble(mSourceLat.getText());
          double sourceLong = Double.parseDouble(mSourceLong.getText());
          double destLat = Double.parseDouble(mDestLat.getText());
          double destLong = Double.parseDouble(mDestLong.getText());
          ProcessedData data = mRequestHandler
              .fulfillRequest(new MapPoint(sourceLat, sourceLong), new MapPoint(destLat, destLong));
          UpdateVisualization(data);
        } catch (NumberFormatException e) {
          mStatus.setText("Invalid input format. \n" + e.toString());
        }
      }
    });
  }

  private void UpdateVisualization(ProcessedData newData) {
    mData = newData;
  }
}

class ProcessedData {

  public int mNumSegments;
  public ArrayList<PathPart> mPathParts;
  public double mRegionLeft;
  public double mRegionTop;
  public double mRegionRight;
  public double mRegionBottom;

  class PathPart implements Comparable<PathPart> {

    public ArrayList<MapPoint> mPathPoints;
    public ArrayList<MapPoint> mSourcePoints;
    public ArrayList<MapPoint> mDestPoints;
    public Double mMetrics;

    @Override
    public int compareTo(PathPart other) {
      return mMetrics.compareTo(other.mMetrics);
    }
  }

  void sortPathAccordingToMetrics() {
    Collections.sort(mPathParts);
  }
}

interface OnMapRequest {

  ProcessedData fulfillRequest(MapPoint source, MapPoint destination);
}

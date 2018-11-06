package client;

import com.alee.laf.WebLookAndFeel;
import com.sun.istack.internal.NotNull;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.map.awt.graphics.AwtGraphicFactory;
import org.mapsforge.map.awt.input.MouseEventListener;
import org.mapsforge.map.awt.view.MapView;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.datastore.MultiMapDataStore;
import org.mapsforge.map.layer.cache.FileSystemTileCache;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.cache.TwoLevelTileCache;
import org.mapsforge.map.layer.renderer.MapWorkerPool;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.reader.ReadBuffer;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.util.MapViewProjection;
import util.MapPoint;

/**
 * Responsible for initiating request to server and computing the overlap.
 */
abstract class OnMapRequest {

  private ProcessedData mProcessedData;

  /**
   * Implement in subclasses, for initiating the request.
   *
   * @param source The source geo location
   * @param destination The destination geo location
   */
  abstract public ProcessedData fulfillRequest(MapPoint source, MapPoint destination);

  ProcessedData runRequestThread(MapPoint source, MapPoint destination) {
    mProcessedData = null;
    RequestThread thread = new RequestThread(source, destination);
    thread.start();
    try {
      // Timeout after one minute.
      thread.join(60000);
    } catch (InterruptedException e) {
      return null;
    }
    if (thread.isAlive()) {
      thread.interrupt();
    }
    return mProcessedData;
  }

  private class RequestThread extends Thread {

    private MapPoint mSource;
    private MapPoint mDestination;

    public RequestThread(MapPoint source, MapPoint destination) {
      mSource = source;
      mDestination = destination;
    }

    @Override
    public void run() {
      mProcessedData = fulfillRequest(mSource, mDestination);
    }
  }
}

//
//  private int findNearest(MapPoint original) {
//    int closest = mHopper.getLocationIndex()
//        .findClosest(original.getLat(), original.getLon(), EdgeFilter.ALL_EDGES)
//        .getClosestNode();
//    return closest;
//  }

public class WindowUI extends JFrame {

  private final MapView MAP_VIEW;
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

  // To specify the state current mouse handler is in
  private int mUIStage;

  public WindowUI(int width, int height, String mapFilePath, String title,
      OnMapRequest requestHandler) {
    mStatus.setLineWrap(true);
    mStatus.setWrapStyleWord(true);

    mThreshold.setValue(100);

    mUIStage = 0;

    mRequestHandler = requestHandler;

    mGoButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent actionEvent) {
        try {
          mUIStage = 0;
          mStatus.setText("");
          String sourceLatStr = mSourceLat.getText();
          String sourceLongStr = mSourceLong.getText();
          String destLatStr = mDestLat.getText();
          String destLongStr = mDestLong.getText();
          if (sourceLatStr.isEmpty() || sourceLongStr.isEmpty() || destLatStr.isEmpty()
              || destLongStr.isEmpty()) {
            mStatus.setText("Please provide locations of source and destination");
            return;
          }
          double sourceLat = Double.parseDouble(mSourceLat.getText());
          double sourceLong = Double.parseDouble(mSourceLong.getText());
          double destLat = Double.parseDouble(mDestLat.getText());
          double destLong = Double.parseDouble(mDestLong.getText());
          if (mRequestHandler == null) {
            mStatus.setText("RequestHandler not set");
            return;
          }
          ProcessedData data = mRequestHandler
              .runRequestThread(new MapPoint(sourceLat, sourceLong),
                  new MapPoint(destLat, destLong));
          System.out.println(Thread.activeCount());
          if (data == null) {
            mStatus.setText("Request failed. Check connection.");
          }
          mThreshold.setValue(100);
          UpdateVisualization(data);
        } catch (NumberFormatException e) {
          mStatus.setText("Invalid input format. \n" + e.toString());
        }
      }
    });

    // Increase read buffer size
    ReadBuffer.setMaximumBufferSize(6500000);

    MAP_VIEW = new MapView();
    MAP_VIEW.addMouseListener(new MouseEvent());
    MAP_VIEW.getMapScaleBar().setVisible(true);
    MAP_VIEW.getModel().displayModel.setFixedTileSize(512);

    // Load Map UI data
    final BoundingBox boundingBox = addLayers(mapFilePath);

    // Multi-threading rendering
    MapWorkerPool.NUMBER_OF_THREADS = 2;

    mMapContainer.setLayout(new GridLayout());
    mMapContainer.add(MAP_VIEW);

    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        MAP_VIEW.destroyAll();
        AwtGraphicFactory.clearResourceMemoryCache();
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
      }

      @Override
      public void windowOpened(WindowEvent e) {
        final Model model = MAP_VIEW.getModel();
        byte zoomLevel = LatLongUtils
            .zoomForBounds(model.mapViewDimension.getDimension(), boundingBox,
                model.displayModel.getTileSize());
        model.mapViewPosition
            .setMapPosition(new MapPosition(boundingBox.getCenterPoint(), zoomLevel));
      }
    });

    setTitle(title);
    setSize(width, height);
    setContentPane(mBasePanel);
    setLocationRelativeTo(null);
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
  }

  public void run() {
    setVisible(true);
  }

  // Load the map data for displaying

  private void UpdateVisualization(ProcessedData newData) {
    mData = newData;
  }

  private BoundingBox addLayers(String mapUIPath) {
    MAP_VIEW.getModel().displayModel.setFixedTileSize(512);
    MultiMapDataStore mapDataStore = new MultiMapDataStore(MultiMapDataStore.DataPolicy.RETURN_ALL);
    mapDataStore.addMapDataStore(new MapFile(mapUIPath), false, false);
    TileRendererLayer tileRendererLayer = createTileRendererLayer(createTileCache(64), mapDataStore,
        MAP_VIEW.getModel().mapViewPosition);
    MAP_VIEW.getLayerManager().getLayers().add(tileRendererLayer);
    return mapDataStore.boundingBox();
  }

  private TileCache createTileCache(int capacity) {
    TileCache firstLevelTileCache = new InMemoryTileCache(capacity);
    File cacheDirectory = new File(System.getProperty("java.io.tmpdir"), "mapsforge");
    TileCache secondLevelTileCache = new FileSystemTileCache(1024, cacheDirectory,
        AwtGraphicFactory.INSTANCE);
    return new TwoLevelTileCache(firstLevelTileCache, secondLevelTileCache);
  }

  private TileRendererLayer createTileRendererLayer(TileCache tileCache, MapDataStore mapDataStore,
      MapViewPosition mapViewPosition) {
    TileRendererLayer tileRendererLayer = new TileRendererLayer(tileCache, mapDataStore,
        mapViewPosition, false, false, false, AwtGraphicFactory.INSTANCE);
    tileRendererLayer.setXmlRenderTheme(InternalRenderTheme.OSMARENDER);
    return tileRendererLayer;
  }

  private class MouseEvent extends MouseEventListener {

    private MapViewProjection mReference;

    MouseEvent() {
      super(MAP_VIEW);
      mReference = new MapViewProjection(MAP_VIEW);
    }

    @Override
    public void mouseClicked(java.awt.event.MouseEvent e) {
      super.mouseClicked(e);

      LatLong location = mReference.fromPixels(e.getX(), e.getY());
      switch (mUIStage) {
        case 0: {
          mSourceLat.setText(Double.toString(location.getLatitude()));
          mSourceLong.setText(Double.toString(location.getLongitude()));
          mUIStage = 1;
          break;
        }
        case 1: {
          mDestLat.setText(Double.toString(location.getLatitude()));
          mDestLong.setText(Double.toString(location.getLongitude()));
          mUIStage = 2;
          break;
        }
        case 2: {
          break;
        }
        default: {
          MAP_VIEW.getLayerManager().redrawLayers();
          break;
        }
      }
    }
  }
}

class ProcessedData {

  public int mNumSegments;
  public ArrayList<PathPart> mPathParts;
  public double mRegionLeft;
  public double mRegionTop;
  public double mRegionRight;
  public double mRegionBottom;

  void sortPathAccordingToMetrics() {
    Collections.sort(mPathParts);
  }

  public class PathPart implements Comparable<PathPart> {

    public ArrayList<MapPoint> mPathPoints;
    public ArrayList<MapPoint> mSourcePoints;
    public ArrayList<MapPoint> mDestPoints;
    public Double mMetrics;

    @Override
    public int compareTo(@NotNull PathPart other) {
      return mMetrics.compareTo(other.mMetrics);
    }
  }
}

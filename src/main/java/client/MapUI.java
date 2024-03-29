package client;

import java.awt.Dimension;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.core.util.Parameters;
import org.mapsforge.map.awt.graphics.AwtGraphicFactory;
import org.mapsforge.map.awt.util.JavaPreferences;
import org.mapsforge.map.awt.view.MapView;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.datastore.MultiMapDataStore;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.cache.FileSystemTileCache;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.cache.TwoLevelTileCache;
import org.mapsforge.map.layer.debug.TileCoordinatesLayer;
import org.mapsforge.map.layer.debug.TileGridLayer;
import org.mapsforge.map.layer.overlay.Circle;
import org.mapsforge.map.layer.overlay.Polyline;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.IMapViewPosition;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.model.common.PreferencesFacade;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.util.MapViewProjection;
import util.Convex;
import util.Logger;
import util.MainPathEmptyException;
import util.MapPoint;
import util.Pair;
import util.Triple;

public final class MapUI implements PostProcess {

  private final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;
  private final boolean SHOW_DEBUG_LAYERS = false;

  private final MapView MAP_VIEW;
  private final JFrame FRAME;
  private final float THRESHOLD = 0.8f;
  // shitty implementation
  private ArrayList<LatLong> mStarts;
  private ArrayList<LatLong> mEnds;
  private List<ConvexLayer> mSources;
  private List<ConvexLayer> mTargets;
  private List<LineLayer> mLayers;
  private List<ArrayList<LatLong>> mPaths;
  private HashSet<Pair> mMainPathSet;
  private List<LatLong> mMainPath;
  private List<Triple<LineLayer, ConvexLayer, ConvexLayer>> mLineSourcesTargets;

  public MapUI(String mapFileLocation, String windowTitle) {
    // Multithreading rendering
    Parameters.NUMBER_OF_THREADS = 2;

    List<File> mapFiles = new ArrayList<File>();
    mapFiles.add(new File(mapFileLocation));
    MAP_VIEW = createMapView();
    final MapView mapView = MAP_VIEW;
    final BoundingBox boundingBox = addLayers(mapView, mapFiles);

    final PreferencesFacade preferencesFacade = new JavaPreferences(
        Preferences.userNodeForPackage(MapUI.class));

    FRAME = new JFrame();

    MAP_VIEW.addMouseListener(new MouseEvent());
    FRAME.setTitle(windowTitle);
    FRAME.add(MAP_VIEW);
    FRAME.pack();
    FRAME.setSize(new Dimension(800, 600));
    FRAME.setLocationRelativeTo(null);
    FRAME.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    FRAME.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        mapView.getModel().save(preferencesFacade);
        mapView.destroyAll();
        AwtGraphicFactory.clearResourceMemoryCache();
        FRAME.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
      }

      @Override
      public void windowOpened(WindowEvent e) {
        final Model model = mapView.getModel();
        byte zoomLevel = LatLongUtils
            .zoomForBounds(model.mapViewDimension.getDimension(), boundingBox,
                model.displayModel.getTileSize());
        model.mapViewPosition
            .setMapPosition(new MapPosition(boundingBox.getCenterPoint(), zoomLevel));
      }
    });
    mPaths = new ArrayList<>();
    mMainPathSet = new HashSet<>();
    mLayers = new ArrayList<>();
    mStarts = new ArrayList<>();
    mEnds = new ArrayList<>();
    mSources = new ArrayList<>();
    mTargets = new ArrayList<>();
    mLineSourcesTargets = new ArrayList<>();
  }

  public int createDot(LatLong coordinates, int color, float strokeWidth) {
    ArrayList<LatLong> list = new ArrayList<>();
    list.add(coordinates);
    list.add(coordinates);
    return createPolyline(list, color, strokeWidth);
  }

  public int createPolyline(List<LatLong> coordinates, int color, float strokeWidth) {
    org.mapsforge.core.graphics.Paint paintStroke = GRAPHIC_FACTORY.createPaint();
    paintStroke.setStyle(Style.STROKE);
    paintStroke.setColor(color);
    paintStroke.setStrokeWidth(strokeWidth);
    Polyline pl = new Polyline(paintStroke, GRAPHIC_FACTORY);
    pl.getLatLongs().addAll(coordinates);
    MAP_VIEW.getLayerManager().getLayers().add(pl);
    MAP_VIEW.getLayerManager().redrawLayers();
    return pl.hashCode();
  }

  public int createCircle(LatLong latLong, int color, float radius) {
    org.mapsforge.core.graphics.Paint paintStroke = GRAPHIC_FACTORY.createPaint();
    paintStroke.setStyle(Style.STROKE);
    paintStroke.setColor(color);
    paintStroke.setStrokeWidth(3.0f);
    Circle circle = new Circle(latLong, radius, null, paintStroke);
    MAP_VIEW.getLayerManager().getLayers().add(circle);
    MAP_VIEW.getLayerManager().redrawLayers();
    return 0;
  }

  @Override
  public void setMainPath(ArrayList<MapPoint> path) throws MainPathEmptyException {
    if (path.size() > 0) {
      ArrayList<LatLong> list = new ArrayList<LatLong>();
      LatLong prev = new MapPoint.LatLongAdapter(path.get(0));
      LatLong curt;
      list.add(prev);
      for (int i = 1; i < path.size(); i++) {
        curt = new MapPoint.LatLongAdapter(path.get(i));
        list.add(curt);
        mMainPathSet.add(new Pair<>(prev, curt));
        prev = curt;
      }
      mMainPath = list;
      mPaths.add(list);
    } else {
      throw new MainPathEmptyException();
    }
  }

  @Override
  public void addPath(ArrayList<MapPoint> path) throws MainPathEmptyException {
    if (mPaths.size() == 0) {
      throw new MainPathEmptyException();
    }
    if (path.size() > 0) {
      ArrayList<LatLong> list = new ArrayList<>();
      for (int i = 0; i < path.size(); i++) {
        list.add(new LatLong(path.get(i).mFirst, path.get(i).mSecond));
      }
      mPaths.add(list);
      // shitty implementation
      mStarts.add(new LatLong(path.get(0).mFirst, path.get(0).mSecond));
      mEnds.add(new LatLong(path.get(path.size() - 1).mFirst, path.get(path.size() - 1).mSecond));
    }
  }

  @Override
  public void done() {

    Logger.printf(Logger.DEBUG, "Total number of path: " + mPaths.size());

    if (mMainPath != null) {
      for (LineLayer myLineLayer : mLayers) {
        MAP_VIEW.getLayerManager().getLayers().remove(myLineLayer);
      }
      mLayers.clear();
      HashSet<Pair<LatLong, LatLong>> overLapList;        // List for the start and end point of the paths
      HashMap<Pair<LatLong, LatLong>, HashSet<Pair<LatLong, LatLong>>> otherPaths = new HashMap<>();
      // convert path to hashset
      for (int i = 0; i < mPaths.size(); i++) {
        List<LatLong> path = mPaths.get(i);
        LatLong prev = path.get(0);
        LatLong curt;
        HashSet<Pair<LatLong, LatLong>> set = new HashSet<>();
        for (int j = 1; j < path.size(); j++) {
          curt = path.get(j);
          set.add(new Pair<>(prev, curt));
          prev = curt;
        }
        otherPaths.put(new Pair<>(path.get(0), path.get(path.size() - 1)), set);
      }
      LatLong prev = mMainPath.get(0);
      LatLong curt;
      for (int i = 1; i < mMainPath.size(); i++) {
        curt = mMainPath.get(i);
        overLapList = new HashSet<>();
        Pair<LatLong, LatLong> curPair = new Pair<>(prev, curt);
        ArrayList<LatLong> list = new ArrayList<>();

        for (Pair<LatLong, LatLong> key : otherPaths.keySet()) {
          if (otherPaths.get(key).contains(curPair)) {
            overLapList.add(key);
          }
        }
        list.add(prev);
        list.add(curt);
        while (overLapList.size() > 0) {
          i++;
          if (i >= mMainPath.size()) {
            break;
          }
          prev = curt;
          curt = mMainPath.get(i);
          curPair = new Pair<>(prev, curt);
          int counter = 0;
          for (Pair key : otherPaths.keySet()) {
            if (otherPaths.get(key).contains(curPair)) {
              if (overLapList.contains(key)) {
                counter++;
              } else {
                counter = -1;
                break;
              }
            }
          }
          if (counter < 0 || counter != overLapList.size()) {
            i--;
            break;
          }
          list.add(curt);
        }

        HashMap<LatLong, Integer> dots = new HashMap<>();

        ArrayList<LatLong> sourceDots = new ArrayList<>();
        ArrayList<LatLong> targetDots = new ArrayList<>();

        for (Pair<LatLong, LatLong> p : overLapList) {
          dots.put(p.mFirst, new java.awt.Color(6, 0, 133, 255).getRGB());
          dots.put(p.mSecond, new java.awt.Color(6, 0, 133, 255).getRGB());
          sourceDots.add(p.mFirst);
          targetDots.add(p.mSecond);
        }

        if (overLapList.size() > 1) {
          LineLayer lineLayer = new LineLayer(GRAPHIC_FACTORY, dots,
              getHeatMapColor(overLapList.size() / (0.0f + mPaths.size())),
              6.0f, list);

          MAP_VIEW.getLayerManager().getLayers().add(lineLayer);
          mLayers.add(lineLayer);
          ConvexLayer newSource = new ConvexLayer(GRAPHIC_FACTORY,
              getHeatMapColor(overLapList.size() / (0.0f + mPaths.size())),
              6.0f, sourceDots);

          mSources.add(newSource);
          ConvexLayer newTarget = new ConvexLayer(GRAPHIC_FACTORY,
              getHeatMapColor(overLapList.size() / (0.0f + mPaths.size())),
              6.0f, targetDots);

          mTargets.add(newTarget);

          mLineSourcesTargets.add(new Triple<>(lineLayer, newSource, newTarget));
          MAP_VIEW.getLayerManager().getLayers().add(newSource);
          MAP_VIEW.getLayerManager().getLayers().add(newTarget);
          newSource.setVisible(false);
          newTarget.setVisible(false);
          MAP_VIEW.getLayerManager().redrawLayers();
        }
        prev = curt;
      }
    }
    FRAME.setVisible(true);
  }

  /////// Helper Functions ///////

  private int getHeatMapColor(float value) {
    int color[][] = {{6, 0, 133, 255}, {255, 255, 0}};
    int color2[][] = {{255, 255, 0}, {255, 14, 29, 255}};
    int r, g, b;
    if (value < THRESHOLD) {
      value *= 10.0f / (THRESHOLD * 10);
      r = (int) ((color[1][0] - color[0][0]) * value + color[0][0]);
      g = (int) ((color[1][1] - color[0][1]) * value + color[0][1]);
      b = (int) ((color[1][2] - color[0][2]) * value + color[0][2]);
    } else {
      value -= THRESHOLD;
      value *= 10.0f / ((1 - THRESHOLD) * 10);
      r = (int) ((color2[1][0] - color2[0][0]) * value + color2[0][0]);
      g = (int) ((color2[1][1] - color2[0][1]) * value + color2[0][1]);
      b = (int) ((color2[1][2] - color2[0][2]) * value + color2[0][2]);
    }
    return new java.awt.Color(r, g, b, 255).getRGB();
  }

  private BoundingBox addLayers(MapView mapView, List<File> mapFiles) {
    Layers layers = mapView.getLayerManager().getLayers();

    // Vector
    mapView.getModel().displayModel.setFixedTileSize(512);
    MultiMapDataStore mapDataStore = new MultiMapDataStore(MultiMapDataStore.DataPolicy.RETURN_ALL);
    for (File file : mapFiles) {
      mapDataStore.addMapDataStore(new MapFile(file), false, false);
    }
    TileRendererLayer tileRendererLayer = createTileRendererLayer(createTileCache(64), mapDataStore,
        mapView.getModel().mapViewPosition);
    layers.add(tileRendererLayer);
    BoundingBox boundingBox = mapDataStore.boundingBox();

    // Debug
    if (SHOW_DEBUG_LAYERS) {
      layers.add(new TileGridLayer(GRAPHIC_FACTORY, mapView.getModel().displayModel));
      layers.add(new TileCoordinatesLayer(GRAPHIC_FACTORY, mapView.getModel().displayModel));
    }

    return boundingBox;
  }

  private MapView createMapView() {
    MapView mapView = new MapView();
    mapView.getMapScaleBar().setVisible(true);
    if (SHOW_DEBUG_LAYERS) {
      mapView.getFpsCounter().setVisible(true);
    }

    return mapView;
  }

  private TileCache createTileCache(int capacity) {
    TileCache firstLevelTileCache = new InMemoryTileCache(capacity);
    File cacheDirectory = new File(System.getProperty("java.io.tmpdir"), "mapsforge");
    TileCache secondLevelTileCache = new FileSystemTileCache(1024, cacheDirectory, GRAPHIC_FACTORY);
    return new TwoLevelTileCache(firstLevelTileCache, secondLevelTileCache);
  }

  private TileRendererLayer createTileRendererLayer(TileCache tileCache, MapDataStore mapDataStore,
      IMapViewPosition mapViewPosition) {
    TileRendererLayer tileRendererLayer = new TileRendererLayer(tileCache, mapDataStore,
        mapViewPosition, false, false, false, GRAPHIC_FACTORY) {
      @Override
      public boolean onTap(LatLong tapLatLong, org.mapsforge.core.model.Point layerXY,
          org.mapsforge.core.model.Point tapXY) {
        Logger.println(Logger.DEBUG, "Tap on: " + tapLatLong);
        return true;
      }
    };
    tileRendererLayer.setXmlRenderTheme(InternalRenderTheme.OSMARENDER);
    return tileRendererLayer;
  }

  private class MouseEvent implements MouseListener {

    private MapViewProjection mReference;

    MouseEvent() {
      mReference = new MapViewProjection(MAP_VIEW);
    }

    public void mousePressed(java.awt.event.MouseEvent e) {

    }

    public void mouseReleased(java.awt.event.MouseEvent e) {

    }

    public void mouseEntered(java.awt.event.MouseEvent e) {

    }

    public void mouseExited(java.awt.event.MouseEvent e) {

    }

    public void mouseClicked(java.awt.event.MouseEvent e) {
      LatLong location = mReference.fromPixels(e.getX(), e.getY());
      double min_dist = 100.0;
      Triple<LineLayer, ConvexLayer, ConvexLayer> bestLayer = null;
      for (Triple<LineLayer, ConvexLayer, ConvexLayer> triple : mLineSourcesTargets) {
        LineLayer lineLayer = triple.mFirst;
        double dist = lineLayer.contains(location);
        if (dist > 0 && dist < min_dist) {
          min_dist = dist;
          bestLayer = triple;
        }
      }
      for (Triple<LineLayer, ConvexLayer, ConvexLayer> triple : mLineSourcesTargets) {
        triple.mFirst.setVisible(false);
        triple.mSecond.setVisible(false);
        triple.mThird.setVisible(false);
      }
      if (bestLayer == null) {
        for (Triple<LineLayer, ConvexLayer, ConvexLayer> triple : mLineSourcesTargets) {
          triple.mFirst.setVisible(true);
        }
      } else {
        bestLayer.mFirst.setVisible(true);
        bestLayer.mSecond.setVisible(true);
        bestLayer.mThird.setVisible(true);
      }
      MAP_VIEW.getLayerManager().redrawLayers();
    }
  }

  private class ConvexLayer extends Polyline {

    public ConvexLayer(GraphicFactory graphicFactory, int pathColor,
        float pathStrokeWidth, ArrayList<LatLong> dots) {
      super(null, graphicFactory);
      org.mapsforge.core.graphics.Paint paintStroke = GRAPHIC_FACTORY.createPaint();
      paintStroke.setStyle(Style.STROKE);
      paintStroke.setColor(pathColor);
      paintStroke.setStrokeWidth(pathStrokeWidth);
      this.setPaintStroke(paintStroke);
      List<LatLong> convex = MapPoint
          .convertToLatlong(Convex.getConvex(MapPoint.convertFromLatlong(dots)));
      if (convex != null) {
        super.getLatLongs().addAll(convex);
      } else {
        System.out.println("got null");
      }
    }
  }

  private class LineLayer extends Polyline {

    private HashMap<LatLong, Integer> mDots;
    private List<LatLong> mPath;

    public LineLayer(GraphicFactory graphicFactory, HashMap<LatLong, Integer> dotWithColor,
        int pathColor,
        float pathStrokeWidth, List<LatLong> path) {
      super(null, graphicFactory);
      org.mapsforge.core.graphics.Paint paintStroke = GRAPHIC_FACTORY.createPaint();
      paintStroke.setStyle(Style.STROKE);
      paintStroke.setColor(pathColor);
      paintStroke.setStrokeWidth(pathStrokeWidth);
      this.setPaintStroke(paintStroke);
      mDots = dotWithColor;
      mPath = path;

      super.getLatLongs().addAll(mPath);
    }

    @Override
    public synchronized void draw(BoundingBox boundingBox, byte zoomLevel,
        org.mapsforge.core.graphics.Canvas canvas, org.mapsforge.core.model.Point topLeftPoint) {
      super.draw(boundingBox, zoomLevel, canvas, topLeftPoint);
      long mapSize = MercatorProjection.getMapSize(zoomLevel, displayModel.getTileSize());
      /** Draw the points **/
      for (LatLong latLong : mDots.keySet()) {
        int pixelX = (int) (MercatorProjection.longitudeToPixelX(latLong.longitude, mapSize)
            - topLeftPoint.x);
        int pixelY = (int) (MercatorProjection.latitudeToPixelY(latLong.latitude, mapSize)
            - topLeftPoint.y);
        org.mapsforge.core.graphics.Paint paintStroke = GRAPHIC_FACTORY.createPaint();
        paintStroke.setStyle(Style.STROKE);
        paintStroke.setColor(mDots.get(latLong));
        paintStroke.setStrokeWidth(6.0f);
        canvas.drawCircle(pixelX, pixelY, 3, paintStroke);
      }
    }

    public double contains(LatLong point) {
      double threshold = 0.01;
      for (LatLong dot : mPath) {
        double dist = point.distance(dot);
        if (dist < threshold) {
          return dist;
        }
      }
      return -1.0;
    }
  }
}

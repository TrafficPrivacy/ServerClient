package client;

import com.graphhopper.storage.NodeAccess;
import java.awt.Dimension;
import java.awt.event.ContainerEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.mapsforge.map.layer.overlay.Polygon;
import org.mapsforge.map.layer.overlay.Polyline;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.IMapViewPosition;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.model.common.PreferencesFacade;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import util.Convex;
import util.Logger;
import util.MapPoint;
import util.MapPoint.LatLongAdapter;
import util.Pair;

public final class Visualizer {

  private final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;
  private final boolean SHOW_DEBUG_LAYERS = false;

  private final MapView MAP_VIEW;
  private final JFrame FRAME;
  private final float STROKEWIDTH = 1.0f;
  private final float BORDERSTROKEWIDTH = 1.0f;
  private final java.awt.Color DOTCOLOR = new java.awt.Color(31, 46, 250, 255);
  private final java.awt.Color FILLCOLOR = new java.awt.Color(255, 0, 4, 63);
  private final java.awt.Color BORDERCOLOR = new java.awt.Color(255, 0, 4, 164);
  private NodeAccess mNodeAccess;

  public Visualizer(String mapFileLocation, String windowTitle, NodeAccess nodeAccess) {
    // Multi threading rendering
    Parameters.NUMBER_OF_THREADS = 2;

    List<File> mapFiles = new ArrayList<File>();
    mapFiles.add(new File(mapFileLocation));
    MAP_VIEW = createMapView();
    final MapView mapView = MAP_VIEW;
    final BoundingBox boundingBox = addLayers(mapView, mapFiles);

    final PreferencesFacade preferencesFacade = new JavaPreferences(
        Preferences.userNodeForPackage(MapUI.class));

    FRAME = new JFrame();

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

    mNodeAccess = nodeAccess;
  }

  public void show() {
    FRAME.setVisible(true);
  }

  public void drawRegions(ArrayList<Pair<int[], int[]>> regions) {
    for (Pair<int[], int[]> pair : regions) {
      int[] points = pair.mFirst;
      int[] borderPoints = pair.mSecond;
      for (int point : points) {
        MapPoint mapPoint = MapPoint.convertFromGHPointIndex(point, mNodeAccess);
        createDot(new LatLongAdapter(mapPoint), STROKEWIDTH);
      }
      ArrayList<MapPoint> borders = new ArrayList<>();
      for (int borderPoint : borderPoints) {
        borders.add(MapPoint.convertFromGHPointIndex(borderPoint, mNodeAccess));
      }
      List<LatLong> convex = MapPoint.convertToLatlong(Convex.getConvex(borders));
      createPolygon(convex, BORDERSTROKEWIDTH);
    }
  }

  public void createDot(LatLong coordinates, float strokeWidth) {
    createCircle(coordinates, DOTCOLOR.getRGB(), 3, true);
  }

  public void createPolygon(List<LatLong> coordinates, float strokeWidth) {
    org.mapsforge.core.graphics.Paint paintStroke = GRAPHIC_FACTORY.createPaint();
    org.mapsforge.core.graphics.Paint paintFill = GRAPHIC_FACTORY.createPaint();
    paintStroke.setStyle(Style.STROKE);
    paintStroke.setColor(BORDERCOLOR.getRGB());
    paintStroke.setStrokeWidth(strokeWidth);
    paintFill.setColor(FILLCOLOR.getRGB());
    Polygon pg = new Polygon(paintFill, paintStroke, GRAPHIC_FACTORY);
    pg.addPoints(coordinates);
    MAP_VIEW.getLayerManager().getLayers().add(pg);
    MAP_VIEW.getLayerManager().redrawLayers();
  }

  public void createPolyline(List<LatLong> coordinates, int color, float strokeWidth) {
    org.mapsforge.core.graphics.Paint paintStroke = GRAPHIC_FACTORY.createPaint();
    paintStroke.setStyle(Style.STROKE);
    paintStroke.setColor(color);
    paintStroke.setStrokeWidth(strokeWidth);
    Polyline pl = new Polyline(paintStroke, GRAPHIC_FACTORY);
    pl.getLatLongs().addAll(coordinates);
    MAP_VIEW.getLayerManager().getLayers().add(pl);
    MAP_VIEW.getLayerManager().redrawLayers();
  }

  public int createCircle(LatLong latLong, int color, float radius, boolean fill) {
    org.mapsforge.core.graphics.Paint paintStroke = GRAPHIC_FACTORY.createPaint();
    paintStroke.setStyle(Style.STROKE);
    paintStroke.setColor(color);
    paintStroke.setStrokeWidth(STROKEWIDTH);
    org.mapsforge.core.graphics.Paint paintFill = GRAPHIC_FACTORY.createPaint();
    paintFill.setColor(color);
    Circle circle = new Circle(latLong, radius, fill ? paintFill : null, paintStroke);
    MAP_VIEW.getLayerManager().getLayers().add(circle);
    MAP_VIEW.getLayerManager().redrawLayers();
    return 0;
  }

  /////// Helper Functions ///////

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
      // Draw the points
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

package client;

import com.graphhopper.GraphHopper;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import java.io.File;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.map.awt.graphics.AwtGraphicFactory;
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
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.reader.ReadBuffer;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import util.MapPoint;

/**
 * A client with user interface
 */
public class UIClient {

  private int mServerPort;
  private String mServerIP;
  private GraphHopper mHopper;

  private final MapView MAP_VIEW;
  private final JFrame FRAME;

  public UIClient(String serverIP, int serverPort, String pbfPath, String ghPath,
      String mapUIPath) {
    mServerPort = serverPort;
    EncodingManager em = new EncodingManager("car");
    mHopper = new GraphHopperOSM()
        .setOSMFile(pbfPath)
        .forDesktop()
        .setGraphHopperLocation(ghPath)
        .setEncodingManager(em)
        .importOrLoad();
    mServerIP = serverIP;

    MAP_VIEW = new MapView();
    MAP_VIEW.getMapScaleBar().setVisible(true);
    MAP_VIEW.getModel().displayModel.setFixedTileSize(512);

    // Load Map UI data
    MultiMapDataStore mapDataStore = new MultiMapDataStore(MultiMapDataStore.DataPolicy.RETURN_ALL);
    mapDataStore.addMapDataStore(new MapFile(mapUIPath), false, false);

    // Increase read buffer size
    ReadBuffer.setMaximumBufferSize(6500000);

    // Multithreading rendering
    MapWorkerPool.NUMBER_OF_THREADS = 2;

    // Add widgets to JFrame
    FRAME = new JFrame();
    FRAME.setTitle("UIClient");

    JLayeredPane layeredPane = new JLayeredPane();



  }

  private BoundingBox addLayers(String mapUIPath) {
    MAP_VIEW.getModel().displayModel.setFixedTileSize(512);
    MultiMapDataStore mapDataStore = new MultiMapDataStore(MultiMapDataStore.DataPolicy.RETURN_ALL);
    mapDataStore.addMapDataStore(new MapFile(mapUIPath), false, false);
    TileRendererLayer tileRendererLayer = createTileRendererLayer(createTileCache(64), mapDataStore,
        MAP_VIEW.getModel().mapViewPosition);
    MAP_VIEW.getLayerManager().getLayers().add(tileRendererLayer);
    BoundingBox boundingBox = mapDataStore.boundingBox();
    return boundingBox;
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

  private int findNearest(MapPoint original) {
    int closest = mHopper.getLocationIndex()
        .findClosest(original.getLat(), original.getLon(), EdgeFilter.ALL_EDGES)
        .getClosestNode();
    return closest;
  }

}

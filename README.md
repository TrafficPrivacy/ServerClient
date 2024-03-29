# ServerClient
Server and Client for demo

# To run:
Checkout the repository.

Open the project with Intellij.

Drop the .osm.pbf and .map data into the data directory or download using the script
in the data/ (the total download size will be around 254MB).

Then run Server first and Client second.

## Run from command line with maven:

 - Server:
```bash
mvn exec:java -Dexec.mainClass="server.ServerRunner" \
-Dexec.args="--port 12280 --osmPath data/linois-latest.osm.pbf \
--ghPath data/illinois"
```
 - Client:
```bash
mvn exec:java -Dexec.mainClass="client.ClientRunner" \
-Dexec.args="40.102039 -88.224335 41.8941 -87.711411 \
--port 12280 --osmPath data/illinois-latest.osm.pbf \
--ghPath data/illinois --mapPath data/illinois.map"
```
 - ServerProfiler:
 ```bash
 mvn exec:java -Dexec.mainClass="server.ServerProfiler" \
 -Dexec.args="--iterations 500 --strategy astar \
 --osmPath data/illinois-latest.osm.pbf \ 
 --ghPath data/illinois \
 --outFilePath data/server-astar.csv"
 ```
 - New York Cab Data Server:
```bash
mvn exec:java -Dexec.mainClass="server.ServerRunner" \
-Dexec.args="--port 12280 --osmPath data/new-york-latest.osm.pbf \
--ghPath data/newyork"
```

 - New York Cab Data Client:
```bash
mvn exec:java -Dexec.mainClass="client.NewYorkExperiment" \
-Dexec.args="--port 12280 --osmPath data/new-york-latest.osm.pbf \
--ghPath data/newyork --tripCSV data/yellow_tripdata_2015-12.csv"
```

 - Change the arguments in `-Dexec.args=` for different command line arguments

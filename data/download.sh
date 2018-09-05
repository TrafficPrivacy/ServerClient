#!/bin/sh
wget https://download.geofabrik.de/north-america/us/illinois-latest.osm.pbf
wget http://ftp-stud.hs-esslingen.de/pub/Mirrors/download.mapsforge.org/maps/v4/north-america/us/illinois.map
wget https://download.geofabrik.de/north-america/us/new-york-latest.osm.pbf
# See the full list of trip data here:
# https://github.com/toddwschneider/nyc-taxi-data/blob/master/raw_data_urls.txt
wget https://s3.amazonaws.com/nyc-tlc/trip+data/yellow_tripdata_2015-12.csv

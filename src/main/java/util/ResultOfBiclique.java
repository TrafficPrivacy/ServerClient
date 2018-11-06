package util;

import java.util.HashMap;
import java.util.HashSet;

public class ResultOfBiclique {
    private HashMap<Pair<MapPoint,MapPoint>,Pair<Pair<Integer,Integer>,Pair<Integer,Integer>>> result;
    private HashMap<Pair<MapPoint,MapPoint>,Pair<HashSet<Integer>,HashSet<Integer>>>result_detailed;
    private HashMap<Integer,Pair<MapPoint,MapPoint>>sequence;
    public ResultOfBiclique(HashMap<Pair<MapPoint,MapPoint>,Pair<Pair<Integer,Integer>,Pair<Integer,Integer>>> result_input,HashMap<Pair<MapPoint,MapPoint>,Pair<HashSet<Integer>,HashSet<Integer>>>result_detailed_input,HashMap<Integer,Pair<MapPoint,MapPoint>>sequence_input)
    {
        result=result_input;
        result_detailed=result_detailed_input;
        sequence=sequence_input;
    }
    public HashMap<Pair<MapPoint,MapPoint>,Pair<Pair<Integer,Integer>,Pair<Integer,Integer>>> get_result()
    {
        return result;
    }
    public HashMap<Pair<MapPoint,MapPoint>,Pair<HashSet<Integer>,HashSet<Integer>>> get_detailed_result()
    {
        return result_detailed;
    }
    public HashMap<Integer,Pair<MapPoint,MapPoint>> get_sequence()
    {
        return sequence;
    }
}

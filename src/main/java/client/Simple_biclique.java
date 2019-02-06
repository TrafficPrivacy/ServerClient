package client;

import util.MapPoint;
import util.Pair;
import util.ResultOfBiclique;

import java.util.*;

public class Simple_biclique implements PostProcess{
    private ArrayList<MapPoint> mainpath;
    private ArrayList<ArrayList<MapPoint>> otherpaths;
    private HashMap<Pair<MapPoint,MapPoint>,Pair<Pair<Integer,Integer>,Pair<Integer,Integer>>>result;
    private HashMap<Pair<MapPoint,MapPoint>,Pair<HashSet<Integer>,HashSet<Integer>>>result_detailed;
    private HashMap<Integer,Pair<MapPoint,MapPoint>>sequence;
    private Pair<Integer,Integer> endpoints;
    private HashMap<MapPoint,Integer> indexer;
    private Integer num;


    private HashSet<Integer> create_z(Boolean left,boolean []visited, Integer current,HashMap<Integer,Integer> matched_pairs,HashMap<Pair<Integer,Integer>,Integer> edges_complement,HashSet<Integer> matched_right)
    {
        HashSet<Integer> result=new HashSet<>();
        result.add(current);
        if(left)
        {
            for(Integer tmp:matched_right)
            {
                Pair<Integer,Integer> segment_two=new Pair<>(current,tmp);
                if(edges_complement.containsKey(segment_two) && edges_complement.get(segment_two)>0 && !visited[tmp]) {
                    visited[tmp]=true;
                    HashSet<Integer> intermediate_result = create_z(false, visited,tmp, matched_pairs, edges_complement, matched_right);
                    result.addAll(intermediate_result);
                }
            }
        }
        else
        {
            Integer a=matched_pairs.get(current);
            if(!visited[a]) {
                result.add(a);
                visited[a]=true;
                HashSet<Integer> intermediate_result = create_z(true, visited,a, matched_pairs, edges_complement, matched_right);
                result.addAll(intermediate_result);
            }
        }
        return result;
    }
    private boolean contain(Pair<MapPoint,MapPoint> segement,ArrayList<MapPoint> path)
    {
        MapPoint start=path.get(0);
        for(int i=1;i<path.size();++i)
        {
            MapPoint cur=path.get(i);
            Pair<MapPoint,MapPoint> segement_two=new Pair<>(start,cur);
            if(segement_two.equals(segement))
            {
                return true;
            }
            start=cur;
        }
        return false;
    }
    private void get_endpoints()
    {
        HashMap<MapPoint,Integer>leftm=new HashMap<>();
        HashMap<MapPoint,Integer>rightm=new HashMap<>();
        leftm.put(mainpath.get(0),1);
        rightm.put(mainpath.get(mainpath.size()-1),1);
        endpoints.mFirst+=1;
        endpoints.mSecond+=1;
        for(int i=0;i<otherpaths.size();++i)
        {
            if(leftm.containsKey(otherpaths.get(i).get(0)))
            {

            }
            else
            {
                leftm.put(otherpaths.get(i).get(0),1);
                endpoints.mFirst+=1;
            }
            int size=otherpaths.get(i).size();
            if(rightm.containsKey(otherpaths.get(i).get(size-1)))
            {

            }
            else
            {
                rightm.put(otherpaths.get(i).get(size-1),1);
                endpoints.mSecond+=1;
            }
        }
    }
    public Simple_biclique() {
        mainpath=new ArrayList<>();
        otherpaths=new ArrayList<>();
        result=new HashMap<>();
        result_detailed=new HashMap<>();
        sequence= new HashMap<>();
        indexer=new HashMap<>();
        endpoints=new Pair<>(0,0);
        num=new Integer(0);
    }
    public ArrayList<MapPoint> get_mainpath()
    {
        return mainpath;
    }
    @Override
    public void setMainPath(ArrayList<MapPoint> path)
    {
        mainpath=path;
        for(int i=0;i<path.size();++i)
        {
            if(!indexer.containsKey(path.get(i)))
            {
                indexer.put(path.get(i),num);
                num+=1;
            }
        }
    }
    @Override
    public void addPath(ArrayList<MapPoint> path)
    {
        otherpaths.add(path);
        for(int i=0;i<path.size();++i)
        {
            if(!indexer.containsKey(path.get(i)))
            {
                indexer.put(path.get(i),num);
                num+=1;
            }
        }
    }
    @Override
    public void done()
    {
        return ;
    }

    public  HashMap<Pair<MapPoint,MapPoint>,Pair<Pair<Integer,Integer>,Pair<Integer,Integer>>> get_result()
    {
        //for initialization
        result.clear();
        result_detailed.clear();
        sequence.clear();
        endpoints.mFirst=0;
        endpoints.mSecond=0;
        get_endpoints();
        //next is about the algorithm
        MapPoint pre=mainpath.get(0);
        //  System.out.print("There are "+otherpaths.size()+" paths\n");
        // System.out.print("Mainpath length is "+mainpath.size()+"\n");
        for(int i=1;i<mainpath.size();++i)
        {
            Integer source=new Integer(num+5);
            Integer destination=new Integer(num+10);
            MapPoint cur=mainpath.get(i);
            Pair<MapPoint,MapPoint>segment=new Pair<>(pre,cur);
            HashSet<Integer> left=new HashSet<>();
            HashSet<Integer> right=new HashSet<>();
            HashMap<Pair<Integer,Integer>,Integer> edges=new HashMap<>();//the edges of graph G
            HashMap<Pair<Integer,Integer>,Integer> edges_complement=new HashMap<>();//the edges of graph G'
            int times=0;
            for(int j=0;j<otherpaths.size();++j)
            {
                ArrayList<MapPoint> path=otherpaths.get(j);
                if(contain(segment,path))
                {
                    Integer start=new Integer(indexer.get(path.get(0)));
                    Integer end=new Integer(indexer.get(path.get(path.size()-1)));
                    left.add(start);
                    right.add(end);
                    times+=1;
                }
            }
            Pair<Integer,Integer>left_number=new Pair<>(0,endpoints.mFirst);
            Pair<Integer,Integer>right_number=new Pair<>(0,endpoints.mSecond);
            if(times==left.size()*right.size())
            {
                left_number.mFirst=endpoints.mFirst;
                right_number.mFirst=endpoints.mSecond;
            }
            Pair<Pair<Integer,Integer>,Pair<Integer,Integer>> p=new Pair<>(left_number,right_number);
            result.put(segment,p);
            pre=cur;
        }
        otherpaths.clear();
        indexer.clear();
        num=0;
        return result;
    }
}

package client;

import util.MapPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.*;
import java.util.stream.StreamSupport;

import util.Pair;

public class Conditional_biclique{
    private ArrayList<MapPoint> mainpath;
    private ArrayList<ArrayList<MapPoint>> otherpaths;
    private HashMap<Pair<MapPoint,MapPoint>,Pair<MapPoint[],MapPoint[]>>result;
    private HashMap<MapPoint,Integer> indexer;
    private HashMap<Integer,Integer> degree;
    private HashMap<Pair<Integer,Integer>,Integer> edges;
    private HashSet<Integer> left_result;
    private HashSet<Integer> right_result;
    private Integer num;
    private Integer k;
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
    private boolean check(HashSet<Integer> left_result,HashSet<Integer> right_result)
    {
        for(Integer tmp:left_result)
        {
            for(Integer tmp2:right_result)
            {
                Pair<Integer,Integer> segment=new Pair<>(tmp,tmp2);
                if(edges.containsKey(segment))continue;
                else return false;
            }
        }
        return true;
    }
    private boolean dfs(HashSet<Integer> left,HashSet<Integer> right,boolean [] visited,int remain_left, int remain_right,int left_k,int right_k)
    {
        if(remain_right==0)
        {
            if(check(left_result,right_result))
            {
                return true;
            }
            else return false;
        }
        else
        {
            if(remain_left>0)
            {
                for(Integer tmp:left)
                {
                    if(!visited[tmp] && degree.get(tmp)>=right_k)
                    {
                        left.add(tmp);
                        visited[tmp]=true;
                        if(dfs(left,right,visited,(remain_left-1),remain_right,left_k,right_k))return true;
                        left.remove(tmp);
                        visited[tmp]=false;
                    }
                }
            }
            else
            {
                for(Integer tmp:right)
                {
                    if(!visited[tmp] && degree.get(tmp)>=left_k)
                    {
                        right.add(tmp);
                        visited[tmp]=true;
                        if(dfs(left,right,visited,remain_left,(remain_right-1),left_k,right_k))return true;
                        right.remove(tmp);
                        visited[tmp]=false;
                    }
                }
            }
            return  false;
        }
    }
    public Conditional_biclique() {
        mainpath=new ArrayList<>();
        otherpaths=new ArrayList<>();
        result=new HashMap<>();
        indexer=new HashMap<>();
        num=new Integer(0);
    }
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
    public void done(int left_k,int right_k)
    {
        MapPoint pre=mainpath.get(0);
        for(int i=1;i<mainpath.size();++i)
        {
            MapPoint cur=mainpath.get(i);
            Pair<MapPoint,MapPoint>segment=new Pair<>(pre,cur);
            HashSet<Integer> left=new HashSet<>();
            HashSet<Integer> right=new HashSet<>();
            degree=new HashMap<>();
            edges=new HashMap<>();//the edges of graph G
            //this is about building a bipartite graph ;
            for(int j=0;j<otherpaths.size();++j)
            {
                ArrayList<MapPoint> path=otherpaths.get(j);
                if(contain(segment,path))
                {
                    Integer start=new Integer(indexer.get(path.get(0)));
                    Integer end=new Integer(indexer.get(path.get(path.size()-1)));
                    left.add(start);
                    right.add(end);
                    edges.put(new Pair<>(start,end),1);
                }
            }
            //this is about adding degrees for each node
            for(Integer tmp:left)
            {
                for(Integer tmp2:right)
                {
                    Pair<Integer,Integer>segment_tmp=new Pair<>(tmp,tmp2);
                    if(edges.containsKey(segment_tmp))
                    {
                        if(degree.containsKey(tmp))
                        {
                            degree.put(tmp,degree.get(tmp)+1);
                        }
                        else degree.put(tmp,1);
                    }
                    segment_tmp=new Pair<>(tmp2,tmp);
                    if(edges.containsKey(segment_tmp))
                    {
                        if(degree.containsKey(tmp2))
                        {
                            degree.put(tmp2,degree.get(tmp2)+1);
                        }
                        else degree.put(tmp2,1);
                    }
                }
            }
            //deleting nodes
            boolean flag=true;
            while(flag)
            {
                flag=false;
                for(Integer tmp:left)
                {
                    if (degree.get(tmp)<right_k)
                    {
                        left.remove(tmp);
                        flag=true;
                        for (Integer tmp2:right)
                        {
                            Pair<Integer,Integer>segemnt=new Pair<>(tmp,tmp2);
                            if(edges.containsKey(segemnt))
                            {
                                edges.remove(segemnt);
                                degree.put(tmp2,degree.get(tmp2)-1);
                            }
                        }
                    }
                }
                for(Integer tmp:right)
                {
                    if(degree.get(tmp)<left_k)
                    {
                        right.remove(tmp);
                        flag=true;
                        for(Integer tmp2:left)
                        {
                            Pair<Integer,Integer> segemnt=new Pair<>(tmp2,tmp);
                            if(edges.containsKey(segemnt))
                            {
                                edges.remove(segemnt);
                                degree.put(tmp2,degree.get(tmp2)-1);
                            }
                        }
                    }
                }
            }
            //dfs for enumeration
            boolean [] visited=new boolean[num+10];
            if(dfs(left,right,visited,left_k,right_k,left_k,right_k))
            {
                System.out.print("Find answer!\n");
            }
            else System.out.print("Not find answer!\n");
            pre=cur;
        }
    }
}

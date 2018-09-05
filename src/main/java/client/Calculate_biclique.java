package client;

import util.MainPathEmptyException;
import util.MapPoint;
import util.Pair;

import java.util.*;

public class Calculate_biclique implements PostProcess{
    private ArrayList<MapPoint> mainpath;
    private ArrayList<ArrayList<MapPoint>> otherpaths;
    private HashMap<Pair<MapPoint,MapPoint>,Pair<MapPoint[],MapPoint[]>>result;
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
    public Calculate_biclique() {
        mainpath=new ArrayList<>();
        otherpaths=new ArrayList<>();
        result=new HashMap<>();
        indexer=new HashMap<>();
        num=new Integer(0);
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
        MapPoint pre=mainpath.get(0);
        System.out.print("There are "+otherpaths.size()+" paths\n");
        System.out.print("Mainpath length is "+mainpath.size()+"\n");
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
            //System.out.print("Start to build graph G!\n");
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
           // System.out.print("Building graph G done!\n");
           //System.out.print("There are "+left.size()+" nodes in the left and "+right.size()+" nodes in the right\n");
           // System.out.print("There are "+edges.size()+" edges in Graph G!\n");
            //build G'
            Iterator<Integer> it=left.iterator();
            while(it.hasNext())
            {
                Iterator<Integer> it_two=right.iterator();
                Integer a=it.next();
                int p=0;
                while(it_two.hasNext())
                {
                    Integer b=it_two.next();
                    Pair<Integer,Integer> segment_two=new Pair<>(a,b);
                    if(edges.containsKey(segment_two)) {
                        continue;
                    }
                    else
                    {
                        edges_complement.put(segment_two,1);
                        segment_two=new Pair<>(b,a);
                        edges_complement.put(segment_two,0);
                    }
                }
            }
            if(edges_complement.size()>0)System.out.print("There are "+edges_complement.size()+" edges in Graph G'!\n");
            else
            {
                pre=cur;
                continue;
            }
         //   System.out.print("There are "+left.size()+" nodes in the left and "+right.size()+" nodes in the right\n");
            it=left.iterator();
            while(it.hasNext())
            {
                Integer next=it.next();
                edges_complement.put(new Pair<>(source,next),1);
                edges_complement.put(new Pair<>(next,source),0);
            }
            it=right.iterator();
            while(it.hasNext())
            {
                Integer next=it.next();
                edges_complement.put(new Pair<>(next,destination),1);
                edges_complement.put(new Pair<>(destination,next),0);
            }

            //maxflow algorithm for finding maximum matching in bipartite graph G'
            left.add(source);
            right.add(destination);
            Integer maxflow=new Integer(0);
            while(true)
            {
                Integer[] previous=new Integer[num+20];
                boolean[] visited=new boolean[num+20];
                for(int k=0;k<visited.length;++k) {
                    visited[k]=false;
                }
                visited[source]=true;
                previous[source]=-1;
                LinkedList<Integer> q = new LinkedList<>();
                q.add(source);
                while(!q.isEmpty())
                {
                    Integer current=q.getFirst();
                    q.removeFirst();
                    it=left.iterator();
                    while(it.hasNext())
                    {
                        Integer next=it.next();
                        Pair<Integer,Integer> segement=new Pair<>(current,next);
                        if(edges_complement.containsKey(segement) && edges_complement.get(segement)>0 && !visited[next])
                        {
                            q.add(next);
                            previous[next]=current;
                            visited[next]=true;
                        }
                    }
                    it=right.iterator();
                    while(it.hasNext())
                    {
                        Integer next=it.next();
                        Pair<Integer,Integer> segement=new Pair<>(current,next);
                        if(edges_complement.containsKey(segement) && edges_complement.get(segement)>0 && !visited[next])
                        {
                            q.add(next);
                            previous[next]=current;
                            visited[next]=true;
                        }
                    }
                }
                if(!visited[destination])break;
                else
                {
                    Integer current=destination;
                    while(previous[current]!=-1)
                    {
                        Integer p=previous[current];
                        Pair<Integer,Integer> segment_tmp=new Pair<>(p,current);
                        edges_complement.put(segment_tmp,edges_complement.get(segment_tmp)-1);
                        segment_tmp=new Pair<>(current,p);
                        edges_complement.put(segment_tmp,edges_complement.get(segment_tmp)+1);
                        current=p;
                    }
                    maxflow+=1;
                }
            }
            System.out.print("Maxflow for graph G' is "+maxflow+"\n");
            //get minimum vertex cover in G'
            left.remove(source);
            right.remove(destination);
            HashSet<Integer> unmatched_left=new HashSet<>();
            HashSet<Integer> matched_right=new HashSet<>();
            HashSet<Integer> z=new HashSet<>();//z is used to create minimum cover
            HashSet<Integer> k=new HashSet<>();//k is the vertex cover
            HashMap<Integer,Integer>matched_pairs=new HashMap<>();
            boolean[] matched=new boolean[num+20];
            for(int j=0;j<matched.length;++j)matched[j]=false;
            it=left.iterator();
            while(it.hasNext())
            {
                Iterator<Integer> it_two=right.iterator();
                Integer a=it.next();
                while(it_two.hasNext())
                {
                    Integer b=it_two.next();
                    Pair<Integer,Integer> segment_two=new Pair<>(a,b);
                    if(edges_complement.containsKey(segment_two) && edges_complement.get(segment_two)==0)
                    {
                        matched[a]=true;
                        matched[b]=true;
                        matched_pairs.put(b,a);
                    }
                }
            }
            it=left.iterator();
            while(it.hasNext())
            {
                Integer a=it.next();
                if(!matched[a])
                {
                    unmatched_left.add(a);
                    z.add(a);
                }
            }
            it=right.iterator();
            while(it.hasNext())
            {
                Integer a=it.next();
                if(matched[a])
                {
                    matched_right.add(a);
                }
            }
            boolean[] visited=new boolean[num+20];
            for(Integer tmp:unmatched_left)
            {
                visited[tmp]=true;
                z.addAll(create_z(true,visited,tmp,matched_pairs,edges_complement,matched_right));
            }
            HashSet<Integer> tmp_left=new HashSet<>(left);
            HashSet<Integer> tmp_right=new HashSet<>(right);
            tmp_left.removeAll(z);
            tmp_right.retainAll(z);
            k.addAll(tmp_left);
            k.addAll(tmp_right);
            System.out.print("Minimal vertex cover size "+k.size()+"\n");
            tmp_left=new HashSet<>(left);
            tmp_right=new HashSet<>(right);
            tmp_left.removeAll(k);
            tmp_right.removeAll(k);
            for(Integer tmp: tmp_left)
            {
                for(Integer tmp2:tmp_right)
                {
                    Pair<Integer,Integer> segment_tmp=new Pair<>(tmp,tmp2);
                    if(!edges.containsKey(segment_tmp))System.out.print("Error\n");
                }
            }

            System.out.print("Segment: "+pre.toString()+"->"+cur.toString()+"; Maximal Biclique: Left:"+tmp_left.size()+" Right: "+tmp_right.size()+"\n");
            pre=cur;
        }
    };
}

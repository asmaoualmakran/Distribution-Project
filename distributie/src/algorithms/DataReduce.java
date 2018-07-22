package algorithms;

import data.models.Comment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveTask;


public class DataReduce extends RecursiveTask<List<Comment>>{

    int lo;
    int hi;
    int SEQUENTIALCUTOFF;
    String brand;
    List<Comment> commentList;
    List<Comment> reducedData;


    public DataReduce(String brand, List<Comment> commentList, int SEQUENTIALCUTOFF){
        this(brand,commentList,SEQUENTIALCUTOFF,new ArrayList<>(),0,commentList.size());
    }

    private DataReduce(String brand, List<Comment>commentList, int SEQUENTIALCUTOFF,List<Comment>reducedData, int lo, int hi ){

        this.brand = brand;
        this.commentList = commentList;
        this.SEQUENTIALCUTOFF = SEQUENTIALCUTOFF;
        this.reducedData = reducedData;
        this.lo = lo;
        this.hi = hi;

    }

    @Override
    protected List<Comment> compute() {

        if(hi-lo <SEQUENTIALCUTOFF){

            for(int i =lo; i < hi; i++){

                Comment comment = commentList.get(i);
                String exactBrandRegex = ".*\\b"+ brand +"\\b.*";

                if(comment.body.matches(exactBrandRegex)){
                    reducedData.add(comment);
                }

            }
        }else{
            int mid = (lo+hi)/2;
            DataReduce left_task = new DataReduce(brand,commentList,SEQUENTIALCUTOFF,reducedData, lo, mid);
            DataReduce right_task = new DataReduce(brand,commentList,SEQUENTIALCUTOFF,reducedData, mid, hi);
            left_task.fork();
            right_task.compute();
            left_task.join();



        }

       // System.out.println(reducedData.size());
        return reducedData;
    }

}

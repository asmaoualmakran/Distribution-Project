package algorithms;

import data.models.Comment;
import com.vader.sentiment.analyzer.SentimentAnalyzer;
import com.vader.sentiment.util.ScoreType;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RecursiveTask;

public class BrandAnalyser extends RecursiveTask<Float[]> {

    int lo;
    int hi;
    int SEQUENTIALCUTOFF;
    Float[] sentimentArray;
    List<Comment> data;



    public BrandAnalyser(List<Comment> data, int SEQUENTIALCUTOFF){
        this(data, new Float[data.size()],SEQUENTIALCUTOFF, 0, data.size()-1);
    }

    private BrandAnalyser(List<Comment> data, Float[] sentimentArray, int SEQUENTIALCUTOFF, int lo, int hi){

        this.data = data;
        this.sentimentArray = sentimentArray;
        this.SEQUENTIALCUTOFF = SEQUENTIALCUTOFF;
        this.lo = lo;
        this.hi = hi;
    }

    @Override
    protected Float[] compute(){

        SentimentAnalyzer sentimentAnalyzer = new SentimentAnalyzer();

        if(hi-lo < SEQUENTIALCUTOFF){

            for(int i = lo; i <= hi; i++){
               Comment comment = data.get(i);

            // if the brand is in the body or the string null is passed, we just scan the data
            // this is to enable searches without having to to specify the brand (fase 2)
                    try {
                        sentimentAnalyzer.setInputString(comment.body);
                    //    System.out.println(comment.body);
                        sentimentAnalyzer.setInputStringProperties();
                        sentimentAnalyzer.analyze();


                        Map<String, Float> inputStringPolarity = sentimentAnalyzer.getPolarity();
                        float commentCompoundScore = inputStringPolarity.get(ScoreType.COMPOUND);
                        sentimentArray[i] = commentCompoundScore;

                    } catch (IOException e) {
                        System.out.println(e.toString());
                    }

                }

        }else{
            int mid = (lo+hi)/2;
            BrandAnalyser left = new BrandAnalyser(data,sentimentArray,SEQUENTIALCUTOFF,lo,mid);
            BrandAnalyser right = new BrandAnalyser(data,sentimentArray,SEQUENTIALCUTOFF,mid,hi);

            left.fork();
            right.compute();
            left.join();
        }

        return sentimentArray;
    }
}

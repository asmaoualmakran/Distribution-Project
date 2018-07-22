package algorithms;


import data.models.Comment;
import data.readers.RedditCommentLoader;
import com.vader.sentiment.analyzer.SentimentAnalyzer;
import com.vader.sentiment.util.ScoreType;
import org.apache.log4j.BasicConfigurator;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

public class ParallelAnalyser {



    public static void main(String[] args) {



        int P = 4;
        int T = 1000;
        String brand = "BMW";

        try {
            // Multiple files can be provided. These will be read one after the other.
            String[] data = new String[]{
                    "./src/files/dataset_1.json",
            };


            List<Comment> comments = RedditCommentLoader.readData(data);
            float sentimentSubreddit = SentimentSubreddit(comments, P,T);
            System.out.printf("The sentiment value in this subreddit is: %f \n", sentimentSubreddit);

//            float sentimentBrand = SentimentBrand(brand, comments, P, T);
//            System.out.printf("The sentiment of the brand " + brand + " is: %f \n", sentimentBrand);



        }
        catch (IOException e) {
            System.out.println(e.toString());
        }

    }

    public  static Float CalculateSentiment(Float[] sentimentArray,ForkJoinPool pool){

        PrefixSumSentiment.TempTreeNode rootNode = pool.invoke(new PrefixSumSentiment.BuildNodeTask(sentimentArray,0, sentimentArray.length));
        Float[] resultArray = new Float[sentimentArray.length];
        float initial = 0;
        pool.invoke(new PrefixSumSentiment.SumTask(rootNode,initial,sentimentArray,resultArray));
        return resultArray[resultArray.length-1]/resultArray.length;
    }

    public static Float SentimentBrand(String brand,List<Comment> comments, int P, int T){

        ForkJoinPool fjPool = new ForkJoinPool(P);
        List<Comment>reducedData = fjPool.invoke(new DataReduce(brand,comments,T));

        Float[] sentimentArray = fjPool.invoke(new BrandAnalyser(reducedData,T));

        return CalculateSentiment(sentimentArray,fjPool);

/*
        PrefixSumSentiment.TempTreeNode rootNode = fjPool.invoke(new PrefixSumSentiment.BuildNodeTask(sentimentArray, 0, sentimentArray.length));  // build the tree for the sum

        Float[] resultArray = new Float[sentimentArray.length];  // the array where the sums are written to
        float initial = 0;      // the start value for the sum

        fjPool.invoke(new PrefixSumSentiment.SumTask(rootNode, initial , sentimentArray, resultArray));  //apply the sum on the input array and copy it to the result array

        return resultArray[resultArray.length-1];
*/

    }

    public static Float SentimentSubreddit(List<Comment> comments, int P, int T){

        BasicConfigurator.configure();

        ForkJoinPool fjPool = new ForkJoinPool(P);

        Float[] sentimentArray = fjPool.invoke(new BrandAnalyser(comments,T));

       /* PrefixSumSentiment.TempTreeNode rootNode = fjPool.invoke(new PrefixSumSentiment.BuildNodeTask(sentimentArray, 0, sentimentArray.length));

        Float[] resultArray = new Float[sentimentArray.length];
        float initial = 0;

        fjPool.invoke(new PrefixSumSentiment.SumTask(rootNode, initial , sentimentArray, resultArray));

        return resultArray[resultArray.length-1];
        */
       return CalculateSentiment(sentimentArray,fjPool);

    }


}

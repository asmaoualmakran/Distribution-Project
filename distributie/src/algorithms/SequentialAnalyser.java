package algorithms;


import data.models.Comment;
import data.readers.RedditCommentLoader;
import com.vader.sentiment.analyzer.SentimentAnalyzer;
import com.vader.sentiment.util.ScoreType;
import org.apache.log4j.BasicConfigurator;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SequentialAnalyser {



    public static void main(String[] args) {

        try {
            // Multiple files can be provided. These will be read one after the other.
            String[] data = new String[]{
                    "/home/asoualma/dataset_1.json",
                    "/home/asoualma/dataset_2.json"
            };

            List<Comment> comments = RedditCommentLoader.readData(data);

            float result =  NonFilteredRun(comments);
            System.out.println(result);
            float filteredResult  = FilteredRun(comments,"BMW");
            System.out.println(filteredResult);


        }
        catch (IOException e) {
            System.out.println(e.toString());
        }
    }

    public static Float NonFilteredRun(List<Comment> comments) {

        BasicConfigurator.configure();

        SentimentAnalyzer sentimentAnalyzer = new SentimentAnalyzer();
        float totalCompoundScore = 0;

        try {

            for (Comment comment : comments) {
                sentimentAnalyzer.setInputString(comment.body);
                sentimentAnalyzer.setInputStringProperties();
                sentimentAnalyzer.analyze();

                Map<String, Float> inputStringPolarity = sentimentAnalyzer.getPolarity();
                float commentCompoundScore = inputStringPolarity.get(ScoreType.COMPOUND);

                totalCompoundScore += commentCompoundScore;
            }
        }
        catch (IOException e) {
            System.out.println(e.toString());
        }


        System.out.println("average compound score: " + totalCompoundScore / comments.size());
        return totalCompoundScore;
    }


    public static Float FilteredRun(List<Comment> comments, String brand){

        SentimentAnalyzer sentimentAnalyzer = new SentimentAnalyzer();
        float totalCompoundScore = 0;


        try {

            for (Comment comment : comments) {
                if(comment.body.contains(brand)) {
                    sentimentAnalyzer.setInputString(comment.body);
                    sentimentAnalyzer.setInputStringProperties();
                    sentimentAnalyzer.analyze();

                    Map<String, Float> inputStringPolarity = sentimentAnalyzer.getPolarity();
                    float commentCompoundScore = inputStringPolarity.get(ScoreType.COMPOUND);

                    totalCompoundScore += commentCompoundScore;
                }
            }
        }
        catch (IOException e) {
            System.out.println(e.toString());
        }


        System.out.println("average compound score: " + totalCompoundScore / comments.size());
        return totalCompoundScore;

    }
}

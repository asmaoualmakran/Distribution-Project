package data.writers;


import data.models.Comment;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class RedditCommentWriter {

    /**
     * Given a path to a file and comments, write the comments in JSON format to the file
     *
     * @param path    Path to a non-existing file
     * @param comments A list of comments
     */
    static public void writeToFile(String path, List<Comment> comments) throws IOException {
        PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(path)));
        for (Comment comment : comments)
            writeComment(writer, comment);
        writer.close();
    }


    /**
     * Convert a comment to JSON format and write it to the output stream
     *
     * @param writer Somewhere to write data to
     * @param comment A comment
     */
    static private void writeComment(PrintWriter writer, Comment comment) {
        Gson gson = getBuilder();
        String reviewJson = gson.toJson(comment, Comment.class);
        writer.println(reviewJson);
    }


    static private Gson getBuilder() {
        return new GsonBuilder().setPrettyPrinting().create();
    }
}
